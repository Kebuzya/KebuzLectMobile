package com.kebuz.kebuzlect.ui.album

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.kebuz.kebuzlect.R
import com.kebuz.kebuzlect.data.storage.StorageCompat.DeleteResult
import com.kebuz.kebuzlect.ui.common.BarIcon
import com.kebuz.kebuzlect.ui.common.BottomActionButton
import com.kebuz.kebuzlect.ui.common.kebuz
import com.kebuz.kebuzlect.ui.theme.MonoCaps
import com.kebuz.kebuzlect.ui.theme.MonoMeta
import kotlin.math.abs
import kotlin.math.roundToInt

private data class DropSlot(val date: String, val index: Int)

private data class DropIndicator(val x: Float, val y: Float, val height: Float)

@Composable
fun AlbumScreen(
    bucketId: String,
    onBack: () -> Unit,
    viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.factory(bucketId)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = kebuz()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var pendingDeleteIds by remember { mutableStateOf(emptyList<Long>()) }
    val snackbarHostState = remember { SnackbarHostState() }

    val total = state.groups.sumOf { it.photos.size }
    val selected = state.groups.sumOf { group -> group.photos.count { it.selected } }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.onDeleteConfirmed(pendingDeleteIds)
        pendingDeleteIds = emptyList()
    }
    val outputFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.onOutputFolderChosen(uri.toString())
        }
    }
    val onConvert: () -> Unit = {
        if (state.outputUriSet) viewModel.convertSelected() else outputFolderLauncher.launch(null)
    }
    val onDeletePhoto: (Long) -> Unit = { id ->
        when (val result = viewModel.requestDelete(listOf(id))) {
            is DeleteResult.Deleted -> Unit
            is DeleteResult.NeedsConfirmation -> {
                pendingDeleteIds = listOf(id)
                deleteLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                    tint = colors.foreground,
                )
                Text(
                    text = state.title.ifEmpty { stringResource(R.string.album_title) },
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(
                    text = "$selected/$total",
                    style = MonoCaps.copy(fontSize = 13.sp, letterSpacing = 0.04.em),
                    color = colors.magenta,
                )
            }
            HorizontalDivider(color = colors.divider)

            val settings = state.settings
            if (settings != null) {
                SettingsSummaryRow(settings = settings, onClick = { showSettings = !showSettings })
                HorizontalDivider(color = colors.divider)
                if (showSettings) {
                    AlbumSettingsPanel(settings = settings, viewModel = viewModel)
                    HorizontalDivider(color = colors.divider)
                }
            }

            if (state.analyzing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.cyan,
                    trackColor = colors.divider,
                )
            }
            if (state.converting) {
                Text(
                    text = stringResource(R.string.convert_progress, state.convertDone, state.convertTotal),
                    style = MonoMeta,
                    color = colors.muted,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.magenta,
                    trackColor = colors.divider,
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.cyan)
                    }

                    state.groups.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.album_empty), color = colors.muted)
                    }

                    else -> PhotoGrid(
                        state = state,
                        onOpenPhoto = viewModel::openViewer,
                        onToggleGroup = viewModel::toggleGroup,
                        onTogglePhoto = viewModel::togglePhoto,
                        onMovePhoto = viewModel::movePhoto,
                    )
                }
            }

            HorizontalDivider(color = colors.divider)
            BottomActionButton(
                label = stringResource(R.string.album_convert_button, selected, total),
                icon = Icons.Outlined.PictureAsPdf,
                filled = true,
                onClick = onConvert,
                enabled = !state.converting,
                modifier = Modifier.navigationBarsPadding(),
            )
        }

        if (state.viewerOpen) {
            AlbumViewer(
                items = state.viewerItems,
                initialIndex = state.viewerInitialIndex,
                onClose = viewModel::closeViewer,
                onRotateLeft = { viewModel.rotate(it, -90) },
                onRotateRight = { viewModel.rotate(it, 90) },
                onDelete = onDeletePhoto,
                onToggleSelect = viewModel::togglePhoto,
            )
        }

        SnackbarHost(
            snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

@Composable
private fun SettingsSummaryRow(settings: AlbumSessionSettings, onClick: () -> Unit) {
    val colors = kebuz()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Tune, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(18.dp))
        Text(
            text = "BLUR ${settings.blurThreshold.roundToInt()} · DUP ${settings.duplicateThreshold} · Q ${settings.jpegQuality} · DPI ${settings.pdfDpi}",
            style = MonoMeta,
            color = colors.cyan,
            modifier = Modifier.padding(start = 9.dp),
        )
    }
}

