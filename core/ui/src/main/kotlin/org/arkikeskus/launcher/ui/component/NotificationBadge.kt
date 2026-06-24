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
 * (Pixel-style). Shared by [AppIcon] and the home-screen folder icon.
 */
@Composable
fun NotificationBadge(count: Int, showCount: Boolean, modifier: Modifier = Modifier) {
    if (count <= 0) return
    if (showCount) {
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                .border(1.5.dp, Color.White, CircleShape)
                .background(NotificationDotColor, CircleShape)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = Color.White,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(11.dp)
                .border(1.5.dp, Color.White, CircleShape)
                .background(NotificationDotColor, CircleShape),
        )
    }
}
