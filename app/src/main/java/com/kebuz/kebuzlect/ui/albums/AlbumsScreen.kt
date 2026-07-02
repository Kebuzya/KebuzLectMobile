package com.kebuz.kebuzlect.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kebuz.kebuzlect.R
import com.kebuz.kebuzlect.ui.common.AccentMark
import com.kebuz.kebuzlect.ui.common.BarIcon
import com.kebuz.kebuzlect.ui.common.BottomActionButton
import com.kebuz.kebuzlect.ui.common.kebuz
import com.kebuz.kebuzlect.ui.theme.MonoCaps
import com.kebuz.kebuzlect.ui.theme.MonoMeta
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun AlbumsScreen(
    onOpenAlbum: (String) -> Unit,
    onAddAlbum: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AlbumsViewModel = viewModel(),
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val colors = kebuz()
    var removeCandidate by remember { mutableStateOf<AlbumCard?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentMark(colors.neonCyan)
            Text(
                text = stringResource(R.string.albums_title).uppercase(),
                style = MonoCaps,
                color = colors.foreground,
                modifier = Modifier.padding(start = 11.dp).weight(1f),
            )
            BarIcon(
                icon = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.action_settings),
                onClick = onOpenSettings,
                tint = colors.muted,
            )
        }
        HorizontalDivider(color = colors.divider)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (cards.isEmpty()) {
                Text(
                    text = stringResource(R.string.albums_empty),
                    color = colors.muted,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                AlbumList(
                    cards = cards,
                    onOpenAlbum = onOpenAlbum,
                    onMoveAlbum = viewModel::moveAlbum,
                    onRemoveRequest = { removeCandidate = it },
                )
            }
        }

        HorizontalDivider(color = colors.divider)
        BottomActionButton(
            label = stringResource(R.string.action_new_album),
            icon = Icons.Outlined.CreateNewFolder,
            filled = false,
            onClick = onAddAlbum,
            modifier = Modifier.navigationBarsPadding(),
        )
    }

    val candidate = removeCandidate
    if (candidate != null) {
        AlertDialog(
            onDismissRequest = { removeCandidate = null },
            containerColor = colors.surface,
            title = {
                Text(stringResource(R.string.remove_album_title), color = colors.foreground)
            },
            text = {
                Text(stringResource(R.string.remove_album_message, candidate.name), color = colors.muted)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeAlbum(candidate.bucketId)
                        removeCandidate = null
                    },
                ) {
                    Text(stringResource(R.string.action_remove), color = colors.red)
                }
            },
            dismissButton = {
                TextButton(onClick = { removeCandidate = null }) {
                    Text(stringResource(R.string.action_cancel), color = colors.muted)
                }
            },
        )
    }
}

