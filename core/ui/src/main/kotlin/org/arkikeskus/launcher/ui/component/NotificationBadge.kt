package org.arkikeskus.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Red dot used for notification badges; the white ring keeps it readable on any background. */
private val NotificationDotColor = Color(0xFFE53935)

/**
 * A notification badge: a numeric count (Nova-style) when [showCount] is true, otherwise a plain dot
 * (Pixel-style). [scale] multiplies the size (1.0 = default). Shared by [AppIcon] and the folder icon.
 */
@Composable
fun NotificationBadge(count: Int, showCount: Boolean, scale: Float = 1f, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val s = scale.coerceIn(0.5f, 2.5f)
    if (showCount) {
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = (16 * s).dp, minHeight = (16 * s).dp)
                .border((1.5f * s).dp, Color.White, CircleShape)
                .background(NotificationDotColor, CircleShape)
                .padding(horizontal = (4 * s).dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = Color.White,
                fontSize = (9 * s).sp,
                lineHeight = (10 * s).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    } else {
        Box(
            modifier = modifier
                .size((11 * s).dp)
                .border((1.5f * s).dp, Color.White, CircleShape)
                .background(NotificationDotColor, CircleShape),
        )
    }
}
