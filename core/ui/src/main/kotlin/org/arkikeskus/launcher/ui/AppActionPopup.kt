package org.arkikeskus.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.arkikeskus.launcher.model.AppItem

/** One action row in the long-press popup (e.g. "App info", "Remove from home"). */
data class PopupAction(val label: String, val onClick: () -> Unit)

private const val POPUP_MARGIN_PX = 24

/**
 * The Pixel-style long-press popup, shared by the home screen and the app drawer. Shows the app's
 * deep shortcuts (queried off the main thread) above the caller-supplied [actions], with a caret
 * pointing at the icon ([anchor] in root coords; [preferAbove] for dock/bottom items).
 */
@Composable
fun AppActionPopup(
    app: AppItem,
    anchor: IntOffset,
    preferAbove: Boolean,
    actions: List<PopupAction>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val cardWidth = 280.dp

    var shortcuts by remember(app.key) { mutableStateOf<List<AppShortcuts.Item>>(emptyList()) }
    LaunchedEffect(app.key) {
        shortcuts = withContext(Dispatchers.IO) { AppShortcuts.query(context, app) }
    }

    val positionProvider = remember(anchor, preferAbove) {
        AnchoredPopupPositionProvider(anchor, preferAbove)
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardColor,
            tonalElevation = 3.dp,
            shadowElevation = 10.dp,
            modifier = Modifier.width(cardWidth),
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                shortcuts.forEach { shortcut ->
                    PopupRow(shortcut.label) {
                        AppShortcuts.start(context, shortcut)
                        onDismiss()
                    }
                }
                if (shortcuts.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                actions.forEach { action ->
                    PopupRow(action.label) {
                        action.onClick()
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

/** Places the menu centered under (or above) the icon, clamped to the screen. */
private class AnchoredPopupPositionProvider(
    private val anchor: IntOffset,
    private val preferAbove: Boolean,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val margin = POPUP_MARGIN_PX
        val x = (anchor.x - popupContentSize.width / 2)
            .coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
        val y = if (preferAbove) {
            (anchor.y - popupContentSize.height - margin).coerceAtLeast(margin)
        } else {
            (anchor.y + margin)
                .coerceAtMost((windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
        }
        return IntOffset(x, y)
    }
}