@Composable
private fun AlbumList(
    cards: List<AlbumCard>,
    onOpenAlbum: (String) -> Unit,
    onMoveAlbum: (String, Int) -> Unit,
    onRemoveRequest: (AlbumCard) -> Unit,
) {
    val colors = kebuz()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var draggingId by remember { mutableStateOf<String?>(null) }
    var targetSlot by remember { mutableStateOf<Int?>(null) }
    var indicatorY by remember { mutableStateOf<Float?>(null) }
    var initialY by remember { mutableFloatStateOf(0f) }
    var pressY by remember { mutableFloatStateOf(0f) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    var draggedHeight by remember { mutableIntStateOf(0) }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }

    val indexById by rememberUpdatedState(
        remember(cards) {
            cards.withIndex().associate { (index, card) -> card.bucketId to index }
        },
    )

    fun clearDropTarget() {
        targetSlot = null
        indicatorY = null
    }

    fun updateDropTarget() {
        val id = draggingId ?: return
        val y = pointerY
        val rows = listState.layoutInfo.visibleItemsInfo.filter { it.key is String }
        val nearest = rows.minByOrNull { item ->
            maxOf(item.offset - y, y - (item.offset + item.size), 0f)
        }
        val index = nearest?.let { indexById[it.key as String] }
        if (nearest == null || index == null) {
            clearDropTarget()
            return
        }
        val after = y > nearest.offset + nearest.size / 2f
        val slot = if (after) index + 1 else index
        val source = indexById[id]
        if (source != null && (slot == source || slot == source + 1)) {
            clearDropTarget()
            return
        }
        targetSlot = slot
        indicatorY = (if (after) nearest.offset + nearest.size else nearest.offset).toFloat()
    }

    fun updateAutoScroll() {
        if (draggingId == null) {
            autoScrollSpeed = 0f
            return
        }
        val zone = with(density) { 72.dp.toPx() }
        val maxStep = with(density) { 9.dp.toPx() }
        val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
        val y = pointerY
        autoScrollSpeed = when {
            y < zone -> -maxStep * ((zone - y) / zone).coerceIn(0f, 1f)
            y > viewportHeight - zone -> maxStep * ((y - (viewportHeight - zone)) / zone).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    fun startDrag(press: Offset) {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.key is String && press.y >= item.offset && press.y <= item.offset + item.size
        } ?: return
        initialY = info.offset.toFloat()
        draggedHeight = info.size
        pressY = press.y
        pointerY = press.y
        clearDropTarget()
        draggingId = info.key as String
    }

    fun cancelDrag() {
        draggingId = null
        clearDropTarget()
        autoScrollSpeed = 0f
    }

    fun endDrag() {
        val from = draggingId
        val slot = targetSlot
        cancelDrag()
        if (from != null && slot != null) onMoveAlbum(from, slot)
    }

    LaunchedEffect(autoScrollSpeed) {
        if (autoScrollSpeed == 0f) return@LaunchedEffect
        while (isActive) {
            listState.scrollBy(autoScrollSpeed)
            updateDropTarget()
            delay(12)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { press -> startDrag(press) },
                        onDrag = { change, _ ->
                            if (draggingId != null) {
                                change.consume()
                                pointerY = change.position.y
                                updateDropTarget()
                                updateAutoScroll()
                            }
                        },
                        onDragEnd = { endDrag() },
                        onDragCancel = { cancelDrag() },
                    )
                },
        ) {
            items(cards, key = { it.bucketId }) { card ->
                Column {
                    AlbumRow(
                        card = card,
                        isGhost = card.bucketId == draggingId,
                        onClick = { onOpenAlbum(card.bucketId) },
                        onRemoveRequest = { onRemoveRequest(card) },
                    )
                    HorizontalDivider(color = colors.divider)
                }
            }
        }

        val indY = indicatorY
        if (indY != null) {
            val indicatorColor = colors.neonCyan
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(1f)
                    .drawBehind {
                        val strokeWidth = 3.dp.toPx()
                        val y = indY.coerceIn(strokeWidth / 2f, size.height - strokeWidth / 2f)
                        val inset = 8.dp.toPx()
                        drawLine(
                            color = indicatorColor,
                            start = Offset(inset, y),
                            end = Offset(size.width - inset, y),
                            strokeWidth = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                                0f,
                            ),
                        )
                    },
            )
        }

        val ghostCard = draggingId?.let { id -> cards.firstOrNull { it.bucketId == id } }
        if (ghostCard != null && draggedHeight > 0) {
            val rowHeight = with(density) { draggedHeight.toDp() }
            Box(
                modifier = Modifier
                    .zIndex(2f)
                    .offset { IntOffset(0, (initialY + pointerY - pressY).roundToInt()) }
                    .fillMaxWidth()
                    .height(rowHeight)
                    .graphicsLayer { shadowElevation = 16.dp.toPx() }
                    .background(colors.surface),
            ) {
                AlbumRow(card = ghostCard, isGhost = false, onClick = {}, onRemoveRequest = {})
            }
        }
    }
}

@Composable
private fun AlbumRow(
    card: AlbumCard,
    isGhost: Boolean,
    onClick: () -> Unit,
    onRemoveRequest: () -> Unit,
) {
    val colors = kebuz()
    val accent = if (card.newCount > 0) colors.neonMagenta else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onClick() }
            .graphicsLayer { alpha = if (isGhost) 0.35f else 1f },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(accent))
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 11.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(colors.tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = colors.cyan,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
                Text(
                    text = card.name,
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = albumMeta(card),
                    style = MonoMeta,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            AlbumTrailing(card)
            AlbumRowMenu(onRemoveRequest = onRemoveRequest)
        }
    }
}

@Composable
private fun AlbumRowMenu(onRemoveRequest: () -> Unit) {
    val colors = kebuz()
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        BarIcon(
            icon = Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.action_album_menu),
            onClick = { menuOpen = true },
            tint = colors.muted,
            size = 20,
        )
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.action_remove_from_list), color = colors.red)
                },
                onClick = {
                    menuOpen = false
                    onRemoveRequest()
                },
            )
        }
    }
}

@Composable
private fun albumMeta(card: AlbumCard): String {
    if (card.loadingCounts) {
        return if (card.source.isBlank()) "…" else card.source
    }
    val lec = stringResource(R.string.albums_lec_count, card.lectureCount)
    return if (card.source.isBlank()) lec else "${card.source} · $lec"
}

@Composable
private fun AlbumTrailing(card: AlbumCard) {
    val colors = kebuz()
    when {
        card.loadingCounts -> Unit
        card.newCount > 0 -> Text(
            text = stringResource(R.string.albums_new_badge, card.newCount),
            style = MonoMeta.copy(fontWeight = FontWeight.SemiBold),
            color = Color.Black,
            modifier = Modifier
                .background(colors.neonMagenta)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        card.lectureCount > 0 -> Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = colors.green,
            modifier = Modifier.size(20.dp),
        )
    }
}
