package org.fmdx.app

import android.os.Bundle
import android.widget.NumberPicker
import android.widget.TextView
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.fmdx.app.BuildConfig
import org.fmdx.app.model.SignalUnit
import org.fmdx.app.model.SpectrumPoint
import org.fmdx.app.model.TunerInfo
import org.fmdx.app.model.TunerState
import org.fmdx.app.ui.theme.FmDxTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
                    onRefreshSpectrum = viewModel::refreshSpectrum,
                    formatSignal = { s, unit -> viewModel.formatSignal(s, unit) },
                    currentPty = viewModel::currentPty,
                    antennaLabel = viewModel::antennaLabel,
                    onUpdateSettings = viewModel::updateSettings
                )
            }
        }
    }
}

@Composable
private fun FmDxApp(
    state: UiState,
    snackbarHostState: SnackbarHostState,
    onUpdateUrl: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleAudio: () -> Unit,
    onTuneDirect: (Double) -> Unit,
    onToggleEq: () -> Unit,
    onToggleIms: () -> Unit,
    onCycleAntenna: () -> Unit,
    onScan: () -> Unit,
    onRefreshSpectrum: () -> Unit,
    formatSignal: (TunerState?, SignalUnit) -> String,
    currentPty: (TunerState?) -> String,
    antennaLabel: () -> String,
    onUpdateSettings: (signalUnit: SignalUnit, networkBuffer: Int, playerBuffer: Int, restartAudioOnTune: Boolean) -> Unit
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    when {
        showSettings -> {
            SettingsScreen(
                state = state,
                onUpdateSettings = onUpdateSettings,
                onBack = { showSettings = false }
            )
        }

        showAbout -> {
            AboutScreen(onBack = { showAbout = false })
        }

        else -> {
            MainScreen(
                state = state,
                snackbarHostState = snackbarHostState,
                onUpdateUrl = onUpdateUrl,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onToggleAudio = onToggleAudio,
                onTuneDirect = onTuneDirect,
                onToggleEq = onToggleEq,
                onToggleIms = onToggleIms,
                onCycleAntenna = onCycleAntenna,
                onScan = onScan,
                onRefreshSpectrum = onRefreshSpectrum,
                formatSignal = formatSignal,
                currentPty = currentPty,
                antennaLabel = antennaLabel,
                onShowSettings = {
                    showAbout = false
                    showSettings = true
                },
                onShowAbout = {
                    showSettings = false
                    showAbout = true
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreen(
    state: UiState,
    snackbarHostState: SnackbarHostState,
    onUpdateUrl: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleAudio: () -> Unit,
    onTuneDirect: (Double) -> Unit,
    onToggleEq: () -> Unit,
    onToggleIms: () -> Unit,
    onCycleAntenna: () -> Unit,
    onScan: () -> Unit,
    onRefreshSpectrum: () -> Unit,
    formatSignal: (TunerState?, SignalUnit) -> String,
    currentPty: (TunerState?) -> String,
    antennaLabel: () -> String,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit
) {
    var isSpectrumDragging by remember { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    val tabs = buildList {
        add(SectionTab(R.string.server) { ServerSection(state, onUpdateUrl, onConnect, onDisconnect) })
        if (state.isConnected) {
            add(SectionTab(R.string.tuner) { TunerSection(state, onTuneDirect, formatSignal, currentPty) })
            add(
                SectionTab(R.string.controls) {
                    ControlButtons(
                        state,
                        onToggleEq,
                        onToggleIms,
                        onCycleAntenna,
                        antennaLabel
                    )
                }
            )
            add(SectionTab(R.string.rds) { RdsSection(state, currentPty) })
            add(SectionTab(R.string.station) { StationSection(state) })
            add(
                SectionTab(R.string.spectrum) {
                    SpectrumSection(
                        state = state,
                        onScan = onScan,
                        onRefreshSpectrum = onRefreshSpectrum,
                        onTuneDirect = onTuneDirect,
                        onDragStateChange = { dragging -> isSpectrumDragging = dragging }
                    )
                }
            )
        }
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(state.isConnected, tabs.size) {
        if (!state.isConnected) {
            pagerState.scrollToPage(0)
        } else if (pagerState.currentPage >= tabs.size) {
            pagerState.scrollToPage(tabs.lastIndex)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.main_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConnectionStatusIndicator(
                            isConnected = state.isConnected,
                            isConnecting = state.isConnecting
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onToggleAudio, enabled = state.isConnected) {
                            val playing = state.audioPlaying
                            val icon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow
                            Icon(
                                imageVector = icon,
                                contentDescription = if (playing) stringResource(id = R.string.stop_audio) else stringResource(
                                    id = R.string.play_audio
                                )
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(id = R.string.menu)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.settings)) },
                                    onClick = {
                                        showMenu = false
                                        onShowSettings()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.about)) },
                                    onClick = {
                                        showMenu = false
                                        onShowAbout()
                                    }
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, tab ->
                    val tabTag = "main_tab_${tab.titleRes}"
                    Tab(
                        modifier = Modifier.testTag(tabTag),
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = stringResource(id = tab.titleRes)) },
                        enabled = index == 0 || state.isConnected
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = state.isConnected && !isSpectrumDragging,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("main_sections_pager")
            ) { page ->
                key(page) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            tabs[page].content()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: UiState,
    onUpdateSettings: (signalUnit: SignalUnit, networkBuffer: Int, playerBuffer: Int, restartAudioOnTune: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(state, onUpdateSettings)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val githubLabel = stringResource(id = R.string.about_github_link)
    val githubUrl = stringResource(id = R.string.about_github_url)
    val siteLabel = stringResource(id = R.string.about_site_link)
    val siteUrl = stringResource(id = R.string.about_site_url)
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE
    val versionLabel = stringResource(id = R.string.about_version, versionName, versionCode)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.about_message),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = versionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.about_links_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ListItem(
                        headlineContent = { Text(text = githubLabel) },
                        supportingContent = { Text(text = githubUrl) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri(githubUrl) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(text = siteLabel) },
                        supportingContent = { Text(text = siteUrl) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri(siteUrl) }
                    )
                }
            }
        }
    }
}

private data class SectionTab(
    @param:StringRes val titleRes: Int,
    val content: @Composable () -> Unit
)

@Composable
private fun ConnectionStatusIndicator(
    isConnected: Boolean,
    isConnecting: Boolean
) {
    val label = when {
        isConnecting -> stringResource(id = R.string.connecting)
        isConnected -> stringResource(id = R.string.connected)
        else -> stringResource(id = R.string.disconnected)
    }
    val indicatorColor = when {
        isConnecting -> MaterialTheme.colorScheme.primary
        isConnected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier
            .background(indicatorColor.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = indicatorColor
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ServerSection(
    state: UiState,
    onUpdateUrl: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = onUpdateUrl,
                    label = { Text(stringResource(id = R.string.server_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Uri
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConnect() })
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.isConnected) {
                        OutlinedButton(onClick = onConnect, enabled = false) {
                            Text(text = stringResource(id = R.string.connect))
                        }
                        Button(onClick = onDisconnect) {
                            Text(text = stringResource(id = R.string.disconnect))
                        }
                    } else {
                        Button(onClick = onConnect, enabled = !state.isConnecting) {
                            Text(text = stringResource(id = R.string.connect))
                        }
                        OutlinedButton(onClick = onDisconnect, enabled = false) {
                            Text(text = stringResource(id = R.string.disconnect))
                        }
                    }
                }
                if (state.isConnecting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                state.statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (state.isConnected) {
            ServerInfoCard(tunerInfo = state.tunerInfo)
        }
    }
}

@Composable
private fun ServerInfoCard(tunerInfo: TunerInfo?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RdsLabelText(text = stringResource(id = R.string.server_info_title))
            if (tunerInfo == null) {
                Text(
                    text = stringResource(id = R.string.server_info_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(
                        id = R.string.server_info_tuner_name,
                        tunerInfo.tunerName
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = tunerInfo.tunerDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun FrequencyControlsCard(
    state: UiState,
    onTuneDirect: (Double) -> Unit
) {
    val stateMinKHz = state.tunerState?.minFreqMHz?.times(1000)?.roundToInt()
    val stateMaxKHz = state.tunerState?.maxFreqMHz?.times(1000)?.roundToInt()
    val stepKHz = state.tunerState?.stepKHz?.takeIf { it > 0 } ?: DEFAULT_FREQUENCY_STEP_KHZ
    val minKHz = stateMinKHz?.takeIf { it > 0 } ?: DEFAULT_MIN_FREQUENCY_KHZ
    val provisionalMaxKHz = stateMaxKHz?.takeIf { it >= minKHz } ?: DEFAULT_MAX_FREQUENCY_KHZ
    val maxKHz = max(provisionalMaxKHz, minKHz)

    val currentFreqKHz = state.tunerState?.freqKHz
        ?: state.pendingFrequencyMHz?.let { (it * 1000).roundToInt() }
        ?: minKHz

    var selectedMHz by rememberSaveable { mutableIntStateOf(0) }
    var selectedDecimalIndex by rememberSaveable { mutableIntStateOf(0) }

    val minMhz = minKHz / 1000
    val maxMhz = maxKHz / 1000
    val mhzSteps = maxMhz - minMhz + 1
    val decimalSteps = max(1, 1000 / stepKHz)

    fun minDecimalIndexFor(mhz: Int): Int {
        if (decimalSteps <= 1) return 0
        val minIndex = (minKHz % 1000) / stepKHz
        val maxIndex = (maxKHz % 1000) / stepKHz
        val raw = when {
            mhz == minMhz && mhz == maxMhz -> min(minIndex, maxIndex)
            mhz == minMhz -> minIndex
            else -> 0
        }
        return raw.coerceIn(0, decimalSteps - 1)
    }

    fun maxDecimalIndexFor(mhz: Int): Int {
        if (decimalSteps <= 1) return 0
        val minIndex = (minKHz % 1000) / stepKHz
        val maxIndex = (maxKHz % 1000) / stepKHz
        val raw = when {
            mhz == minMhz && mhz == maxMhz -> max(minIndex, maxIndex)
            mhz == maxMhz -> maxIndex
            else -> decimalSteps - 1
        }
        val minBound = minDecimalIndexFor(mhz)
        return raw.coerceIn(minBound, decimalSteps - 1)
    }

    LaunchedEffect(currentFreqKHz, stepKHz, minMhz, maxMhz) {
        val mhz = (currentFreqKHz / 1000).coerceIn(minMhz, maxMhz)
        val minIndex = minDecimalIndexFor(mhz)
        val maxIndex = maxDecimalIndexFor(mhz)
        val decimalIndex = ((currentFreqKHz % 1000) / stepKHz).coerceIn(minIndex, maxIndex)
        selectedMHz = mhz
        selectedDecimalIndex = decimalIndex
    }

    LaunchedEffect(minKHz, maxKHz, stepKHz) {
        snapshotFlow { selectedMHz to selectedDecimalIndex }
            .debounce(400)
            .collectLatest { (mhz, decimalIndex) ->
                val requestedKHz = mhz * 1000 + decimalIndex * stepKHz
                val clampedKHz = requestedKHz.coerceIn(minKHz, maxKHz)
                onTuneDirect(clampedKHz / 1000.0)
            }
    }

    val mhzDisplayValues = remember(minMhz, maxMhz) {
        Array(mhzSteps) { index -> (minMhz + index).toString() }
    }

    val decimalDisplayValues = remember(stepKHz) {
        Array(decimalSteps) { index ->
            String.format(Locale.ROOT, "%02d", (index * stepKHz) / 10)
        }
    }

    val minDecimalIndexForSelectedMhz = minDecimalIndexFor(selectedMHz)
    val maxDecimalIndexForSelectedMhz = maxDecimalIndexFor(selectedMHz)

    val isControlReady = state.isConnected

    LaunchedEffect(minMhz, maxMhz) {
        val clampedMhz = selectedMHz.coerceIn(minMhz, maxMhz)
        if (clampedMhz != selectedMHz) {
            selectedMHz = clampedMhz
        }
    }

    LaunchedEffect(selectedMHz, minDecimalIndexForSelectedMhz, maxDecimalIndexForSelectedMhz) {
        val clampedIndex = selectedDecimalIndex.coerceIn(minDecimalIndexForSelectedMhz, maxDecimalIndexForSelectedMhz)
        if (clampedIndex != selectedDecimalIndex) {
            selectedDecimalIndex = clampedIndex
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MHz Picker
                val pickerTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
                AndroidView(
                    modifier = Modifier.width(100.dp),
                    factory = { context ->
                        NumberPicker(context).apply {
                            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                            wrapSelectorWheel = false
                        }
                    },
                    update = { picker ->
                        val desiredMin = 0
                        val desiredMax = mhzSteps - 1
                        val currentValues = picker.displayedValues
                        val needsValuesReset = currentValues == null ||
                            currentValues.size != mhzDisplayValues.size ||
                            !currentValues.contentEquals(mhzDisplayValues)
                        if (needsValuesReset) {
                            picker.displayedValues = null
                        }
                        if (picker.minValue != desiredMin) {
                            picker.minValue = desiredMin
                        }
                        if (picker.maxValue != desiredMax) {
                            picker.maxValue = desiredMax
                        }
                        if (needsValuesReset) {
                            picker.displayedValues = mhzDisplayValues
                            picker.invalidate()
                        }
                        picker.setTextColorCompat(pickerTextColor)
                        val coercedValue = (selectedMHz - minMhz).coerceIn(0, mhzSteps - 1)
                        if (picker.value != coercedValue) {
                            picker.value = coercedValue
                        }
                        picker.isEnabled = isControlReady
                        picker.setOnValueChangedListener { _, _, newVal ->
                            selectedMHz = newVal + minMhz
                        }
                    }
                )
                Text(text = ".", style = MaterialTheme.typography.headlineMedium)
                // Decimal Picker
                AndroidView(
                    modifier = Modifier.width(100.dp),
                    factory = { context ->
                        NumberPicker(context).apply {
                            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                            wrapSelectorWheel = decimalSteps > 1
                        }
                    },
                    update = { picker ->
                        val desiredMin = 0
                        val desiredMax = decimalSteps - 1
                        val currentValues = picker.displayedValues
                        val needsValuesReset = currentValues == null ||
                            currentValues.size != decimalDisplayValues.size ||
                            !currentValues.contentEquals(decimalDisplayValues)
                        if (needsValuesReset) {
                            picker.displayedValues = null
                        }
                        if (picker.minValue != desiredMin) {
                            picker.minValue = desiredMin
                        }
                        if (picker.maxValue != desiredMax) {
                            picker.maxValue = desiredMax
                        }
                        if (needsValuesReset) {
                            picker.displayedValues = decimalDisplayValues
                            picker.invalidate()
                        }
                        picker.setTextColorCompat(pickerTextColor)
                        val coercedValue = selectedDecimalIndex.coerceIn(
                            minDecimalIndexForSelectedMhz,
                            maxDecimalIndexForSelectedMhz
                        )
                        if (picker.value != coercedValue) {
                            picker.value = coercedValue
                        }
                        picker.wrapSelectorWheel = decimalSteps > 1
                        picker.isEnabled = isControlReady
                        picker.setOnValueChangedListener { numberPicker, oldVal, newVal ->
                            var adjustedNewVal = newVal
                            if (decimalSteps > 1) {
                                if (oldVal == decimalSteps - 1 && newVal == 0) {
                                    selectedMHz = (selectedMHz + 1).coerceAtMost(maxMhz)
                                } else if (oldVal == 0 && newVal == decimalSteps - 1) {
                                    selectedMHz = (selectedMHz - 1).coerceAtLeast(minMhz)
                                }
                            }
                            val minIndexForCurrentMhz = minDecimalIndexFor(selectedMHz)
                            val maxIndexForCurrentMhz = maxDecimalIndexFor(selectedMHz)
                            adjustedNewVal = adjustedNewVal.coerceIn(minIndexForCurrentMhz, maxIndexForCurrentMhz)
                            if (numberPicker.value != adjustedNewVal) {
                                numberPicker.value = adjustedNewVal
                            }
                            selectedDecimalIndex = adjustedNewVal
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TunerSection(
    state: UiState,
    onTuneDirect: (Double) -> Unit,
    formatSignal: (TunerState?, SignalUnit) -> String,
    currentPty: (TunerState?) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val tunerState = state.tunerState
        if (tunerState != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RdsPsPiContent(tunerState)
                    RdsPtyEccContent(tunerState, currentPty)
                    RdsRadiotextContent(tunerState)
                }
            }
        }
        SignalStrengthCard(
            tunerState = tunerState,
            signalUnit = state.signalUnit,
            isConnecting = state.isConnecting,
            formatSignal = formatSignal
        )
        FrequencyControlsCard(state, onTuneDirect)
    }
}

@Composable
private fun SignalStrengthCard(
    tunerState: TunerState?,
    signalUnit: SignalUnit,
    isConnecting: Boolean,
    formatSignal: (TunerState?, SignalUnit) -> String
) {
    val signalValue = tunerState?.signalDbf
    val progress = signalValue
        ?.coerceIn(0.0, SIGNAL_MAX_DBF)
        ?.div(SIGNAL_MAX_DBF)
        ?.toFloat()
        ?: 0f
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(stringResource(id = R.string.signal_label_prefix))
                    }
                    append(formatSignal(tunerState, signalUnit))
                },
                style = MaterialTheme.typography.titleMedium
            )
            if (isConnecting || signalValue == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private const val SIGNAL_MAX_DBF = 130.0
private const val DEFAULT_MIN_FREQUENCY_KHZ = 65000
private const val DEFAULT_MAX_FREQUENCY_KHZ = 108_000
private const val DEFAULT_FREQUENCY_STEP_KHZ = 100

@Suppress("DEPRECATION")
private fun NumberPicker.setTextColorCompat(@ColorInt color: Int) {
    try {
        val selectorWheelPaintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
        selectorWheelPaintField.isAccessible = true
        val paint = selectorWheelPaintField.get(this) as? Paint
        paint?.color = color
    } catch (_: Exception) {
        // ignore
    }
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is TextView) {
            child.setTextColor(color)
        }
    }
    invalidate()
}

@Composable
private fun StatusSection(
    state: UiState,
    formatSignal: (TunerState?, SignalUnit) -> String
) {
    val signalValue = state.tunerState?.signalDbf
    val progress = signalValue
        ?.coerceIn(0.0, SIGNAL_MAX_DBF)
        ?.div(SIGNAL_MAX_DBF)
        ?.toFloat()
        ?: 0f
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isConnecting || signalValue == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RdsLabelText(text = stringResource(id = R.string.signal_label, ""))
                Spacer(Modifier.width(8.dp))
                Text(text = formatSignal(state.tunerState, state.signalUnit))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RdsLabelText(text = stringResource(id = R.string.users_label, ""))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.tunerState?.users?.toString()
                        ?: stringResource(id = R.string.default_value)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RdsLabelText(text = stringResource(id = R.string.audio_label, ""))
                Spacer(Modifier.width(8.dp))
                val audioStatus =
                    if (state.audioPlaying) stringResource(id = R.string.audio_playing) else stringResource(
                        id = R.string.audio_stopped
                    )
                Text(text = audioStatus)
            }
        }
    }
}

@Composable
private fun SettingsSection(
    state: UiState,
    onUpdateSettings: (signalUnit: SignalUnit, networkBuffer: Int, playerBuffer: Int, restartAudioOnTune: Boolean) -> Unit
) {
    var signalUnit by remember(state.signalUnit) { mutableStateOf(state.signalUnit) }
    var networkBuffer by remember(state.networkBuffer) { mutableStateOf(state.networkBuffer.toString()) }
    var playerBuffer by remember(state.playerBuffer) { mutableStateOf(state.playerBuffer.toString()) }
    var restartAudioOnTune by remember(state.restartAudioOnTune) { mutableStateOf(state.restartAudioOnTune) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RdsLabelText(
                    text = stringResource(id = R.string.settings_display_title),
                )
                SignalUnitSelector(
                    selected = signalUnit,
                    onSignalUnitSelected = { signalUnit = it })
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RdsLabelText(
                    text = stringResource(id = R.string.settings_audio_buffering_title),
                )
                Text(
                    text = stringResource(id = R.string.settings_audio_buffering_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                RdsLabelText(text = stringResource(id = R.string.settings_network_buffer_label))
                OutlinedTextField(
                    value = networkBuffer,
                    onValueChange = { networkBuffer = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(id = R.string.settings_network_buffer_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                RdsLabelText(text = stringResource(id = R.string.settings_player_buffer_label))
                OutlinedTextField(
                    value = playerBuffer,
                    onValueChange = { playerBuffer = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(id = R.string.settings_player_buffer_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { restartAudioOnTune = !restartAudioOnTune }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = restartAudioOnTune,
                        onCheckedChange = { restartAudioOnTune = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    RdsLabelText(text = stringResource(id = R.string.settings_restart_audio_on_tune))
                }
            }
        }
        Button(
            onClick = {
                onUpdateSettings(
                    signalUnit,
                    networkBuffer.toIntOrNull() ?: state.networkBuffer,
                    playerBuffer.toIntOrNull() ?: state.playerBuffer,
                    restartAudioOnTune
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.apply_settings))
        }
    }
}

@Composable
private fun SignalUnitSelector(
    selected: SignalUnit,
    onSignalUnitSelected: (SignalUnit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RdsLabelText(
            text = stringResource(id = R.string.signal_unit),
        )
        OutlinedButton(onClick = { expanded = true }) {
            Text(text = selected.displayName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SignalUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.displayName) },
                    onClick = {
                        onSignalUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ControlButtons(
    state: UiState,
    onToggleEq: () -> Unit,
    onToggleIms: () -> Unit,
    onCycleAntenna: () -> Unit,
    antennaLabel: () -> String
) {
    val canSwitchAntenna = state.tunerInfo?.canSwitchAntenna() == true
    val imsActive = state.tunerState?.ims == true
    val eqActive = state.tunerState?.eq == true
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ControlToggleButton(
                    text = stringResource(id = if (imsActive) R.string.ims_on else R.string.ims_off),
                    pressed = imsActive,
                    onClick = onToggleIms,
                    enabled = state.isConnected,
                    modifier = Modifier.weight(1f)
                )
                ControlToggleButton(
                    text = stringResource(id = if (eqActive) R.string.eq_on else R.string.eq_off),
                    pressed = eqActive,
                    onClick = onToggleEq,
                    enabled = state.isConnected,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = onCycleAntenna,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isConnected && canSwitchAntenna
            ) {
                Text(text = stringResource(id = R.string.antenna_switch))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RdsLabelText(text = stringResource(id = R.string.antenna_current, ""))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = antennaLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ControlToggleButton(
    text: String,
    pressed: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ButtonDefaults.filledTonalButtonColors(
        containerColor = if (pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (pressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    ) {
        Text(text = text)
    }
}

@Composable
private fun RdsLabelText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun RdsLabelValueRow(
    label: String,
    modifier: Modifier = Modifier,
    valueContent: @Composable (Modifier) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RdsLabelText(text = label)
        Spacer(modifier = Modifier.width(8.dp))
        valueContent(Modifier.weight(1f))
    }
}

@Composable
private fun RdsPsPiContent(tuner: TunerState?) {
    val piValue = tuner?.pi ?: stringResource(id = R.string.default_value)
    val displayPiValue = if (piValue.contains('?')) "    " else piValue
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RdsLabelText(text = stringResource(id = R.string.rds_ps_label))
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            AnnotatedErrorText(
                tuner?.ps ?: stringResource(id = R.string.default_value),
                tuner?.psErrors ?: emptyList()
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        RdsLabelText(text = stringResource(id = R.string.rds_pi_label, ""))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = displayPiValue)
    }
}

private const val RDS_RADIOTEXT_LENGTH = 64

private fun String.padToRadiotextLength(): String {
    val trimmed = take(RDS_RADIOTEXT_LENGTH)
    if (trimmed.isEmpty()) {
        return " ".repeat(RDS_RADIOTEXT_LENGTH)
    }
    return trimmed.padEnd(RDS_RADIOTEXT_LENGTH, ' ')
}

@Composable
private fun RdsRadiotextContent(tuner: TunerState?) {
    RdsLabelText(text = stringResource(id = R.string.radiotext_label))
    val baseStyle = MaterialTheme.typography.bodyMedium
    val radiotextStyle = baseStyle.copy(
        fontSize = baseStyle.fontSize * 0.8f,
        fontFamily = FontFamily.Monospace
    )
    val lineModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = lineModifier,
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                AnnotatedErrorText(
                    text = (tuner?.rt0 ?: "").padToRadiotextLength(),
                    errors = tuner?.rt0Errors ?: emptyList(),
                    minLines = 1,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    style = radiotextStyle
                )
            }
        }
        Surface(
            modifier = lineModifier,
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                AnnotatedErrorText(
                    text = (tuner?.rt1 ?: "").padToRadiotextLength(),
                    errors = tuner?.rt1Errors ?: emptyList(),
                    minLines = 1,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    style = radiotextStyle
                )
            }
        }
    }
}

@Composable
private fun RdsSection(
    state: UiState,
    currentPty: (TunerState?) -> String
) {
    val tuner = state.tunerState
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RdsPsPiContent(tuner)
            RdsPtyEccContent(tuner, currentPty)
            val country = tuner?.countryName ?: tuner?.countryIso
            if (!country.isNullOrBlank()) {
                RdsLabelValueRow(label = stringResource(id = R.string.country_label, "")) { valueModifier ->
                    Text(
                        text = country,
                        modifier = valueModifier
                    )
                }
            }
            val flags = tuner?.flags()
            if (!flags.isNullOrBlank()) {
                RdsLabelValueRow(label = stringResource(id = R.string.flags_label, "")) { valueModifier ->
                    Text(
                        text = flags,
                        modifier = valueModifier
                    )
                }
            }
            tuner?.diDisplay()?.let { di ->
                RdsLabelValueRow(label = stringResource(id = R.string.rds_di_label, "")) { valueModifier ->
                    Text(
                        text = di,
                        modifier = valueModifier
                    )
                }
            }
            val afText =
                tuner?.afList?.size?.let { stringResource(id = R.string.af_frequencies, it) }
                    ?: stringResource(id = R.string.none)
            RdsLabelValueRow(label = stringResource(id = R.string.rds_af_label, "")) { valueModifier ->
                Text(
                    text = afText,
                    modifier = valueModifier
                )
            }
            RdsRadiotextContent(tuner)
        }
    }
}

@Composable
private fun RdsPtyEccContent(tuner: TunerState?, currentPty: (TunerState?) -> String) {
    val ecc = tuner?.ecc?.takeUnless { it.isBlank() } ?: "   "
    val pty = currentPty(tuner).trimStart()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: PTY label (green) + value
        RdsLabelText(text = stringResource(id = R.string.rds_pty_label, ""))
        Spacer(Modifier.width(8.dp))
        Text(pty)

        // Push right group to the edge
        Spacer(Modifier.weight(1f))

        // Right: ECC label (green) + value aligned to the right
        RdsLabelText(text = stringResource(id = R.string.rds_ecc_label, ""))
        Spacer(Modifier.width(8.dp))
        Text(
            text = ecc,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun AnnotatedErrorText(
    text: String,
    errors: List<Int>,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = LocalTextStyle.current
) {
    val sanitized = text.ifEmpty { " " }
    val annotated = buildAnnotatedString {
        sanitized.forEachIndexed { index, c ->
            val hasError = errors.getOrNull(index)?.let { it > 0 } ?: false
            if (hasError) {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))) {
                    append(c)
                }
            } else {
                append(c)
            }
        }
    }
    Text(
        text = annotated,
        minLines = minLines,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow,
        style = style
    )
}

@Composable
private fun StationSection(state: UiState) {
    val tx = state.tunerState?.txInfo
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StationDetailRow(
                labelRes = R.string.station_name_label,
                value = tx?.name ?: stringResource(id = R.string.default_value)
            )
            StationDetailRow(
                labelRes = R.string.station_location_label,
                value = tx?.city ?: stringResource(id = R.string.default_value)
            )
            StationDetailRow(
                labelRes = R.string.station_country_label,
                value = tx?.countryCode ?: stringResource(id = R.string.default_value)
            )
            StationDetailRow(
                labelRes = R.string.station_distance_label,
                value = tx?.distanceKm?.let { stringResource(id = R.string.km_unit, it) }
                    ?: stringResource(id = R.string.default_value)
            )
            StationDetailRow(
                labelRes = R.string.station_power_label,
                value = tx?.erpKw?.let { stringResource(id = R.string.kw_unit, it) }
                    ?: stringResource(id = R.string.default_value)
            )
            StationDetailRow(
                labelRes = R.string.station_polarization_label,
                value = tx?.polarization ?: stringResource(id = R.string.default_value)
            )
            StationDetailRow(
                labelRes = R.string.station_azimuth_label,
                value = tx?.azimuthDeg?.let { stringResource(id = R.string.deg_unit, it) }
                    ?: stringResource(id = R.string.default_value)
            )
        }
    }
}

@Composable
private fun StationDetailRow(@StringRes labelRes: Int, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RdsLabelText(
            text = stringResource(id = labelRes, ""),
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SpectrumSection(
    state: UiState,
    onScan: () -> Unit,
    onRefreshSpectrum: () -> Unit,
    onTuneDirect: (Double) -> Unit,
    onDragStateChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isScanning) {
                    LinearProgressIndicator()
                }
            }
            val spectrum = state.spectrum
            if (spectrum.isEmpty()) {
                onDragStateChange(false)
                Text(text = stringResource(id = R.string.spectrum_unavailable))
            } else {
                val minSpectrumFreq = spectrum.first().frequencyMHz
                val maxSpectrumFreq = spectrum.last().frequencyMHz
                val initialFreq = (state.tunerState?.freqMHz ?: state.pendingFrequencyMHz ?: minSpectrumFreq)
                    .coerceIn(minSpectrumFreq, maxSpectrumFreq)

                var sliderValue by remember(minSpectrumFreq, maxSpectrumFreq) { mutableStateOf(initialFreq) }

                LaunchedEffect(state.tunerState?.freqMHz, minSpectrumFreq, maxSpectrumFreq) {
                    state.tunerState?.freqMHz?.let { tuned ->
                        sliderValue = tuned.coerceIn(minSpectrumFreq, maxSpectrumFreq)
                        onDragStateChange(false)
                    }
                }

                val displayFreq = ((sliderValue * 10).roundToInt() / 10.0)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RdsLabelText(text = stringResource(id = R.string.spectrum_selected_frequency_label))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.spectrum_selected_frequency_value, displayFreq),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                SpectrumGraph(points = spectrum, highlightFreq = sliderValue)

                val sliderInteraction = remember { MutableInteractionSource() }
                val sliderDragging by sliderInteraction.collectIsDraggedAsState()
                LaunchedEffect(sliderDragging) {
                    onDragStateChange(sliderDragging)
                }

                val sliderSteps = ((maxSpectrumFreq - minSpectrumFreq) * 10).roundToInt().coerceAtLeast(1) - 1
                Slider(
                    value = sliderValue.toFloat(),
                    onValueChange = { raw ->
                        val snapped = ((raw * 10f).roundToInt() / 10.0)
                            .coerceIn(minSpectrumFreq, maxSpectrumFreq)
                        sliderValue = snapped
                    },
                    onValueChangeFinished = {
                        val snapped = ((sliderValue * 10).roundToInt() / 10.0)
                        val currentFreq = state.tunerState?.freqMHz
                        if (currentFreq == null || abs(currentFreq - snapped) >= 0.0001) {
                            onTuneDirect(snapped)
                        }
                        onDragStateChange(false)
                    },
                    valueRange = minSpectrumFreq.toFloat()..maxSpectrumFreq.toFloat(),
                    steps = sliderSteps.coerceAtLeast(0),
                    interactionSource = sliderInteraction
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onScan, enabled = !state.isScanning) {
                    Text(text = stringResource(id = R.string.start_scan))
                }
                Button(onClick = onRefreshSpectrum) {
                    Text(text = stringResource(id = R.string.refresh_spectrum))
                }
            }
        }
    }
}

@Composable
private fun SpectrumGraph(
    points: List<SpectrumPoint>,
    highlightFreq: Double
) {
    if (points.isEmpty()) {
        Text(text = stringResource(id = R.string.spectrum_unavailable))
        return
    }
    val minFreq = points.first().frequencyMHz
    val maxFreq = points.last().frequencyMHz
    val freqSpan = (maxFreq - minFreq).coerceAtLeast(0.0001)
    val maxSig = points.maxOfOrNull { it.signalDbf }?.coerceAtLeast(130.0) ?: 130.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val strokeWidthPx = with(LocalDensity.current) { 2.dp.toPx() }

    Card {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val width = size.width
            val height = size.height

            drawRect(color = surfaceColor)
            val path = Path()
            points.forEachIndexed { index, point ->
                val ratio = ((point.frequencyMHz - minFreq) / freqSpan).toFloat().coerceIn(0f, 1f)
                val x = ratio * width
                val normalized = point.signalDbf.coerceIn(0.0, maxSig).toFloat() / maxSig.toFloat()
                val y = height - (normalized * height)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(path, color = secondaryColor, style = Stroke(width = strokeWidthPx))
            if (highlightFreq in minFreq..maxFreq) {
                val highlightRatio = ((highlightFreq - minFreq) / freqSpan).toFloat().coerceIn(0f, 1f)
                val x = highlightRatio * width
                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, height),
                    strokeWidth = strokeWidthPx
                )
            }
        }
    }
}
