package org.arkikeskus.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.arkikeskus.launcher.feature.appdrawer.AppDrawerScreen
import org.arkikeskus.launcher.feature.home.HomeScreen

/**
 * Hosts the launcher surface: the home screen with the app drawer as a **finger-following** slide-up
 * overlay. A swipe up on home drives [progress] (0 = closed, 1 = open) so the drawer tracks the
 * finger; releasing flings/settles to the nearest end. A pull-down at the top of the drawer (or BACK
 * / HOME) closes it. [onOpenSettings] is a long-press on home / the launcher's own drawer icon.
 */
@Composable
fun LauncherShell(
    homeSignals: Flow<Unit>,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var shellHeightPx by remember { mutableFloatStateOf(1f) }
    val drawerVisible by remember { derivedStateOf { progress.value > 0.001f } }
    val settleSpring = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

    fun animateProgress(target: Float) {
        scope.launch { progress.animateTo(target, settleSpring) }
    }

    fun dragDrawer(dyPx: Float) {
        // Swipe up (negative dy) increases progress; clamp to [0, 1].
        scope.launch { progress.snapTo((progress.value - dyPx / shellHeightPx).coerceIn(0f, 1f)) }
    }

    fun settleDrawer(velocityPxPerSec: Float) {
        val target = when {
            velocityPxPerSec < -700f -> 1f
            velocityPxPerSec > 700f -> 0f
            else -> if (progress.value > 0.4f) 1f else 0f
        }
        animateProgress(target)
    }

    LaunchedEffect(homeSignals) {
        homeSignals.collect { animateProgress(0f) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { shellHeightPx = it.height.toFloat().coerceAtLeast(1f) },
    ) {
        HomeScreen(
            onOpenDrawer = { animateProgress(1f) },
            onDrawerDrag = { dragDrawer(it) },
            onDrawerSettle = { settleDrawer(it) },
            onOpenSettings = onOpenSettings,
            homeSignals = homeSignals,
            modifier = Modifier.fillMaxSize(),
        )

        if (drawerVisible) {
            AppDrawerScreen(
                onClose = { animateProgress(0f) },
                onDrawerDrag = { dragDrawer(it) },
                onDrawerSettle = { settleDrawer(it) },
                onOpenSettings = {
                    animateProgress(0f)
                    onOpenSettings()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = (1f - progress.value) * size.height },
            )
        }
    }

    BackHandler(enabled = drawerVisible) { animateProgress(0f) }
}
