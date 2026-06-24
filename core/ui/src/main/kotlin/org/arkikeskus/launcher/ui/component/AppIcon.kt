package org.arkikeskus.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.arkikeskus.launcher.model.AppItem

/**
 * An app icon plus optional label. The icon is loaded via Coil (see AppIconFetcher), so it is
 * cached and loaded off the main thread. Caller supplies [labelColor] (white on wallpaper, the
 * theme on-surface color inside the drawer). When [badgeCount] > 0 a notification dot is drawn at
 * the icon's top-right corner.
 */
@Composable
fun AppIcon(
    appItem: AppItem,
    labelColor: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    showLabel: Boolean = true,
    maxLabelLines: Int = 1,
    badgeCount: Int = 0,
    badgeShowCount: Boolean = true,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            AsyncImage(
                model = appItem,
                contentDescription = appItem.label,
                modifier = Modifier.size(iconSize),
            )
            if (badgeCount > 0) {
                if (badgeShowCount) {
                    // Numeric badge (Nova-style); the white ring keeps it readable on any background.
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                            .border(1.5.dp, Color.White, CircleShape)
                            .background(NotificationDotColor, CircleShape)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                } else {
                    // Plain dot (Pixel-style).
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .border(1.5.dp, Color.White, CircleShape)
                            .background(NotificationDotColor, CircleShape),
                    )
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = appItem.label,
                color = labelColor,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = maxLabelLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val NotificationDotColor = Color(0xFFE53935)
