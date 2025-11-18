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
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
import kotlinx.coroutines.launch
import org.fmdx.app.model.SignalUnit
import org.fmdx.app.model.SpectrumPoint
import org.fmdx.app.model.TunerInfo
import org.fmdx.app.model.TunerState
import org.fmdx.app.model.TxInfo
import org.fmdx.app.ui.theme.FmDxTheme
import java.util.Locale
import kotlin.math.abs
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
    onUpdateSettings: (signalUnit: SignalUnit, networkBuffer: Int, playerBuffer: Int, restartAudioOnTune: Boolean, passThroughEnabled: Boolean) -> Unit
) {
    val showSettingsState = rememberSaveable { mutableStateOf(false) }
    val showAboutState = rememberSaveable { mutableStateOf(false) }
    val showSettings by showSettingsState
    val showAbout by showAboutState

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
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onToggleAudio = onToggleAudio,
                onTuneDirect = onTuneDirect,
                onToggleEq = onToggleEq,
                onToggleIms = onToggleIms,
                onToggleStereoMode = onToggleStereoMode,
                onCycleAntenna = onCycleAntenna,
                onScan = onScan,
                formatSignal = formatSignal,
                currentPty = currentPty,
                antennaLabel = antennaLabel,
                onShowSettings = { showSettingsState.value = true },
                onShowAbout = { showAboutState.value = true }
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
    onToggleStereoMode: () -> Unit,
    onCycleAntenna: () -> Unit,
    onScan: () -> Unit,
    formatSignal: (TunerState?, SignalUnit) -> String,
    currentPty: (TunerState?) -> String,
    antennaLabel: () -> String,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit
) {
    var isSpectrumDragging by remember { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    val tabs = buildList {
        add(SectionTab(titleRes = R.string.server, requiresConnection = false) {
            ServerSection(
                state,
                onUpdateUrl,
                onConnect,
                onDisconnect
            )
        })
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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val spectrumPageIndex = tabs.indexOfFirst { it.titleRes == R.string.spectrum }
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(state.isConnected, tabs.size) {
        if (!state.isConnected) {
            pagerState.scrollToPage(0)
        } else {
            if (pagerState.currentPage >= tabs.size) {
                pagerState.scrollToPage(tabs.lastIndex)
                return@LaunchedEffect
            }
            if (pagerState.currentPage == 0) {
                val tunerPageIndex = tabs.indexOfFirst { it.titleRes == R.string.tuner }
                if (tunerPageIndex != -1) {
                    pagerState.animateScrollToPage(tunerPageIndex)
                }
            }
        }
    }

    val selectedTabIndex = clampTabIndex(pagerState.currentPage, tabs.size)

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
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.about_message),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = versionLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
                    ListItem(
                        headlineContent = { Text(text = fmdxWebServerLabel) },
                        supportingContent = { Text(text = fmdxWebServerUrl) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri(fmdxWebServerUrl) }
                    )
                    ListItem(
                        headlineContent = { Text(text = tefLoggerLabel) },
                        supportingContent = { Text(text = tefLoggerUrl) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri(tefLoggerUrl) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(text = fmdxOrgSiteLabel) },
                        supportingContent = { Text(text = fmdxOrgSiteUrl) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri(fmdxOrgSiteUrl) }
                    )
                }
            }
        }
    }
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
private fun ServerSection(
    state: UiState,
    onUpdateUrl: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val connectAndDismissKeyboard = {
        focusManager.clearFocus(force = true)
        onConnect()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                onClick = {
                                    recentExpanded = false
                                    onUpdateUrl(server)
                                    focusManager.clearFocus(force = true)
                                }
                            )
                        }
                    }
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
        snapshotFlow { selectedMHz to selectedDecimalIndex }
            .debounce(400)
            .collectLatest { (mhz, decimalIndex) ->
                val requestedKHz = mhz * 1000 + decimalIndex * stepKHz
                val clampedKHz = requestedKHz.coerceIn(minKHz, maxKHz)
                onTuneDirect(clampedKHz / 1000.0)
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val tunerState = state.tunerState
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    .padding(16.dp),
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
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FrequencyControlsCard(
                    state = state,
                    onTuneDirect = onTuneDirect
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_pass_through_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.settings_pass_through_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { passThroughEnabled = !passThroughEnabled }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = passThroughEnabled,
                        onCheckedChange = { passThroughEnabled = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (passThroughEnabled) {
                            stringResource(id = R.string.settings_pass_through_enabled)
                        } else {
                            stringResource(id = R.string.settings_pass_through_disabled)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
    onToggleStereoMode: () -> Unit,
    onCycleAntenna: () -> Unit
) {
    val canSwitchAntenna = state.tunerInfo?.canSwitchAntenna() == true
    val imsActive = state.tunerState?.ims == true
    val eqActive = state.tunerState?.eq == true
    val isStereoForced = state.tunerState?.stereoForced == true
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!state.stationLogoUrl.isNullOrBlank()) {
            StationLogo(state.stationLogoUrl)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RdsPsPiContent(tuner)
                RdsPtyEccContent(tuner, currentPty)
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
                RdsRadiotextContent(tuner)
            }
        }
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isScanning) {
                val scanningLabel = stringResource(id = R.string.spectrum_scanning)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .semantics {
                                contentDescription = scanningLabel
                            }
                    )
                    Text(
                        text = scanningLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val spectrum = state.spectrum
            if (spectrum.isEmpty()) {
                onDragStateChange(false)
                Text(
                    text = stringResource(id = R.string.spectrum_plugin_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val sortedSpectrum = remember(spectrum) { spectrum.sortedBy { it.frequencyMHz } }
                val frequencies =
                    remember(sortedSpectrum) { sortedSpectrum.map { it.frequencyMHz } }
                val minSpectrumFreq = frequencies.first()
                val maxSpectrumFreq = frequencies.last()
                val freqSpan = maxSpectrumFreq - minSpectrumFreq
                val initialFreq =
                    (state.tunerState?.freqMHz ?: state.pendingFrequencyMHz ?: minSpectrumFreq)
                        .coerceIn(minSpectrumFreq, maxSpectrumFreq)

                var sliderValue by remember(
                    minSpectrumFreq,
                    maxSpectrumFreq
                ) { mutableDoubleStateOf(initialFreq) }

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
                        text = stringResource(
                            id = R.string.spectrum_selected_frequency_value,
                            displayFreq
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                val minZoomFraction = 0.15f
                var zoomProgress by rememberSaveable(
                    minSpectrumFreq,
                    maxSpectrumFreq
                ) { mutableFloatStateOf(1f) }
                val spanFraction =
                    if (freqSpan <= 1e-6) 1f else minZoomFraction + (1f - minZoomFraction) * zoomProgress
                val smallestGap = remember(frequencies) {
                    frequencies.zipWithNext { a, b -> abs(b - a) }
                        .minOrNull()
                        ?.takeIf { it > 1e-6 }
                }
                val baseSpan = if (freqSpan <= 1e-6) 0.0 else freqSpan * spanFraction
                val visibleSpan = when {
                    freqSpan <= 1e-6 -> 0.0
                    smallestGap == null -> baseSpan
                    else -> max(baseSpan, smallestGap)
                }
                val halfSpan = visibleSpan / 2.0
                var visibleMin = sliderValue - halfSpan
                var visibleMax = sliderValue + halfSpan
                if (visibleMin < minSpectrumFreq) {
                    val overflow = minSpectrumFreq - visibleMin
                    visibleMin += overflow
                    visibleMax += overflow
                }
                if (visibleMax > maxSpectrumFreq) {
                    val overflow = visibleMax - maxSpectrumFreq
                    visibleMin -= overflow
                    visibleMax -= overflow
                }
                visibleMin = visibleMin.coerceIn(minSpectrumFreq, maxSpectrumFreq)
                visibleMax = visibleMax.coerceIn(minSpectrumFreq, maxSpectrumFreq)
                if (visibleMax - visibleMin <= 1e-6) {
                    visibleMin = minSpectrumFreq
                    visibleMax = maxSpectrumFreq
                }
                val startIndex = frequencies.indexOfFirst { it >= visibleMin }.let { index ->
                    if (index == -1) 0 else index
                }
                val endIndex = frequencies.indexOfLast { it <= visibleMax }.let { index ->
                    if (index == -1) frequencies.lastIndex else index
                }
                val paddedStart = (startIndex - 1).coerceAtLeast(0)
                val paddedEnd = (endIndex + 1).coerceAtMost(sortedSpectrum.lastIndex)
                val pointsForGraph = if (paddedStart <= paddedEnd) {
                    sortedSpectrum.subList(paddedStart, paddedEnd + 1)
                } else {
                    sortedSpectrum
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.spectrum_zoom),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val zoomMultiplier = if (spanFraction > 0f) 1f / spanFraction else 1f
                    Text(
                        text = String.format(Locale.ROOT, "%.1fx", zoomMultiplier),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = zoomProgress,
                    onValueChange = { zoomProgress = it.coerceIn(0f, 1f) },
                    valueRange = 0f..1f,
                    steps = 9,
                    enabled = freqSpan > 1e-6
                )

                SpectrumGraph(points = pointsForGraph, highlightFreq = sliderValue)

                val sliderInteraction = remember { MutableInteractionSource() }
                val sliderDragging by sliderInteraction.collectIsDraggedAsState()
                LaunchedEffect(sliderDragging) {
                    onDragStateChange(sliderDragging)
                }

                val sliderSteps = (frequencies.size - 2).coerceAtLeast(0)
                Slider(
                    value = sliderValue.toFloat(),
                    onValueChange = { raw ->
                        val target = raw.toDouble().coerceIn(minSpectrumFreq, maxSpectrumFreq)
                        val nearest = frequencies.minByOrNull { abs(it - target) } ?: target
                        sliderValue = nearest
                    },
                    onValueChangeFinished = {
                        val currentFreq = state.tunerState?.freqMHz
                        if (currentFreq == null || abs(currentFreq - sliderValue) >= 0.0001) {
                            onTuneDirect(sliderValue)
                        }
                        onDragStateChange(false)
                    },
                    valueRange = minSpectrumFreq.toFloat()..maxSpectrumFreq.toFloat(),
                    steps = sliderSteps.coerceAtLeast(0),
                    interactionSource = sliderInteraction
                )
            }

            Button(
                onClick = onScan,
                enabled = !state.isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.start_scan))
            }
        }
    }
}

