package org.fmdx.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.fmdx.app.ui.theme.FmDxTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* Foreground service still runs even if denied; ignore. */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(state.errorMessage) {
                state.errorMessage?.let { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            FmDxTheme {
                FmDxApp(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onUpdateUrl = viewModel::updateServerUrl,
                    onConnect = viewModel::connect,
                    onDisconnect = viewModel::disconnect,
                    onToggleAudio = viewModel::toggleAudio,
                    onTuneDirect = viewModel::tuneToFrequency,
                    onToggleEq = viewModel::toggleEq,
                    onToggleIms = viewModel::toggleIms,
                    onToggleStereoMode = viewModel::toggleStereoMode,
                    onCycleAntenna = viewModel::cycleAntenna,
                    onScan = viewModel::requestSpectrumScan,
                    formatSignal = { s, unit -> viewModel.formatSignal(s, unit) },
                    currentPty = viewModel::currentPty,
                    antennaLabel = viewModel::antennaLabel,
                    onUpdateSettings = viewModel::updateSettings,
                    onShowPublicServerPicker = viewModel::showPublicServerPicker,
                    onHidePublicServerPicker = viewModel::hidePublicServerPicker,
                    onRefreshPublicServers = viewModel::refreshPublicServerPicker,
                    onUpdatePublicServerQuery = viewModel::updatePublicServerQuery,
                    onSelectPublicServer = viewModel::selectPublicServer,
                    onRemoveRecentServer = viewModel::removeRecentServer
                )
            }
        }
    }
}
