package com.kebuz.kebuzlect.ui.album

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kebuz.kebuzlect.R
import com.kebuz.kebuzlect.ui.theme.IbmPlexMono
import com.kebuz.kebuzlect.ui.theme.NeonCyan
import com.kebuz.kebuzlect.ui.theme.NeonMagenta
import com.kebuz.kebuzlect.ui.theme.StatusRed
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun AlbumViewer(
    items: List<ViewerItem>,
    initialIndex: Int,
    onClose: () -> Unit,
    onRotateLeft: (Long) -> Unit,
    onRotateRight: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onToggleSelect: (Long) -> Unit,
) {
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, items.lastIndex),
    ) { items.size }

    val scope = rememberCoroutineScope()
    val dragY = remember { Animatable(0f) }
    val dismissThreshold = with(LocalDensity.current) { 140.dp.toPx() }
    val progress = (dragY.value / (dismissThreshold * 3f)).coerceIn(0f, 1f)
    val backgroundAlpha = 1f - progress * 0.6f

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = backgroundAlpha))) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragY.value
                    val scale = 1f - progress * 0.15f
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalX = 0f
                        var totalY = 0f
                        var locked = false
                        var verticalDismiss = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                if (verticalDismiss) {
                                    if (dragY.value > dismissThreshold) onClose()
                                    else scope.launch { dragY.animateTo(0f) }
                                }
                                break
                            }
                            val posChange = change.positionChange()
                            totalX += posChange.x
                            totalY += posChange.y
                            if (!locked && (abs(totalX) > viewConfiguration.touchSlop || abs(totalY) > viewConfiguration.touchSlop)) {
                                locked = true
                                verticalDismiss = abs(totalY) > abs(totalX)
                            }
                            if (verticalDismiss) {
                                change.consume()
                                scope.launch { dragY.snapTo((dragY.value + posChange.y).coerceAtLeast(0f)) }
                            }
                        }
                    }
                },
        ) { page ->
            val item = items[page]
            AsyncImage(
                model = item.photo.uri,
                contentDescription = item.photo.filename,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = item.rotation.toFloat() },
            )
        }

        val current = items.getOrNull(pagerState.currentPage)

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current?.photo?.filename.orEmpty(),
                    color = Color.White,
                    fontFamily = IbmPlexMono,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                if (current != null) {
                    Text(
                        text = "${current.displayDate} · ${pagerState.currentPage + 1}/${items.size}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = IbmPlexMono,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            if (current != null) {
                Row(
                    modifier = Modifier
                        .clickable { onToggleSelect(current.photo.mediaStoreId) }
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (current.selected) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = stringResource(R.string.viewer_include),
                        tint = if (current.selected) NeonMagenta else Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.viewer_include),
                        color = Color.White,
                        fontFamily = IbmPlexMono,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.viewer_close),
                tint = Color.White,
                modifier = Modifier.size(26.dp).clickable { onClose() },
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.viewer_swipe_hint),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        if (current != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                    .navigationBarsPadding()
                    .padding(top = 16.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewerAction(
                    icon = Icons.AutoMirrored.Outlined.RotateLeft,
                    label = stringResource(R.string.viewer_rotate_left),
                    tint = NeonCyan,
                    onClick = { onRotateLeft(current.photo.mediaStoreId) },
                )
                ViewerAction(
                    icon = Icons.AutoMirrored.Outlined.RotateRight,
                    label = stringResource(R.string.viewer_rotate_right),
                    tint = NeonCyan,
                    onClick = { onRotateRight(current.photo.mediaStoreId) },
                )
                ViewerAction(
                    icon = Icons.Outlined.Delete,
                    label = stringResource(R.string.viewer_delete),
                    tint = StatusRed,
                    onClick = { onDelete(current.photo.mediaStoreId) },
                )
            }
        }
    }
}

@Composable
private fun ViewerAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
        Text(
            text = label,
            color = Color(0xFFCFCFD6),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