@Composable
private fun PhotoGrid(
    state: AlbumUiState,
    onOpenPhoto: (Long) -> Unit,
    onToggleGroup: (String) -> Unit,
    onTogglePhoto: (Long) -> Unit,
    onMovePhoto: (Long, String, Int) -> Unit,
) {
    val colors = kebuz()
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    var draggingKey by remember { mutableStateOf<Long?>(null) }
    var targetSlot by remember { mutableStateOf<DropSlot?>(null) }
    var dropIndicator by remember { mutableStateOf<DropIndicator?>(null) }
    var initialOffset by remember { mutableStateOf(Offset.Zero) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var pointerPos by remember { mutableStateOf(Offset.Zero) }
    var draggedSize by remember { mutableStateOf(IntSize.Zero) }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }

    val positionById by rememberUpdatedState(
        remember(state.groups) {
            buildMap {
                state.groups.forEach { group ->
                    group.photos.forEachIndexed { index, photo ->
                        put(photo.photo.mediaStoreId, group.date to index)
                    }
                }
            }
        },
    )

    fun clearDropTarget() {
        targetSlot = null
        dropIndicator = null
    }

    fun updateDropTarget() {
        val key = draggingKey ?: return
        val pos = pointerPos
        val cells = gridState.layoutInfo.visibleItemsInfo.filter { it.key is Long }
        val nearest = cells.minWithOrNull(
            compareBy(
                { item -> maxOf(item.offset.y - pos.y, pos.y - (item.offset.y + item.size.height), 0f) },
                { item -> abs(pos.x - (item.offset.x + item.size.width / 2f)) },
            ),
        )
        val position = nearest?.let { positionById[it.key as Long] }
        if (nearest == null || position == null) {
            clearDropTarget()
            return
        }
        val (date, index) = position
        val after = pos.x > nearest.offset.x + nearest.size.width / 2f
        val slotIndex = if (after) index + 1 else index
        val source = positionById[key]
        if (source != null && source.first == date &&
            (slotIndex == source.second || slotIndex == source.second + 1)
        ) {
            clearDropTarget()
            return
        }
        targetSlot = DropSlot(date, slotIndex)
        dropIndicator = DropIndicator(
            x = if (after) (nearest.offset.x + nearest.size.width).toFloat() else nearest.offset.x.toFloat(),
            y = nearest.offset.y.toFloat(),
            height = nearest.size.height.toFloat(),
        )
    }

    fun updateAutoScroll() {
        if (draggingKey == null) {
            autoScrollSpeed = 0f
            return
        }
        val zone = with(density) { 72.dp.toPx() }
        val maxStep = with(density) { 9.dp.toPx() }
        val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
        val y = pointerPos.y
        autoScrollSpeed = when {
            y < zone -> -maxStep * ((zone - y) / zone).coerceIn(0f, 1f)
            y > viewportHeight - zone -> maxStep * ((y - (viewportHeight - zone)) / zone).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    fun startDrag(press: Offset) {
        val info = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.key is Long &&
                press.x >= item.offset.x && press.x <= item.offset.x + item.size.width &&
                press.y >= item.offset.y && press.y <= item.offset.y + item.size.height
        } ?: return
        initialOffset = Offset(info.offset.x.toFloat(), info.offset.y.toFloat())
        draggedSize = info.size
        pressOffset = press
        pointerPos = press
        clearDropTarget()
        draggingKey = info.key as Long
    }

    fun cancelDrag() {
        draggingKey = null
        clearDropTarget()
        autoScrollSpeed = 0f
    }

    fun endDrag() {
        val from = draggingKey
        val slot = targetSlot
        cancelDrag()
        if (from != null && slot != null) onMovePhoto(from, slot.date, slot.index)
    }

    LaunchedEffect(autoScrollSpeed) {
        if (autoScrollSpeed == 0f) return@LaunchedEffect
        while (isActive) {
            gridState.scrollBy(autoScrollSpeed)
            updateDropTarget()
            delay(12)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { press -> startDrag(press) },
                        onDrag = { change, _ ->
                            if (draggingKey != null) {
                                change.consume()
                                pointerPos = change.position
                                updateDropTarget()
                                updateAutoScroll()
                            }
                        },
                        onDragEnd = { endDrag() },
                        onDragCancel = { cancelDrag() },
                    )
                },
        ) {
            state.groups.forEach { group ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "header_${group.date}") {
                    GroupHeader(group = group, onToggleAll = { onToggleGroup(group.date) })
                }
                items(group.photos, key = { it.photo.mediaStoreId }) { photo ->
                    val id = photo.photo.mediaStoreId
                    PhotoCell(
                        photo = photo,
                        converted = group.isConverted,
                        isGhost = id == draggingKey,
                        onOpen = { onOpenPhoto(id) },
                        onToggle = { onTogglePhoto(id) },
                    )
                }
            }
        }

        val indicator = dropIndicator
        if (indicator != null) {
            val indicatorColor = colors.neonCyan
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(1f)
                    .drawBehind {
                        val strokeWidth = 3.dp.toPx()
                        val x = indicator.x.coerceIn(strokeWidth / 2f, size.width - strokeWidth / 2f)
                        val inset = 2.dp.toPx()
                        drawLine(
                            color = indicatorColor,
                            start = Offset(x, indicator.y + inset),
                            end = Offset(x, indicator.y + indicator.height - inset),
                            strokeWidth = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                                0f,
                            ),
                        )
                    },
            )
        }

        val dragId = draggingKey
        val dragged = dragId?.let { id ->
            state.groups.firstNotNullOfOrNull { group ->
                group.photos.firstOrNull { it.photo.mediaStoreId == id }
            }
        }
        if (dragged != null && draggedSize.width > 0) {
            val cellWidth = with(density) { draggedSize.width.toDp() }
            val cellHeight = with(density) { draggedSize.height.toDp() }
            Box(
                modifier = Modifier
                    .zIndex(2f)
                    .offset {
                        IntOffset(
                            (initialOffset.x + pointerPos.x - pressOffset.x).roundToInt(),
                            (initialOffset.y + pointerPos.y - pressOffset.y).roundToInt(),
                        )
                    }
                    .size(cellWidth, cellHeight)
                    .padding(1.5.dp)
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                        shadowElevation = 16.dp.toPx()
                    }
                    .background(colors.grid),
            ) {
                AsyncImage(
                    model = dragged.photo.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = dragged.rotation.toFloat() },
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(group: GroupUi, onToggleAll: () -> Unit) {
    val colors = kebuz()
    val accent = if (group.allSelected) colors.neonCyan else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dateBg)
            .clickable { onToggleAll() }
            .padding(start = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(width = 3.dp, height = 44.dp).background(accent))
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (group.allSelected) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (group.allSelected) colors.magenta else colors.muted,
                modifier = Modifier.size(22.dp),
            )
            if (group.lectureNumber != null) {
                Text(
                    text = "#${group.lectureNumber}",
                    style = MonoMeta.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = colors.cyan,
                    modifier = Modifier.padding(start = 9.dp),
                )
            }
            Text(
                text = group.displayDate.uppercase(),
                style = MonoCaps.copy(fontSize = 14.sp, letterSpacing = 0.08.em),
                color = colors.foreground,
                modifier = Modifier.weight(1f).padding(start = 9.dp),
            )
            Text(
                text = group.photos.size.toString(),
                style = MonoMeta.copy(fontSize = 13.sp),
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun PhotoCell(
    photo: PhotoUi,
    converted: Boolean,
    isGhost: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = kebuz()
    val borderColor = when {
        photo.isBlurry -> colors.red
        photo.isDuplicate -> colors.amber
        else -> null
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.5.dp)
            .background(colors.grid)
            .clickable { onOpen() }
            .then(
                if (borderColor != null) Modifier.border(2.dp, borderColor) else Modifier,
            ),
    ) {
        AsyncImage(
            model = photo.photo.uri,
            contentDescription = photo.photo.filename,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = photo.rotation.toFloat()
                    if (isGhost) alpha = 0.25f
                },
        )
        if (isGhost) {
            Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)))
        } else {
            Icon(
                imageVector = if (photo.selected) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (photo.selected) colors.neonMagenta else Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .size(20.dp)
                    .clickable { onToggle() },
            )
            if (converted) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.green,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).size(18.dp),
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AlbumSettingsPanel(settings: AlbumSessionSettings, viewModel: AlbumViewModel) {
    val colors = kebuz()
    Column(modifier = Modifier.fillMaxWidth().background(colors.surface).padding(16.dp)) {
        LabeledSlider(
            label = stringResource(R.string.setting_blur_threshold, settings.blurThreshold.roundToInt()),
            value = settings.blurThreshold,
            valueRange = 0f..300f,
            onValueChange = viewModel::setBlurThreshold,
        )
        LabeledSlider(
            label = stringResource(R.string.setting_duplicate_threshold, settings.duplicateThreshold),
            value = settings.duplicateThreshold.toFloat(),
            valueRange = 0f..20f,
            steps = 19,
            onValueChange = { viewModel.setDuplicateThreshold(it.roundToInt()) },
        )
        LabeledSlider(
            label = stringResource(R.string.setting_jpeg_quality, settings.jpegQuality),
            value = settings.jpegQuality.toFloat(),
            valueRange = 1f..100f,
            onValueChange = { viewModel.setJpegQuality(it.roundToInt()) },
        )
        LabeledSlider(
            label = stringResource(R.string.setting_pdf_dpi, settings.pdfDpi),
            value = settings.pdfDpi.toFloat(),
            valueRange = 72f..300f,
            onValueChange = { viewModel.setPdfDpi(it.roundToInt()) },
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text(stringResource(R.string.setting_photos_per_page), color = colors.foreground)
            FilterChip(
                selected = settings.photosPerPage == 1,
                onClick = { viewModel.setPhotosPerPage(1) },
                label = { Text("1") },
                modifier = Modifier.padding(start = 8.dp),
            )
            FilterChip(
                selected = settings.photosPerPage == 2,
                onClick = { viewModel.setPhotosPerPage(2) },
                label = { Text("2") },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        TextButton(onClick = { viewModel.renumber() }, modifier = Modifier.padding(top = 4.dp)) {
            Text(stringResource(R.string.action_renumber), color = colors.cyan)
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
) {
    val colors = kebuz()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MonoMeta, color = colors.muted)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}
