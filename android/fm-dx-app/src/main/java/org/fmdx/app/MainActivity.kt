package org.fmdx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.fmdx.app.ui.theme.FmDxTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

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
                    onCycleAntenna = viewModel::cycleAntenna,
                    onScan = viewModel::requestSpectrumScan,
                    formatSignal = { s, unit -> viewModel.formatSignal(s, unit) },
                    currentPty = viewModel::currentPty,
                    antennaLabel = viewModel::antennaLabel,
                    onUpdateSettings = viewModel::updateSettings
                )
            }
        }
    }
}