@Composable
private fun SpectrumGraph(
    points: List<SpectrumPoint>,
    highlightFreq: Double
) {
    val validPoints = points
        .filter { it.frequencyMHz.isFinite() && it.signalDbf.isFinite() }
        .sortedBy { it.frequencyMHz }
    if (validPoints.isEmpty()) {
        Text(text = stringResource(id = R.string.spectrum_unavailable))
        return
    }
    val minFreq = validPoints.first().frequencyMHz
    val maxFreq = validPoints.last().frequencyMHz
    val freqSpan = (maxFreq - minFreq).coerceAtLeast(0.0001)
    val maxSig = validPoints.maxOfOrNull { it.signalDbf } ?: 0.0
    val minSig = validPoints.minOfOrNull { it.signalDbf } ?: maxSig
    val signalSpan = (maxSig - minSig).takeIf { abs(it) >= 1e-6 } ?: 1.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val strokeWidthPx = with(LocalDensity.current) { 2.dp.toPx() }

    Card {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val width = size.width
            val height = size.height

            drawRect(color = backgroundColor)
            val path = Path()
            var firstX = 0f
            var lastX = 0f
            validPoints.forEachIndexed { index, point ->
                val ratio = ((point.frequencyMHz - minFreq) / freqSpan).toFloat().coerceIn(0f, 1f)
                val x = ratio * width
                val normalized = ((point.signalDbf - minSig) / signalSpan)
                    .toFloat()
                    .coerceIn(0f, 1f)
                val y = height - (normalized * height)
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
                lineTo(lastX, height)
                lineTo(firstX, height)
                close()
            }
            drawPath(fillPath, color = secondaryColor.copy(alpha = 0.18f))
            drawPath(path, color = secondaryColor, style = Stroke(width = strokeWidthPx))
            if (highlightFreq in minFreq..maxFreq) {
                val highlightRatio =
                    ((highlightFreq - minFreq) / freqSpan).toFloat().coerceIn(0f, 1f)
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
                onShowAbout = {}
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
        stationLogoUrl = "https://tef.noobish.eu/logos/HOL/800A.png"
    )
}

@Preview(name = "Server Section", showBackground = true, widthDp = 360)
@Composable
private fun ServerSectionPreview() {
    FmDxTheme {
        Surface {
            ServerSection(
                state = previewUiState().copy(isConnected = false, isConnecting = false),
                onUpdateUrl = {},
                onConnect = {},
                onDisconnect = {}
            )
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
