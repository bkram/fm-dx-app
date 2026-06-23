package org.fmdx.app

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import com.mikepenz.markdown.m3.Markdown
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.seo4d696b75.compose.material3.picker.NumberPicker
import com.seo4d696b75.compose.material3.picker.Picker
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.fmdx.app.data.ConnectionType
import org.fmdx.app.model.PublicServer
import org.fmdx.app.model.SignalUnit
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import org.fmdx.app.model.SpectrumPoint
import org.fmdx.app.model.TunerInfo
import org.fmdx.app.model.TunerState
import org.fmdx.app.model.TxInfo
import org.fmdx.app.ui.theme.FmDxTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun FmDxApp(
    state: UiState,
    snackbarHostState: SnackbarHostState,
    onUpdateUrl: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onConnectUsb: () -> Unit,
    onSetConnectionMode: (direct: Boolean) -> Unit = {},
    onToggleAudio: () -> Unit,
    onTuneDirect: (Double) -> Unit,
    onToggleEq: () -> Unit,
    onToggleIms: () -> Unit,
    onToggleStereoMode: () -> Unit,
    onCycleAntenna: () -> Unit,
    onScan: () -> Unit,
    formatSignal: (TunerState?, SignalUnit) -> String,
    currentPty: (TunerState?) -> String,
    antennaLabel: () -> String,
    onUpdateSettings: (signalUnit: SignalUnit, networkBuffer: Int, playerBuffer: Int, restartAudioOnTune: Boolean, passThroughEnabled: Boolean) -> Unit,
    onShowPublicServerPicker: () -> Unit,
    onHidePublicServerPicker: () -> Unit,
    onRefreshPublicServers: () -> Unit,
    onUpdatePublicServerQuery: (String) -> Unit,
    onSelectPublicServer: (PublicServer) -> Unit,
    onRemoveRecentServer: (String) -> Unit = {}
) {
    val showSettingsState = rememberSaveable { mutableStateOf(false) }
    val showAboutState = rememberSaveable { mutableStateOf(false) }
    val showSettings by showSettingsState
    val showAbout by showAboutState

    val haptics = LocalHapticFeedback.current
    val hapticTap = remember(haptics) {
        { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    val hConnect = { hapticTap(); onConnect() }
    val hConnectUsb = { hapticTap(); onConnectUsb() }
    val hDisconnect = { hapticTap(); onDisconnect() }
    val hToggleAudio = { hapticTap(); onToggleAudio() }
    val hToggleEq = { hapticTap(); onToggleEq() }
    val hToggleIms = { hapticTap(); onToggleIms() }
    val hToggleStereoMode = { hapticTap(); onToggleStereoMode() }
    val hCycleAntenna = { hapticTap(); onCycleAntenna() }
    val hScan = { hapticTap(); onScan() }
    val hTuneDirect: (Double) -> Unit = { freq -> hapticTap(); onTuneDirect(freq) }
    val hSelectPublicServer: (PublicServer) -> Unit = { server ->
        hapticTap(); onSelectPublicServer(server)
    }
    val hShowPublicServerPicker = { hapticTap(); onShowPublicServerPicker() }

    when {
        showSettings -> {
            SettingsScreen(
                state = state,
                onUpdateSettings = onUpdateSettings,
                onBack = { showSettingsState.value = false }
            )
        }

        showAbout -> {
            AboutScreen(onBack = { showAboutState.value = false })
        }

        else -> {
            MainScreen(
                state = state,
                snackbarHostState = snackbarHostState,
                onUpdateUrl = onUpdateUrl,
                onConnect = hConnect,
                onDisconnect = hDisconnect,
                onConnectUsb = hConnectUsb,
                onSetConnectionMode = onSetConnectionMode,
                onToggleAudio = hToggleAudio,
                onTuneDirect = hTuneDirect,
                onToggleEq = hToggleEq,
                onToggleIms = hToggleIms,
                onToggleStereoMode = hToggleStereoMode,
                onCycleAntenna = hCycleAntenna,
                onScan = hScan,
                formatSignal = formatSignal,
                currentPty = currentPty,
                antennaLabel = antennaLabel,
                onShowSettings = { showSettingsState.value = true },
                onShowAbout = { showAboutState.value = true },
                onShowPublicServerPicker = hShowPublicServerPicker,
                onHidePublicServerPicker = onHidePublicServerPicker,
                onRefreshPublicServers = onRefreshPublicServers,
                onUpdatePublicServerQuery = onUpdatePublicServerQuery,
                onSelectPublicServer = hSelectPublicServer,
                onRemoveRecentServer = onRemoveRecentServer
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
    onConnectUsb: () -> Unit = {},
    onSetConnectionMode: (direct: Boolean) -> Unit = {},
    onToggleAudio: () -> Unit,
    onTuneDirect: (Double) -> Unit,
    onToggleEq: () -> Unit,
    onToggleIms: () -> Unit,
    onToggleStereoMode: () -> Unit,
    onCycleAntenna: () -> Unit,
    onScan: () -> Unit,
    formatSignal: (TunerState?, SignalUnit) -> String,
    currentPty: (TunerState?) -> String,
    antennaLabel: () -> String,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    onShowPublicServerPicker: () -> Unit,
    onHidePublicServerPicker: () -> Unit,
    onRefreshPublicServers: () -> Unit,
    onUpdatePublicServerQuery: (String) -> Unit,
    onSelectPublicServer: (PublicServer) -> Unit,
    onRemoveRecentServer: (String) -> Unit = {}
) {
    var isSpectrumDragging by remember { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    val tabs = buildList {
        add(
            SectionTab(
                titleRes = R.string.connection_tab_title,
                scrollable = false,
                requiresConnection = false
            ) {
                ConnectionSection(
                    state = state,
                    onUpdateUrl = onUpdateUrl,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onShowPublicServerPicker = onShowPublicServerPicker,
                    onConnectUsb = onConnectUsb,
                    onSetConnectionMode = onSetConnectionMode,
                    onRemoveRecentServer = onRemoveRecentServer
                )
            }
        )
        if (state.isConnected && state.connectionType == ConnectionType.SERVER) {
            add(
                SectionTab(
                    titleRes = R.string.server_info_tab_title,
                    scrollable = false,
                    requiresConnection = true
                ) { ServerInfoSection(state) }
            )
        }
        if (!state.isConnected) {
            add(
                SectionTab(
                    titleRes = R.string.help_tab_title,
                    scrollable = false,
                    requiresConnection = false
                ) { HelpSection() }
            )
        }
        if (state.isConnected) {
            add(SectionTab(R.string.tuner) {
                TunerSection(
                    state = state,
                    onTuneDirect = onTuneDirect,
                    formatSignal = formatSignal,
                    currentPty = currentPty,
                    onToggleEq = onToggleEq,
                    onToggleIms = onToggleIms,
                    onToggleStereoMode = onToggleStereoMode,
                    onCycleAntenna = onCycleAntenna,
                    antennaLabel = antennaLabel
                )
            })
            add(SectionTab(R.string.rds) { InformationSection(state, currentPty) })
            if (state.isSpectrumAvailable) {
                add(
                    SectionTab(R.string.spectrum) {
                        SpectrumSection(
                            state = state,
                            onScan = onScan,
                            onTuneDirect = onTuneDirect,
                            onDragStateChange = { dragging -> isSpectrumDragging = dragging }
                        )
                    }
                )
            }
        }
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val spectrumPageIndex = tabs.indexOfFirst { it.titleRes == R.string.spectrum }
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(state.isConnected) {
        if (!state.isConnected) {
            pagerState.scrollToPage(0)
            return@LaunchedEffect
        }
        // On a fresh connection, surface the Tuner tab. The tab list grows the
        // moment isConnected flips, but PagerState.pageCount only refreshes on the
        // next measure pass — so wait for the new pages to register before
        // scrolling, otherwise animateScrollToPage clamps to the stale page count
        // and we never reach the Tuner. Keying this effect on isConnected only
        // (not tabs.size) also stops a later spectrum-availability change from
        // cancelling the in-flight scroll animation.
        val tunerPageIndex = tabs.indexOfFirst { it.titleRes == R.string.tuner }
        if (tunerPageIndex > 0) {
            snapshotFlow { pagerState.pageCount }.first { it > tunerPageIndex }
            pagerState.animateScrollToPage(tunerPageIndex)
        }
    }

    val selectedTabIndex = clampTabIndex(pagerState.currentPage, tabs.size)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.main_title),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConnectionStatusIndicator(
                            isConnected = state.isConnected,
                            isConnecting = state.isConnecting
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onToggleAudio,
                            enabled = state.isConnected
                        ) {
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
            PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, tab ->
                    val tabTag = "main_tab_${tab.titleRes}"
                    Tab(
                        modifier = Modifier.testTag(tabTag),
                        selected = selectedTabIndex == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = stringResource(id = tab.titleRes)) },
                        enabled = !tab.requiresConnection || state.isConnected
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = tabs.size > 1 && (!isSpectrumDragging || spectrumPageIndex == -1 || pagerState.currentPage != spectrumPageIndex),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("main_sections_pager")
            ) { page ->
                key(page) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .padding(16.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        val tab = tabs.getOrNull(page)
                        val contentModifier = Modifier
                            .fillMaxWidth()
                            .let { base ->
                                if (tab?.scrollable != false) {
                                    base.verticalScroll(scrollState)
                                } else {
                                    base
                                }
                            }
                        Column(
                            modifier = contentModifier,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            tab?.content?.invoke()
                        }
                    }
                }
            }
        }
    }

    if (state.publicServerPickerState.isVisible) {
        PublicServerPickerSheet(
            pickerState = state.publicServerPickerState,
            onDismiss = onHidePublicServerPicker,
            onRefresh = onRefreshPublicServers,
            onQueryChange = onUpdatePublicServerQuery,
            onSelect = onSelectPublicServer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: UiState,
    onUpdateSettings: (signalUnit: SignalUnit, networkBuffer: Int, playerBuffer: Int, restartAudioOnTune: Boolean, passThroughEnabled: Boolean) -> Unit,
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
                .imePadding()
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
    val fmdxOrgSiteLabel = stringResource(id = R.string.about_site_link)
    val fmdxOrgSiteUrl = stringResource(id = R.string.about_site_url)
    val fmdxWebServerUrl = stringResource(id = R.string.about_fmdxwebserver_url)
    val fmdxWebServerLabel = stringResource(id = R.string.about_fmdxwebserver_title)
    val tefLoggerLabel = stringResource(id = R.string.about_teflogger_title)
    val tefLoggerUrl = stringResource(id = R.string.about_teflogger_url)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.about_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = versionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = stringResource(id = R.string.about_links_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    AboutLinkItem(icon = Icons.Filled.Code, label = githubLabel) {
                        uriHandler.openUri(githubUrl)
                    }
                    AboutLinkItem(icon = Icons.Filled.Dns, label = fmdxWebServerLabel) {
                        uriHandler.openUri(fmdxWebServerUrl)
                    }
                    AboutLinkItem(icon = Icons.Filled.TravelExplore, label = tefLoggerLabel) {
                        uriHandler.openUri(tefLoggerUrl)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    AboutLinkItem(icon = Icons.Filled.Public, label = fmdxOrgSiteLabel) {
                        uriHandler.openUri(fmdxOrgSiteUrl)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutLinkItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text = label) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

internal fun clampTabIndex(currentPage: Int, tabCount: Int): Int {
    if (tabCount <= 0) return 0
    return currentPage.coerceIn(0, tabCount - 1)
}

private data class SectionTab(
    @param:StringRes val titleRes: Int,
    val scrollable: Boolean = true,
    val requiresConnection: Boolean = true,
    val content: @Composable () -> Unit
)

@Composable
private fun HelpSection() {
    val uriHandler = LocalUriHandler.current
    val helpItems = listOf(
        HelpItem(
            title = stringResource(id = R.string.help_getting_started_title),
            description = stringResource(id = R.string.help_getting_started_description)
        ),
        HelpItem(
            title = stringResource(id = R.string.help_tuning_title),
            description = stringResource(id = R.string.help_tuning_description)
        ),
        HelpItem(
            title = stringResource(id = R.string.help_troubleshooting_title),
            description = stringResource(id = R.string.help_troubleshooting_description)
        ),
        HelpItem(
            title = stringResource(id = R.string.help_more_support_title),
            description = stringResource(id = R.string.help_more_support_description),
            linkText = stringResource(id = R.string.help_more_support_link),
            linkUrl = stringResource(id = R.string.help_discord_url)
        )
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(helpItems) { item ->
            HelpCard(
                title = item.title,
                description = item.description,
                linkText = item.linkText,
                onLinkClick = item.linkUrl?.let { url ->
                    { uriHandler.openUri(url) }
                }
            )
        }
    }
}

@Composable
private fun HelpCard(
    title: String,
    description: String,
    linkText: String? = null,
    onLinkClick: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (linkText != null && onLinkClick != null) {
                TextButton(onClick = onLinkClick) {
                    Text(text = linkText)
                }
            }
        }
    }
}

private data class HelpItem(
    val title: String,
    val description: String,
    val linkText: String? = null,
    val linkUrl: String? = null
)

private data class RdsFlagUi(
    @param:StringRes val shortLabelRes: Int,
    @param:StringRes val fullLabelRes: Int,
    val enabled: Boolean
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
private fun ConnectionSection(
    state: UiState,
    onUpdateUrl: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onShowPublicServerPicker: () -> Unit,
    onConnectUsb: () -> Unit = {},
    onSetConnectionMode: (direct: Boolean) -> Unit = {},
    onRemoveRecentServer: (String) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val connectAndDismissKeyboard = {
        focusManager.clearFocus(force = true)
        onConnect()
    }

    // Selected segment is the user's remembered preference (persisted across restarts).
    val mode = if (state.preferDirectMode) ConnectionMode.DIRECT else ConnectionMode.REMOTE

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == ConnectionMode.REMOTE,
                onClick = { onSetConnectionMode(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(stringResource(id = R.string.connection_mode_remote)) }
            SegmentedButton(
                selected = mode == ConnectionMode.DIRECT,
                onClick = { onSetConnectionMode(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(stringResource(id = R.string.connection_mode_direct)) }
        }

        if (mode == ConnectionMode.REMOTE) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val recentServers = state.recentServerUrls
                var recentExpanded by rememberSaveable { mutableStateOf(false) }
                val hasRecent = recentServers.isNotEmpty()

                LaunchedEffect(hasRecent) {
                    if (!hasRecent) {
                        recentExpanded = false
                    }
                }

                Box {
                    OutlinedTextField(
                        value = state.serverUrl,
                        onValueChange = {
                            recentExpanded = false
                            onUpdateUrl(it)
                        },
                        label = { Text(stringResource(id = R.string.server_url)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (hasRecent) {
                                IconButton(onClick = { recentExpanded = !recentExpanded }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = stringResource(id = R.string.recent_servers)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Uri
                        ),
                        keyboardActions = KeyboardActions(onDone = { connectAndDismissKeyboard() })
                    )

                    DropdownMenu(
                        expanded = recentExpanded && hasRecent,
                        onDismissRequest = { recentExpanded = false }
                    ) {
                        recentServers.forEach { server ->
                            DropdownMenuItem(
                                text = { Text(server) },
                                trailingIcon = {
                                    IconButton(onClick = { onRemoveRecentServer(server) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(
                                                id = R.string.recent_servers_remove
                                            )
                                        )
                                    }
                                },
                                onClick = {
                                    recentExpanded = false
                                    onUpdateUrl(server)
                                    focusManager.clearFocus(force = true)
                                }
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onShowPublicServerPicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.browse_public_servers))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.isConnected) {
                        OutlinedButton(onClick = onConnect, enabled = false) {
                            Text(text = stringResource(id = R.string.connect))
                        }
                        Button(onClick = onDisconnect) {
                            Text(text = stringResource(id = R.string.disconnect))
                        }
                    } else {
                        Button(
                            onClick = { connectAndDismissKeyboard() },
                            enabled = !state.isConnecting
                        ) {
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
                val disconnectedLabel = stringResource(id = R.string.disconnected)
                val shouldShowStatus =
                    state.statusMessage != null && state.statusMessage != disconnectedLabel
                if (shouldShowStatus) {
                    Text(
                        text = state.statusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        } else {
            DirectTunerCard(
                state = state,
                onConnectUsb = {
                    focusManager.clearFocus(force = true)
                    onConnectUsb()
                }
            )
            // Disconnect control for an active direct (USB) connection.
            if (state.isConnected && state.connectionType != ConnectionType.SERVER) {
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.disconnect))
                }
            }
        }
    }
}

private enum class ConnectionMode { REMOTE, DIRECT }

@Composable
private fun DirectTunerCard(
    state: UiState,
    onConnectUsb: () -> Unit
) {
    val tunerAttached = state.usbTunerName != null
    val canConnect = tunerAttached && !state.isConnected && !state.isConnecting
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.direct_tuner_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = R.string.direct_tuner_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Live USB attach state so the user knows whether a tuner is plugged in.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (tunerAttached) Icons.Filled.Usb else Icons.Filled.UsbOff,
                    contentDescription = null,
                    tint = if (tunerAttached) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = state.usbTunerName
                        ?: stringResource(id = R.string.usb_tuner_not_detected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (tunerAttached) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Button(
                onClick = onConnectUsb,
                enabled = canConnect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.connect_usb_tuner))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublicServerPickerSheet(
    pickerState: PublicServerPickerState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelect: (PublicServer) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        PublicServerPickerContent(
            pickerState = pickerState,
            onDismiss = onDismiss,
            onRefresh = onRefresh,
            onQueryChange = onQueryChange,
            onSelect = onSelect
        )
    }
}

@Composable
private fun PublicServerPickerContent(
    pickerState: PublicServerPickerState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelect: (PublicServer) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .heightIn(min = 0.dp, max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RdsLabelText(
                text = stringResource(id = R.string.server_picker_title),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh, enabled = !pickerState.isLoading) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(id = R.string.server_picker_refresh)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.server_picker_close)
                )
            }
        }
        OutlinedTextField(
            value = pickerState.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(text = stringResource(id = R.string.server_picker_search_label)) },
            placeholder = { Text(text = stringResource(id = R.string.server_picker_search_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(id = R.string.server_picker_search_label)
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(onSearch = { /* handled via query binding */ })
        )
        if (pickerState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        val errorMessage = pickerState.errorMessage
        when {
            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.server_picker_error_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = onRefresh) {
                            Text(text = stringResource(id = R.string.server_picker_retry))
                        }
                    }
                }
            }

            pickerState.filteredServers.isEmpty() -> {
                val emptyLabel = if (pickerState.query.isBlank()) {
                    stringResource(id = R.string.server_picker_empty_no_query)
                } else {
                    stringResource(
                        id = R.string.server_picker_empty_with_query,
                        pickerState.query
                    )
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = emptyLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onRefresh) {
                            Text(text = stringResource(id = R.string.server_picker_refresh))
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pickerState.filteredServers, key = { it.url }) { server ->
                        PublicServerCard(
                            server = server,
                            onSelect = onSelect
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PublicServerCard(
    server: PublicServer,
    onSelect: (PublicServer) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { onSelect(server) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                PublicServerStatusChip(isOnline = server.isOnline)
            }
            server.displayLocation?.let { location ->
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val tunerDetails = listOfNotNull(
                server.tuner,
                server.version?.takeIf { it.isNotBlank() }
            ).takeIf { it.isNotEmpty() }?.joinToString(separator = " • ")
            tunerDetails?.let { details ->
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val audioDetails = buildList {
                server.audioQuality?.let { add(it) }
                server.audioChannels?.let {
                    add(
                        stringResource(
                            id = R.string.server_picker_audio_channels,
                            it
                        )
                    )
                }
                server.bandwidthLimit?.let { add(it) }
            }.takeIf { it.isNotEmpty() }?.joinToString(separator = " • ")
            audioDetails?.let { details ->
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            server.description?.takeIf { it.isNotBlank() }?.let { description ->
                Markdown(content = description)
            }
            TextButton(
                onClick = { onSelect(server) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(id = R.string.server_picker_select))
            }
        }
    }
}

@Composable
private fun PublicServerStatusChip(isOnline: Boolean, modifier: Modifier = Modifier) {
    val label = if (isOnline) {
        stringResource(id = R.string.server_picker_status_online)
    } else {
        stringResource(id = R.string.server_picker_status_offline)
    }
    val containerColor = if (isOnline) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    }
    val contentColor = if (isOnline) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Box(
        modifier = modifier
            .background(containerColor, shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PublicServerCardPreview() {
    FmDxTheme {
        Surface {
            PublicServerCard(server = PublicServer.sample(), onSelect = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PublicServerPickerContentPreview() {
    val servers = List(3) { index ->
        PublicServer.sample().copy(name = "Sample Server ${index + 1}")
    }
    FmDxTheme {
        Surface {
            PublicServerPickerContent(
                pickerState = PublicServerPickerState(
                    isVisible = true,
                    servers = servers,
                    filteredServers = servers
                ),
                onDismiss = {},
                onRefresh = {},
                onQueryChange = {},
                onSelect = {}
            )
        }
    }
}

@Composable
private fun ServerInfoCard(
    tunerInfo: TunerInfo?,
    users: Int?,
    latencyMs: Double?
) {
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
                if (tunerInfo.tunerDescription.isNotBlank()) {
                    Markdown(content = tunerInfo.tunerDescription)
                }
            }
            ServerInfoMetricRow(
                label = stringResource(id = R.string.server_info_users),
                value = users?.toString() ?: stringResource(id = R.string.server_info_users_unknown)
            )
            val latencyValue = latencyMs?.let {
                val rounded = it.roundToInt().coerceAtLeast(0)
                stringResource(id = R.string.server_info_latency_value, rounded)
            } ?: stringResource(id = R.string.server_info_latency_unknown)
            ServerInfoMetricRow(
                label = stringResource(id = R.string.server_info_latency),
                value = latencyValue
            )
        }
    }
}

@Composable
private fun ServerInfoSection(state: UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ServerInfoCard(
            tunerInfo = state.tunerInfo,
            users = state.tunerState?.users,
            latencyMs = state.serverLatencyMs
        )
    }
}

@Composable
private fun ServerInfoMetricRow(label: String, value: String) {
    val headerStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
    val valueStyle = SpanStyle(color = MaterialTheme.colorScheme.onSurface)
    Text(
        text = buildAnnotatedString {
            withStyle(headerStyle) {
                append(label)
                append(": ")
            }
            withStyle(valueStyle) {
                append(value)
            }
        },
        style = MaterialTheme.typography.bodyMedium
    )
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
    val decimalSteps = max(1, 1000 / stepKHz)
    val decimalLoopCount = if (decimalSteps > 1) 30 else 1
    val decimalRangeSize = decimalSteps * decimalLoopCount
    val decimalRange =
        if (decimalSteps > 1 && decimalRangeSize > 0) 0..<decimalRangeSize else 0..0

    var decimalPickerPosition by rememberSaveable { mutableIntStateOf(0) }

    var isUserInteracting by remember { mutableStateOf(false) }
    LaunchedEffect(currentFreqKHz) {
        isUserInteracting = false
    }

    fun wrappedDecimalIndex(rawPosition: Int): Int {
        if (decimalSteps <= 1) return 0
        val mod = rawPosition % decimalSteps
        return if (mod < 0) mod + decimalSteps else mod
    }

    fun anchorPositionFor(decimalIndex: Int): Int {
        if (decimalSteps <= 1) return decimalRange.first
        val loopMid = decimalLoopCount / 2
        val anchored = loopMid * decimalSteps + decimalIndex
        return anchored.coerceIn(decimalRange.first, decimalRange.last)
    }

    fun alignPickerPositionToIndex(position: Int, index: Int): Int {
        if (decimalSteps <= 1) return decimalRange.first
        val currentWrapped = wrappedDecimalIndex(position)
        val adjusted = position - currentWrapped + index
        return adjusted.coerceIn(decimalRange.first, decimalRange.last)
    }

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

    LaunchedEffect(currentFreqKHz, stepKHz, minMhz, maxMhz, isUserInteracting) {
        if (isUserInteracting) return@LaunchedEffect
        val mhz = (currentFreqKHz / 1000).coerceIn(minMhz, maxMhz)
        val minIndex = minDecimalIndexFor(mhz)
        val maxIndex = maxDecimalIndexFor(mhz)
        val decimalIndex = ((currentFreqKHz % 1000) / stepKHz).coerceIn(minIndex, maxIndex)
        selectedMHz = mhz
        selectedDecimalIndex = decimalIndex
        decimalPickerPosition = anchorPositionFor(decimalIndex)
    }

    LaunchedEffect(minKHz, maxKHz, stepKHz) {
        snapshotFlow { Triple(selectedMHz, selectedDecimalIndex, isUserInteracting) }
            // Only tune in response to the user spinning the picker. Without this gate the
            // programmatic sync (above) emits a spurious tune to the band minimum on connect —
            // which fought the USB restore frequency and polluted the cached frequency.
            .filter { it.third }
            .debounce(400)
            .collectLatest { (mhz, decimalIndex, _) ->
                val requestedKHz = (mhz * 1000 + decimalIndex * stepKHz).coerceIn(minKHz, maxKHz)
                onTuneDirect(requestedKHz / 1000.0)
            }
    }

    val decimalDisplayValues = remember(stepKHz) {
        IntArray(max(1, decimalSteps)) { index ->
            (index * stepKHz) / 10
        }
    }

    val minDecimalIndexForSelectedMhz = minDecimalIndexFor(selectedMHz)
    val maxDecimalIndexForSelectedMhz = maxDecimalIndexFor(selectedMHz)

    val isControlReady = state.isConnected
    val decimalPickerItems = remember(decimalRangeSize, decimalSteps, stepKHz) {
        decimalRange.map { position ->
            val wrapped = wrappedDecimalIndex(position)
            val displayValue = decimalDisplayValues.getOrElse(wrapped) { wrapped }
            val displayText = displayValue.toString().padStart(2, '0')
            DecimalPickerLabel(position, displayValue, displayText)
        }.toPersistentList()
    }
    val decimalPickerIndex = (decimalPickerPosition - decimalRange.first)
        .coerceIn(0, decimalPickerItems.lastIndex.coerceAtLeast(0))

    LaunchedEffect(minMhz, maxMhz) {
        val clampedMhz = selectedMHz.coerceIn(minMhz, maxMhz)
        if (clampedMhz != selectedMHz) {
            selectedMHz = clampedMhz
        }
    }

    LaunchedEffect(
        selectedMHz,
        minDecimalIndexForSelectedMhz,
        maxDecimalIndexForSelectedMhz,
        isUserInteracting
    ) {
        val clampedIndex = selectedDecimalIndex.coerceIn(
            minDecimalIndexForSelectedMhz,
            maxDecimalIndexForSelectedMhz
        )
        if (clampedIndex != selectedDecimalIndex) {
            selectedDecimalIndex = clampedIndex
        }
        if (!isUserInteracting) {
            decimalPickerPosition = anchorPositionFor(clampedIndex)
        }
    }

    fun handleDecimalPickerChange(newPosition: Int) {
        val boundedPosition = newPosition
            .coerceIn(decimalRange.first, decimalRange.last)
            .coerceAtMost(decimalPickerItems.lastIndex.coerceAtLeast(0))
        val previousPosition = decimalPickerPosition
        if (boundedPosition == previousPosition) return
        decimalPickerPosition = boundedPosition
        if (!isControlReady || decimalSteps <= 1) {
            isUserInteracting = true
            return
        }
        val deltaSteps = boundedPosition - previousPosition
        val currentKHz = (selectedMHz * 1000) + (selectedDecimalIndex * stepKHz)
        val targetKHz = (currentKHz + deltaSteps * stepKHz).coerceIn(minKHz, maxKHz)
        val nextMhz = (targetKHz / 1000).coerceIn(minMhz, maxMhz)
        val rawDecimalIndex = ((targetKHz % 1000) / stepKHz)
        val nextDecimal = rawDecimalIndex.coerceIn(
            minDecimalIndexFor(nextMhz),
            maxDecimalIndexFor(nextMhz)
        )
        if (nextMhz != selectedMHz) {
            selectedMHz = nextMhz
        }
        if (nextDecimal != selectedDecimalIndex) {
            selectedDecimalIndex = nextDecimal
        }
        val anchored = alignPickerPositionToIndex(boundedPosition, nextDecimal)
        if (anchored != decimalPickerPosition) {
            decimalPickerPosition = anchored
        }
        isUserInteracting = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val pickerWidth = 96.dp
        val pickerHeight = 196.dp
        val pickerTextStyle = MaterialTheme.typography.headlineMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val mhzItems = remember(minMhz, maxMhz) {
                (minMhz..maxMhz).toList().toPersistentList()
            }
            val mhzSelectedValue = selectedMHz.coerceIn(minMhz, maxMhz)
            Box(
                modifier = Modifier
                    .width(pickerWidth)
                    .height(pickerHeight)
                    .alpha(if (isControlReady) 1f else 0.4f),
                contentAlignment = Center
            ) {
                NumberPicker(
                    value = mhzSelectedValue,
                    range = mhzItems,
                    onValueChange = { newValue ->
                        if (!isControlReady) return@NumberPicker
                        val coerced = newValue.coerceIn(minMhz, maxMhz)
                        if (selectedMHz != coerced) {
                            selectedMHz = coerced
                            val minForNew = minDecimalIndexFor(coerced)
                            val maxForNew = maxDecimalIndexFor(coerced)
                            val clampedDecimal = selectedDecimalIndex.coerceIn(minForNew, maxForNew)
                            if (clampedDecimal != selectedDecimalIndex) {
                                selectedDecimalIndex = clampedDecimal
                                decimalPickerPosition =
                                    alignPickerPositionToIndex(
                                        decimalPickerPosition,
                                        clampedDecimal
                                    )
                            }
                        }
                        isUserInteracting = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isControlReady,
                    labelStyle = pickerTextStyle,
                    labelSize = DpSize(pickerWidth, pickerHeight / 3),
                    dividerHeight = 2.dp
                )
                if (!isControlReady) {
                    DisabledOverlay()
                }
            }
            Text(
                text = ".",
                style = pickerTextStyle,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Box(
                modifier = Modifier
                    .width(pickerWidth)
                    .height(pickerHeight)
                    .alpha(if (isControlReady) 1f else 0.4f),
                contentAlignment = Center
            ) {
                Picker(
                    index = decimalPickerIndex,
                    values = decimalPickerItems,
                    onIndexChange = { newIndex ->
                        val item = decimalPickerItems.getOrNull(newIndex) ?: return@Picker
                        handleDecimalPickerChange(item.position)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isControlReady,
                    labelStyle = pickerTextStyle,
                    labelSize = DpSize(pickerWidth, pickerHeight / 3),
                    dividerHeight = 2.dp
                )
                if (!isControlReady) {
                    DisabledOverlay()
                }
            }
        }
    }
}

@Composable
private fun DisabledOverlay() {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
    )
}

private data class DecimalPickerLabel(
    val position: Int,
    val displayValue: Int,
    val text: String
) {
    override fun toString(): String = text
}

@Composable
private fun TunerSection(
    state: UiState,
    onTuneDirect: (Double) -> Unit,
    formatSignal: (TunerState?, SignalUnit) -> String,
    currentPty: (TunerState?) -> String,
    onToggleEq: () -> Unit,
    onToggleIms: () -> Unit,
    onToggleStereoMode: () -> Unit,
    onCycleAntenna: () -> Unit,
    antennaLabel: () -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val tunerState = state.tunerState
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RdsPsPiContent(tunerState)
                RdsPtyEccContent(tunerState, currentPty)
                RdsRadiotextContent(tunerState)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SignalStrengthInfo(
                    tunerState = tunerState,
                    signalUnit = state.signalUnit,
                    formatSignal = formatSignal,
                    modifier = Modifier.weight(1f)
                )
                val antennaPrefix = stringResource(id = R.string.antenna_current, "").trim()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentWidth(Alignment.End)
                ) {
                    Text(
                        text = antennaPrefix,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = antennaLabel(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrequencyControlsCard(
                    state = state,
                    onTuneDirect = onTuneDirect
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlButtons(
                    state = state,
                    onToggleEq = onToggleEq,
                    onToggleIms = onToggleIms,
                    onToggleStereoMode = onToggleStereoMode,
                    onCycleAntenna = onCycleAntenna
                )
            }
        }
    }
}

@Composable
private fun SignalStrengthInfo(
    tunerState: TunerState?,
    signalUnit: SignalUnit,
    formatSignal: (TunerState?, SignalUnit) -> String,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append(stringResource(id = R.string.signal_label_prefix))
            }
            append(formatSignal(tunerState, signalUnit))
        },
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
    )
}

private const val DEFAULT_MIN_FREQUENCY_KHZ = 65000
private const val DEFAULT_MAX_FREQUENCY_KHZ = 108_000
private const val DEFAULT_FREQUENCY_STEP_KHZ = 100

@Composable
private fun SettingsSection(
    state: UiState,
    onUpdateSettings: (signalUnit: SignalUnit, networkBuffer: Int, playerBuffer: Int, restartAudioOnTune: Boolean, passThroughEnabled: Boolean) -> Unit
) {
    var signalUnit by remember(state.signalUnit) { mutableStateOf(state.signalUnit) }
    var networkBuffer by remember(state.networkBuffer) { mutableStateOf(state.networkBuffer.toString()) }
    var playerBuffer by remember(state.playerBuffer) { mutableStateOf(state.playerBuffer.toString()) }
    var restartAudioOnTune by remember(state.restartAudioOnTune) { mutableStateOf(state.restartAudioOnTune) }
    var passThroughEnabled by remember(state.passThroughEnabled) { mutableStateOf(state.passThroughEnabled) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsCategoryHeader(text = stringResource(id = R.string.settings_display_title))
                Text(
                    text = stringResource(id = R.string.signal_unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SignalUnitSelector(
                    selected = signalUnit,
                    onSignalUnitSelected = { signalUnit = it }
                )
            }
        }
        LanguagePickerCard()
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsCategoryHeader(text = stringResource(id = R.string.settings_audio_buffering_title))
                Text(
                    text = stringResource(id = R.string.settings_audio_buffering_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val bufferLabel = pluralStringResource(
                    id = R.plurals.settings_current_buffers,
                    count = state.networkBuffer,
                    state.networkBuffer,
                    state.playerBuffer
                )
                Text(
                    text = bufferLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = networkBuffer,
                    onValueChange = { networkBuffer = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(id = R.string.settings_network_buffer_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = playerBuffer,
                    onValueChange = { playerBuffer = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(id = R.string.settings_player_buffer_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                SettingsSwitchRow(
                    title = stringResource(id = R.string.settings_restart_audio_on_tune),
                    checked = restartAudioOnTune,
                    onCheckedChange = { restartAudioOnTune = it }
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsCategoryHeader(text = stringResource(id = R.string.settings_pass_through_label))
                SettingsSwitchRow(
                    title = stringResource(id = R.string.settings_pass_through_label),
                    subtitle = stringResource(id = R.string.settings_pass_through_desc),
                    checked = passThroughEnabled,
                    onCheckedChange = { passThroughEnabled = it }
                )
            }
        }
        Button(
            onClick = {
                onUpdateSettings(
                    signalUnit,
                    networkBuffer.toIntOrNull() ?: state.networkBuffer,
                    playerBuffer.toIntOrNull() ?: state.playerBuffer,
                    restartAudioOnTune,
                    passThroughEnabled
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.apply_settings))
        }
    }
}

@Composable
private fun SettingsCategoryHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onCheckedChange(!checked) }
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguagePickerCard() {
    val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        .substringBefore('-')
        .lowercase()
        .takeIf { it.isNotBlank() }
    var selected by remember { mutableStateOf(currentTag) }
    val haptics = LocalHapticFeedback.current
    val choices = listOf(
        null to stringResource(id = R.string.settings_language_auto),
        "en" to "English",
        "nl" to "Nederlands"
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsCategoryHeader(text = stringResource(id = R.string.settings_language_title))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                choices.forEachIndexed { index, (tag, label) ->
                    SegmentedButton(
                        selected = selected == tag,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            selected = tag
                            AppCompatDelegate.setApplicationLocales(
                                if (tag == null) {
                                    LocaleListCompat.getEmptyLocaleList()
                                } else {
                                    LocaleListCompat.forLanguageTags(tag)
                                }
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size)
                    ) {
                        Text(text = label)
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalUnitSelector(
    selected: SignalUnit,
    onSignalUnitSelected: (SignalUnit) -> Unit
) {
    val units = SignalUnit.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        units.forEachIndexed { index, unit ->
            SegmentedButton(
                selected = selected == unit,
                onClick = { onSignalUnitSelected(unit) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = units.size)
            ) {
                Text(text = unit.displayName)
            }
        }
    }
}

@Composable
private fun ControlButtons(
    state: UiState,
    onToggleEq: () -> Unit,
    onToggleIms: () -> Unit,
    onToggleStereoMode: () -> Unit,
    onCycleAntenna: () -> Unit
) {
    val canSwitchAntenna = state.tunerInfo?.canSwitchAntenna() == true
    val imsActive = state.tunerState?.ims == true
    val eqActive = state.tunerState?.eq == true
    val isStereoForced = state.tunerState?.stereoForced == true
    // CEQ (FM_Set_ChannelEqualizer) and IMS (FM_Set_MphSuppression) are native TEF668X DSP
    // features driven by the `G<eq><ims>` command, so they work on USB too.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ControlToggleButton(
                text = stringResource(id = R.string.control_label_ceq),
                pressed = eqActive,
                onClick = onToggleEq,
                enabled = state.isConnected,
                modifier = Modifier.weight(1f)
            )
            ControlToggleButton(
                text = stringResource(id = R.string.control_label_ims),
                pressed = imsActive,
                onClick = onToggleIms,
                enabled = state.isConnected,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ControlToggleButton(
                text = stringResource(
                    id = if (isStereoForced) {
                        R.string.rds_audio_mode_mono
                    } else {
                        R.string.rds_audio_mode_stereo
                    }
                ),
                pressed = isStereoForced,
                onClick = onToggleStereoMode,
                enabled = state.isConnected,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onCycleAntenna,
                modifier = Modifier.weight(1f),
                enabled = state.isConnected && canSwitchAntenna
            ) {
                Text(text = stringResource(id = R.string.antenna_switch))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnnotatedErrorText(
                    tuner?.ps ?: stringResource(id = R.string.default_value),
                    tuner?.psErrors ?: emptyList(),
                    modifier = Modifier.weight(1f)
                )
                if (tuner != null) {
                    RdsAudioModeIndicator(
                        isStereo = tuner.stereo,
                        isForcedMono = tuner.stereoForced
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        RdsLabelText(text = stringResource(id = R.string.rds_pi_label, ""))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = displayPiValue)
    }
}

@Composable
private fun RdsAudioModeIndicator(
    isStereo: Boolean,
    isForcedMono: Boolean,
    modifier: Modifier = Modifier
) {
    val effectiveStereo = if (isForcedMono) false else isStereo
    val label = stringResource(
        id = if (effectiveStereo) {
            R.string.rds_audio_mode_chip_label_stereo
        } else {
            R.string.rds_audio_mode_chip_label_mono
        }
    )
    val modeDescription = stringResource(
        id = if (effectiveStereo) {
            R.string.rds_audio_mode_stereo
        } else {
            R.string.rds_audio_mode_mono
        }
    )
    val contentDescription = stringResource(
        id = R.string.rds_audio_mode_content_description,
        modeDescription
    )
    Surface(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 1.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private const val RDS_RADIOTEXT_LENGTH = 64

private const val RDS_RADIOTEXT_LOOP_SPACER = 6

private fun buildRadiotextDisplay(line: String?, errors: List<Int>): Pair<String, List<Int>> {
    val captured = line.orEmpty().take(RDS_RADIOTEXT_LENGTH)
    val trimmed = captured.trimEnd()
    val spacer = " ".repeat(RDS_RADIOTEXT_LOOP_SPACER)
    val display = if (trimmed.isEmpty()) spacer else trimmed + spacer
    val effectiveErrors = errors.take(trimmed.length)
    val paddedErrors = if (display.length > effectiveErrors.size) {
        effectiveErrors + List(display.length - effectiveErrors.size) { 0 }
    } else {
        effectiveErrors
    }
    return display to paddedErrors
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RdsRadiotextContent(tuner: TunerState?) {
    val baseStyle = MaterialTheme.typography.bodyMedium
    val radiotextStyle = baseStyle.copy(
        fontSize = baseStyle.fontSize * 0.8f,
        fontFamily = FontFamily.Monospace
    )
    val lineModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
    val marqueeModifier = Modifier
        .fillMaxWidth()
        .basicMarquee(
            iterations = Int.MAX_VALUE,
            animationMode = MarqueeAnimationMode.Immediately,
            repeatDelayMillis = 0,
            initialDelayMillis = 0,
            spacing = MarqueeSpacing.fractionOfContainer(0f)
        )
    val (rt0Text, rt0Errors) = buildRadiotextDisplay(tuner?.rt0, tuner?.rt0Errors ?: emptyList())
    val (rt1Text, rt1Errors) = buildRadiotextDisplay(tuner?.rt1, tuner?.rt1Errors ?: emptyList())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = lineModifier,
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                AnnotatedErrorText(
                    text = rt0Text,
                    errors = rt0Errors,
                    minLines = 1,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    style = radiotextStyle,
                    modifier = marqueeModifier
                )
            }
        }
        Surface(
            modifier = lineModifier,
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                AnnotatedErrorText(
                    text = rt1Text,
                    errors = rt1Errors,
                    minLines = 1,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    style = radiotextStyle,
                    modifier = marqueeModifier
                )
            }
        }
    }
}

@Composable
private fun InformationSection(
    state: UiState,
    currentPty: (TunerState?) -> String
) {
    val tuner = state.tunerState
    // The station logo, ECC/country, AF list and transmitter database lookup are all provided by
    // fm-dx-webserver; a direct USB / xdrd tuner only yields the natively decoded RDS fields.
    val webExtras = state.connectionType == ConnectionType.SERVER
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (webExtras && !state.stationLogoUrl.isNullOrBlank()) {
            StationLogo(state.stationLogoUrl)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RdsPsPiContent(tuner)
                RdsPtyEccContent(tuner, currentPty, showEcc = webExtras)
                RdsFlagsRow(tuner)
                val country = tuner?.countryName ?: tuner?.countryIso
                if (!country.isNullOrBlank()) {
                    RdsLabelValueRow(
                        label = stringResource(
                            id = R.string.country_label,
                            ""
                        )
                    ) { valueModifier ->
                        Text(
                            text = country,
                            modifier = valueModifier
                        )
                    }
                }
                tuner?.diDisplay()?.let { di ->
                    RdsLabelValueRow(
                        label = stringResource(
                            id = R.string.rds_di_label,
                            ""
                        )
                    ) { valueModifier ->
                        Text(
                            text = di,
                            modifier = valueModifier
                        )
                    }
                }
                if (webExtras) {
                    val afText = tuner?.afList?.size?.let { count ->
                        pluralStringResource(id = R.plurals.af_frequencies, count = count, count)
                    } ?: stringResource(id = R.string.none)
                    RdsLabelValueRow(
                        label = stringResource(
                            id = R.string.rds_af_label,
                            ""
                        )
                    ) { valueModifier ->
                        Text(
                            text = afText,
                            modifier = valueModifier
                        )
                    }
                }
                RdsRadiotextContent(tuner)
            }
        }
        if (webExtras) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StationDetailsContent(state)
                }
            }
        }
    }
}

@Composable
private fun RdsPtyEccContent(
    tuner: TunerState?,
    currentPty: (TunerState?) -> String,
    showEcc: Boolean = true
) {
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

        // Right: ECC label (green) + value — ECC isn't decoded on the raw protocol, so it is
        // only shown for server connections that supply it.
        if (showEcc) {
            Spacer(Modifier.weight(1f))
            RdsLabelText(text = stringResource(id = R.string.rds_ecc_label, ""))
            Spacer(Modifier.width(8.dp))
            Text(
                text = ecc,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun StationLogo(logoUrl: String?) {
    if (logoUrl.isNullOrBlank()) return
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp, max = 160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .crossfade(true)
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = stringResource(id = R.string.station_logo_content_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 120.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun RdsFlagsRow(tuner: TunerState?) {
    if (tuner == null) return
    val flags = buildList {
        add(
            RdsFlagUi(
                shortLabelRes = R.string.rds_flag_tp_short,
                fullLabelRes = R.string.rds_flag_tp_full,
                enabled = tuner.tp
            )
        )
        add(
            RdsFlagUi(
                shortLabelRes = R.string.rds_flag_ta_short,
                fullLabelRes = R.string.rds_flag_ta_full,
                enabled = tuner.ta
            )
        )
        add(
            RdsFlagUi(
                shortLabelRes = R.string.rds_flag_ms_music,
                fullLabelRes = R.string.rds_flag_ms_music_full,
                enabled = tuner.ms
            )
        )
        add(
            RdsFlagUi(
                shortLabelRes = R.string.rds_flag_ms_speech,
                fullLabelRes = R.string.rds_flag_ms_speech_full,
                enabled = !tuner.ms
            )
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        flags.forEach { flag ->
            val (containerColor, contentColor) = if (flag.enabled) {
                MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            }
            val statusText = stringResource(
                id = if (flag.enabled) R.string.status_state_on else R.string.status_state_off
            )
            val description = stringResource(id = flag.fullLabelRes)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                contentColor = contentColor,
                tonalElevation = if (flag.enabled) 2.dp else 0.dp,
                modifier = Modifier.semantics {
                    contentDescription = "$description: $statusText"
                }
            ) {
                Text(
                    text = stringResource(id = flag.shortLabelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AnnotatedErrorText(
    text: String,
    errors: List<Int>,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
        text = annotated,
        minLines = minLines,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow,
        style = style
    )
}

@Composable
private fun StationDetailsContent(state: UiState) {
    val tx = state.tunerState?.txInfo
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StationDetailRow(
            labelRes = R.string.station_name_label,
            value = tx?.name ?: stringResource(id = R.string.default_value)
        )
        StationDetailRow(
            labelRes = R.string.station_location_label,
            value = buildList {
                tx?.city?.takeUnless { it.isBlank() }?.let(::add)
                tx?.countryCode?.takeUnless { it.isBlank() }?.let(::add)
            }
                .takeUnless { it.isEmpty() }
                ?.joinToString(", ")
                ?: stringResource(id = R.string.default_value)
        )
        StationMetricsRow(
            metrics = listOf(
                StationMetricData(
                    labelRes = R.string.station_distance_label,
                    value = tx?.distanceKm?.let { stringResource(id = R.string.km_unit, it) }
                        ?: stringResource(id = R.string.default_value)
                ),
                StationMetricData(
                    labelRes = R.string.station_power_label,
                    value = tx?.erpKw?.let { stringResource(id = R.string.kw_unit, it) }
                        ?: stringResource(id = R.string.default_value)
                )
            )
        )
        StationMetricsRow(
            metrics = listOf(
                StationMetricData(
                    labelRes = R.string.station_polarization_label,
                    value = tx?.polarization ?: stringResource(id = R.string.default_value)
                ),
                StationMetricData(
                    labelRes = R.string.station_azimuth_label,
                    value = tx?.azimuthDeg?.let { stringResource(id = R.string.deg_unit, it) }
                        ?: stringResource(id = R.string.default_value)
                )
            )
        )
    }
}

@Composable
private fun StationDetailRow(
    modifier: Modifier = Modifier,
    @StringRes labelRes: Int,
    value: String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
private fun StationMetricsRow(
    metrics: List<StationMetricData>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        metrics.forEach { metric ->
            StationMetric(labelRes = metric.labelRes, value = metric.value)
        }
    }
}

private data class StationMetricData(
    @param:StringRes val labelRes: Int,
    val value: String
)

@Composable
private fun StationMetric(
    @StringRes labelRes: Int,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        RdsLabelText(
            text = stringResource(id = labelRes, "")
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun SpectrumSection(
    state: UiState,
    onScan: () -> Unit,
    onTuneDirect: (Double) -> Unit,
    onDragStateChange: (Boolean) -> Unit
) {
    val spectrum = state.spectrum
    if (spectrum.isEmpty()) {
        // Plugin gating in MainViewModel hides this tab when data is unavailable, but if it is
        // momentarily empty (e.g. between scans) keep the layout calm rather than blank.
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.spectrum_plugin_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        onDragStateChange(false)
        return
    }

    val sortedSpectrum = remember(spectrum) { spectrum.sortedBy { it.frequencyMHz } }
    val tunedFreq = state.tunerState?.freqMHz
        ?: state.pendingFrequencyMHz
        ?: sortedSpectrum.first().frequencyMHz

    val availableBands = remember(sortedSpectrum) {
        if (sortedSpectrum.isEmpty()) emptyList() else {
            val dataMin = sortedSpectrum.first().frequencyMHz
            val dataMax = sortedSpectrum.last().frequencyMHz
            SpectrumBand.entries.filter { band ->
                // Require at least 2 MHz of real overlap. Smaller slivers (e.g. a server with
                // 80–88 MHz dumps a 0.5 MHz "tail" into CCIR which would otherwise render as
                // 11 points crammed into the leftmost 2 % of the 20.5 MHz canvas).
                val overlap = minOf(dataMax, band.max) - maxOf(dataMin, band.min)
                overlap >= 2.0
            }
        }
    }
    if (availableBands.isEmpty()) {
        onDragStateChange(false)
        return
    }

    val initialBand = remember(availableBands, state.tunerState?.freqMHz) {
        availableBands.firstOrNull { tunedFreq in it.min..it.max }
            ?: availableBands.first()
    }
    var selectedBand by rememberSaveable(availableBands) { mutableStateOf(initialBand) }

    LaunchedEffect(tunedFreq, availableBands) {
        availableBands.firstOrNull { tunedFreq in it.min..it.max }?.let { band ->
            if (band != selectedBand) selectedBand = band
        }
    }

    val visibleMin = selectedBand.min
    val visibleMax = selectedBand.max
    val visiblePoints = remember(sortedSpectrum, visibleMin, visibleMax) {
        sortedSpectrum.filter { it.frequencyMHz in visibleMin..visibleMax }
    }

    onDragStateChange(false)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Prominent now-tuned frequency display.
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f MHz", tunedFreq),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "%s · %.1f – %.1f MHz",
                        stringResource(id = selectedBand.labelRes),
                        visibleMin,
                        visibleMax
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (availableBands.size > 1) {
            SpectrumBandPresets(
                bands = availableBands,
                selected = selectedBand,
                onSelect = { selectedBand = it }
            )
        }

        // Tap a peak on the graph to tune to it.
        SpectrumGraph(
            points = visiblePoints,
            visibleMin = visibleMin,
            visibleMax = visibleMax,
            highlightFreq = tunedFreq,
            onTapFrequency = { freq ->
                val nearest = sortedSpectrum.minByOrNull { abs(it.frequencyMHz - freq) }?.frequencyMHz
                    ?: freq
                onTuneDirect(nearest)
            }
        )

        FilledTonalButton(
            onClick = onScan,
            enabled = !state.isScanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = stringResource(id = R.string.spectrum_scanning))
            } else {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.start_scan))
            }
        }
    }
}

@Composable
private fun SpectrumGraph(
    points: List<SpectrumPoint>,
    visibleMin: Double,
    visibleMax: Double,
    highlightFreq: Double,
    onTapFrequency: (Double) -> Unit
) {
    val validPoints = points
        .filter { it.frequencyMHz.isFinite() && it.signalDbf.isFinite() }
        .sortedBy { it.frequencyMHz }
    val freqSpan = (visibleMax - visibleMin).coerceAtLeast(0.0001)
    val maxSig = validPoints.maxOfOrNull { it.signalDbf } ?: 0.0
    val minSig = validPoints.minOfOrNull { it.signalDbf } ?: maxSig
    val signalSpan = (maxSig - minSig).takeIf { abs(it) >= 1e-6 } ?: 1.0

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val strokeWidthPx = with(LocalDensity.current) { 2.5.dp.toPx() }
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val textMeasurer = rememberTextMeasurer()

    val gridFreqs = remember(visibleMin, visibleMax) {
        val step = when {
            freqSpan > 15.0 -> 5.0
            freqSpan > 6.0 -> 2.0
            freqSpan > 2.0 -> 1.0
            else -> 0.5
        }
        val first = ceil(visibleMin / step) * step
        generateSequence(first) { it + step }
            .takeWhile { it <= visibleMax + 1e-6 }
            .toList()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .pointerInput(visibleMin, visibleMax) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (size.width > 0) {
                                val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                                onTapFrequency(visibleMin + ratio * freqSpan)
                            }
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val labelHeight = 18f * density
            val plotHeight = (height - labelHeight).coerceAtLeast(20f)

            // Gridlines + labels at major MHz boundaries inside the visible window.
            gridFreqs.forEach { freq ->
                val ratio = ((freq - visibleMin) / freqSpan).toFloat().coerceIn(0f, 1f)
                val x = ratio * width
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, plotHeight),
                    strokeWidth = density
                )
                val label = String.format(Locale.US, "%.0f", freq)
                val measured = textMeasurer.measure(AnnotatedString(label), labelStyle)
                val tx = (x - measured.size.width / 2f).coerceIn(2f, width - measured.size.width - 2f)
                drawText(measured, topLeft = androidx.compose.ui.geometry.Offset(tx, plotHeight + 2f))
            }

            // Spectrum trace.
            if (validPoints.isNotEmpty()) {
                val path = Path()
                var firstX = 0f
                var lastX = 0f
                validPoints.forEachIndexed { index, point ->
                    val ratio = ((point.frequencyMHz - visibleMin) / freqSpan)
                        .toFloat()
                        .coerceIn(0f, 1f)
                    val x = ratio * width
                    val normalized = ((point.signalDbf - minSig) / signalSpan)
                        .toFloat()
                        .coerceIn(0f, 1f)
                    val y = plotHeight - (normalized * plotHeight)
                    if (index == 0) {
                        firstX = x
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    lastX = x
                }
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(lastX, plotHeight)
                    lineTo(firstX, plotHeight)
                    close()
                }
                drawPath(fillPath, color = secondaryColor.copy(alpha = 0.20f))
                drawPath(path, color = secondaryColor, style = Stroke(width = strokeWidthPx))
            }

            // Tuned-frequency marker.
            if (highlightFreq in visibleMin..visibleMax) {
                val highlightRatio =
                    ((highlightFreq - visibleMin) / freqSpan).toFloat().coerceIn(0f, 1f)
                val x = highlightRatio * width
                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, plotHeight),
                    strokeWidth = strokeWidthPx
                )
            }
        }
    }
}

private enum class SpectrumBand(val min: Double, val max: Double, @androidx.annotation.StringRes val labelRes: Int) {
    OIRT(65.8, 74.0, R.string.spectrum_band_oirt),
    LOW(83.0, 87.5, R.string.spectrum_band_low),
    CCIR(87.5, 108.0, R.string.spectrum_band_ccir)
}

@Composable
private fun SpectrumBandPresets(
    bands: List<SpectrumBand>,
    selected: SpectrumBand,
    onSelect: (SpectrumBand) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        bands.forEach { band ->
            FilterChip(
                selected = selected == band,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(band)
                },
                label = { Text(stringResource(id = band.labelRes)) }
            )
        }
    }
}

@Preview(name = "Main Screen", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MainScreenPreview() {
    FmDxTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val state = previewUiState()
        Surface {
            MainScreen(
                state = state,
                snackbarHostState = snackbarHostState,
                onUpdateUrl = {},
                onConnect = {},
                onDisconnect = {},
                onToggleAudio = {},
                onTuneDirect = {},
                onToggleEq = {},
                onToggleIms = {},
                onToggleStereoMode = {},
                onCycleAntenna = {},
                onScan = {},
                formatSignal = { tuner, _ ->
                    tuner?.signalDbf?.let { String.format(Locale.US, "%.1f dBf", it) } ?: "--"
                },
                currentPty = { "10/Pop Music" },
                antennaLabel = { state.previewAntennaLabel() },
                onShowSettings = {},
                onShowAbout = {},
                onShowPublicServerPicker = {},
                onHidePublicServerPicker = {},
                onRefreshPublicServers = {},
                onUpdatePublicServerQuery = {},
                onSelectPublicServer = {}
            )
        }
    }
}

@Preview(name = "Help Section", showBackground = true, widthDp = 360)
@Composable
private fun HelpSectionPreview() {
    FmDxTheme {
        Surface {
            HelpSection()
        }
    }
}

@Preview(name = "Spectrum Section", showBackground = true, widthDp = 360)
@Composable
private fun SpectrumSectionPreview() {
    FmDxTheme {
        Surface {
            SpectrumSection(
                state = previewUiState(),
                onScan = {},
                onTuneDirect = {},
                onDragStateChange = {}
            )
        }
    }
}

@Preview(name = "Information Section", showBackground = true, widthDp = 360)
@Composable
private fun InformationSectionPreview() {
    FmDxTheme {
        Surface {
            InformationSection(
                state = previewUiState(),
                currentPty = { "10/Pop Music" }
            )
        }
    }
}

@Preview(name = "Station Logo", showBackground = true, widthDp = 240)
@Composable
private fun StationLogoPreview() {
    FmDxTheme {
        Surface {
            StationLogo(logoUrl = "https://tef.noobish.eu/logos/HOL/800A.png")
        }
    }
}

private fun previewSpectrum(): List<SpectrumPoint> {
    val start = 87.5
    val end = 108.0
    val step = 0.2
    val points = mutableListOf<SpectrumPoint>()
    var freq = start
    var index = 0
    while (freq <= end + 1e-6) {
        val baseline = -70 + (index % 6) * 4
        val boost = when {
            freq in 99.0..100.0 -> 12.0
            freq in 104.0..104.4 -> 7.0
            else -> 0.0
        }
        val roundedFreq = (freq * 100).roundToInt() / 100.0
        points += SpectrumPoint(
            frequencyMHz = roundedFreq,
            signalDbf = baseline.toDouble() + boost - (index / 30.0)
        )
        freq += step
        index++
    }
    return points
}

private fun previewTunerState(): TunerState {
    return TunerState(
        freqMHz = 99.7,
        minFreqMHz = 87.5,
        maxFreqMHz = 108.0,
        stepKHz = 100,
        signalDbf = -48.0,
        stereo = true,
        stereoForced = false,
        ims = false,
        eq = false,
        antennaIndex = 1,
        users = 4,
        ps = "FMDX",
        psErrors = List(8) { 0 },
        pi = "800A",
        ecc = "E1",
        countryName = "Netherlands",
        countryIso = "HOL",
        tp = true,
        ta = false,
        ms = true,
        pty = 10,
        ptyText = "Pop Music",
        dynamicPty = false,
        artificialHead = false,
        compressed = false,
        rt0 = "Preview radio text line one    ",
        rt0Errors = emptyList(),
        rt1 = "Preview radio text line two",
        rt1Errors = emptyList(),
        afList = listOf(90.1, 94.5, 102.3),
        txInfo = TxInfo(
            name = "FM-DX Radio",
            city = "The Hague",
            countryCode = "HOL",
            distanceKm = "12",
            erpKw = "5",
            polarization = "Horizontal",
            azimuthDeg = "180"
        )
    )
}

private fun previewTunerInfo(): TunerInfo {
    return TunerInfo(
        tunerName = "SDRplay RSPdx",
        tunerDescription = "Remote FM-DX receiver",
        antennaNames = listOf("Omni", "Yagi", "Loop"),
        activeAntenna = 1
    )
}

private fun previewUiState(): UiState {
    val tunerState = previewTunerState()
    val tunerInfo = previewTunerInfo()
    return UiState(
        serverUrl = "http://192.168.1.100:8901",
        recentServerUrls = listOf(
            "http://192.168.1.100:8901",
            "http://dx.example.net:8000"
        ),
        tunerInfo = tunerInfo,
        tunerState = tunerState,
        audioPlaying = true,
        isConnected = true,
        antennas = tunerInfo.antennaNames,
        spectrum = previewSpectrum(),
        isScanning = false,
        statusMessage = "Connected to ${tunerInfo.tunerName}",
        pendingFrequencyMHz = tunerState.freqMHz,
        stationLogoUrl = "https://tef.noobish.eu/logos/HOL/800A.png",
        serverLatencyMs = 42.0
    )
}

@Preview(name = "Connection Section", showBackground = true, widthDp = 360)
@Composable
private fun ConnectionSectionPreview() {
    FmDxTheme {
        Surface {
            ConnectionSection(
                state = previewUiState().copy(isConnected = false, isConnecting = false),
                onUpdateUrl = {},
                onConnect = {},
                onDisconnect = {},
                onShowPublicServerPicker = {}
            )
        }
    }
}

@Preview(name = "Server Info Section", showBackground = true, widthDp = 360)
@Composable
private fun ServerInfoSectionPreview() {
    FmDxTheme {
        Surface {
            ServerInfoSection(state = previewUiState())
        }
    }
}

@Preview(name = "Tuner Section", showBackground = true, widthDp = 360)
@Composable
private fun TunerSectionPreview() {
    FmDxTheme {
        Surface {
            val state = previewUiState()
            TunerSection(
                state = state,
                onTuneDirect = {},
                formatSignal = { tuner, _ ->
                    tuner?.signalDbf?.let { String.format(Locale.US, "%.1f dBf", it) } ?: "--"
                },
                currentPty = { "10/Pop Music" },
                onToggleEq = {},
                onToggleIms = {},
                onToggleStereoMode = {},
                onCycleAntenna = {},
                antennaLabel = { state.previewAntennaLabel() }
            )
        }
    }
}

@Preview(name = "Settings Screen", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun SettingsScreenPreview() {
    FmDxTheme {
        Surface {
            SettingsScreen(
                state = previewUiState(),
                onUpdateSettings = { _, _, _, _, _ -> },
                onBack = {}
            )
        }
    }
}

@Preview(name = "About Screen", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun AboutScreenPreview() {
    val dummyHandler = object : UriHandler {
        override fun openUri(uri: String) = Unit
    }
    FmDxTheme {
        CompositionLocalProvider(LocalUriHandler provides dummyHandler) {
            Surface {
                AboutScreen(onBack = {})
            }
        }
    }
}

@Preview(name = "Connection Indicator", showBackground = true, widthDp = 240)
@Composable
private fun ConnectionStatusIndicatorPreview() {
    FmDxTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConnectionStatusIndicator(isConnected = true, isConnecting = false)
                ConnectionStatusIndicator(isConnected = false, isConnecting = true)
                ConnectionStatusIndicator(isConnected = false, isConnecting = false)
            }
        }
    }
}

private fun UiState.previewAntennaLabel(): String {
    val info = tunerInfo ?: return "Antenna"
    val index = info.activeAntenna.coerceIn(0, info.antennaNames.lastIndex)
    return info.antennaNames.getOrNull(index) ?: "Antenna"
}
