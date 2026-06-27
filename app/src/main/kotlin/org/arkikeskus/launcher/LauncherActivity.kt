package org.arkikeskus.launcher

import android.appwidget.AppWidgetHost
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import org.arkikeskus.launcher.designsystem.theme.LauncherTheme
import org.arkikeskus.launcher.feature.home.APPWIDGET_HOST_ID
import org.arkikeskus.launcher.feature.home.LocalAppWidgetHost
import org.arkikeskus.launcher.ui.LauncherShell

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    /** Emits when HOME is pressed while we are already the foreground home app (onNewIntent). */
    private val homeSignals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val appWidgetHost by lazy { AppWidgetHost(applicationContext, APPWIDGET_HOST_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LauncherTheme {
                CompositionLocalProvider(LocalAppWidgetHost provides appWidgetHost) {
                    LauncherShell(
                        homeSignals = homeSignals,
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // A launcher is the device HOME — a host hiccup must never crash it.
        runCatching { appWidgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { appWidgetHost.stopListening() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignals.tryEmit(Unit)
    }
}
