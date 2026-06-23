package org.arkikeskus.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import org.arkikeskus.launcher.designsystem.theme.LauncherTheme
import org.arkikeskus.launcher.ui.LauncherShell

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    /** Emits when HOME is pressed while we are already the foreground home app (onNewIntent). */
    private val homeSignals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val phonePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            phonePermission.launch(Manifest.permission.READ_PHONE_STATE)
        }

        setContent {
            LauncherTheme {
                LauncherShell(homeSignals = homeSignals)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing HOME while already home delivers a fresh ACTION_MAIN/CATEGORY_HOME intent here.
        homeSignals.tryEmit(Unit)
    }
}
