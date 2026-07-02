package com.kebuz.kebuzlect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kebuz.kebuzlect.ui.theme.IbmPlexMono
import com.kebuz.kebuzlect.ui.theme.KebuzColors
import com.kebuz.kebuzlect.ui.theme.LocalKebuzColors
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em

@Composable
@ReadOnlyComposable
fun kebuz(): KebuzColors = LocalKebuzColors.current

@Composable
fun AccentMark(color: Color, height: Int = 24) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .height(height.dp)
            .background(color),
    )
}

@Composable
fun BottomActionButton(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = kebuz()
    val background = if (filled) colors.neonMagenta else colors.surface
    val content = if (filled) Color.Black else colors.cyan
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .clickable(enabled = enabled) { onClick() }
            .padding(17.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            color = content,
            style = TextStyle(
                fontFamily = IbmPlexMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 0.06.em,
            ),
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
fun BarIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color,
    size: Int = 24,
) {
    Box(
        modifier = Modifier
            .size((size + 16).dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size.dp))
    }
}
