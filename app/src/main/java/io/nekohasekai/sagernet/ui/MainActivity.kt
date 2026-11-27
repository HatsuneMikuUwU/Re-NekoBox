package io.nekohasekai.sagernet.ui

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.aidl.ISagerNetService
import io.nekohasekai.sagernet.aidl.SpeedDisplayData
import io.nekohasekai.sagernet.aidl.TrafficDataBatch
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.bg.SagerConnection
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SubscriptionBean
import io.nekohasekai.sagernet.database.preference.OnPreferenceDataStoreChangeListener
import io.nekohasekai.sagernet.database.preference.PreferenceDataStore
import io.nekohasekai.sagernet.fmt.AbstractBean
import io.nekohasekai.sagernet.fmt.KryoConverters
import io.nekohasekai.sagernet.fmt.PluginEntry
import io.nekohasekai.sagernet.group.GroupInterfaceAdapter
import io.nekohasekai.sagernet.group.GroupUpdater
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.isPreview
import io.nekohasekai.sagernet.ktx.launchCustomTab
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.parseProxies
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import androidx.compose.ui.graphics.Color
import io.nekohasekai.sagernet.ui.components.FloatingTabBar
import io.nekohasekai.sagernet.ui.components.FloatingTabBarMargin
import io.nekohasekai.sagernet.ui.components.LocalBlurBackdrop
import io.nekohasekai.sagernet.ui.components.LocalBottomInset
import io.nekohasekai.sagernet.ui.components.barGlass
import io.nekohasekai.sagernet.ui.components.bottomScrim
import io.nekohasekai.sagernet.ui.components.floatingTabBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.screens.AboutScreen
import io.nekohasekai.sagernet.ui.screens.ConfigurationScreen
import io.nekohasekai.sagernet.ui.screens.GroupScreen
import io.nekohasekai.sagernet.ui.screens.LogcatScreen
import io.nekohasekai.sagernet.ui.screens.RouteScreen
import io.nekohasekai.sagernet.ui.screens.SettingsScreen
import io.nekohasekai.sagernet.ui.screens.ToolsScreen
import io.nekohasekai.sagernet.ui.screens.WebviewScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import moe.matsuri.nb4a.utils.Util

enum class MainDestination(
    val titleRes: Int,
    val icon: ImageVector,
    val inBottomBar: Boolean = false,
) {
    CONFIGURATION(R.string.menu_configuration, Icons.Rounded.Router, inBottomBar = true),
    GROUP(R.string.menu_group, Icons.Rounded.FolderCopy, inBottomBar = true),
    ROUTE(R.string.menu_route, Icons.Rounded.AccountTree, inBottomBar = true),
    SETTINGS(R.string.settings, Icons.Rounded.Settings, inBottomBar = true),
    TRAFFIC(R.string.menu_dashboard, Icons.Rounded.Dashboard),
    TOOLS(R.string.menu_tools, Icons.Rounded.Build),
    LOGCAT(R.string.menu_log, Icons.Rounded.Terminal),
    ABOUT(R.string.menu_about, Icons.Rounded.Info),
}

class MainActivity : ThemedActivity(),
    SagerConnection.Callback,
    OnPreferenceDataStoreChangeListener {

    val connection = SagerConnection(SagerConnection.CONNECTION_ID_MAIN_ACTIVITY_FOREGROUND, true)

    private val currentDestination = MutableStateFlow(MainDestination.CONFIGURATION)
    private val serviceStateFlow = MutableStateFlow(BaseService.State.Idle)
    private val txRateFlow = MutableStateFlow(0L)
    private val rxRateFlow = MutableStateFlow(0L)
    private val enableClashApiFlow = MutableStateFlow(DataStore.enableClashAPI)
    private val profileRefreshSignal = MutableStateFlow(0)

    private val pendingSubscriptionImport = MutableStateFlow<ProxyGroup?>(null)
    private val pendingProfileImport = MutableStateFlow<AbstractBean?>(null)
    private val missingPluginDialog = MutableStateFlow<Pair<String, PluginEntry>?>(null)
    private val showPreviewDialog = MutableStateFlow(false)

    private val connect = registerForActivityResult(VpnRequestActivity.StartService()) {
        if (it) showComposeSnackbar(getString(R.string.vpn_permission_denied))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MessageStore.setCurrentActivity(this)

        connection.connect(this, this)
        DataStore.configurationStore.registerChangeListener(this)
        GroupManager.userInterface = GroupInterfaceAdapter(this)

        if (intent?.action == Intent.ACTION_VIEW) {
            onNewIntent(intent)
        }

        if (Build.VERSION.SDK_INT >= 33) {
            val checkPermission = ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS)
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(POST_NOTIFICATIONS), 0)
            }
        }

        if (isPreview) {
            showPreviewDialog.value = true
        }

        setComposeContent {
            MainExpressiveShell()
        }
    }

    @Composable
    private fun MainExpressiveShell() {
        val destination by currentDestination.collectAsState()
        val serviceState by serviceStateFlow.collectAsState()
        val txRate by txRateFlow.collectAsState()
        val rxRate by rxRateFlow.collectAsState()
        val clashApiEnabled by enableClashApiFlow.collectAsState()
        val refreshTick by profileRefreshSignal.collectAsState()

        val subImport by pendingSubscriptionImport.collectAsState()
        val profImport by pendingProfileImport.collectAsState()
        val pluginMissing by missingPluginDialog.collectAsState()
        val previewHint by showPreviewDialog.collectAsState()

        val isSettingsSubScreen = destination == MainDestination.TOOLS ||
            destination == MainDestination.ABOUT ||
            destination == MainDestination.LOGCAT ||
            destination == MainDestination.TRAFFIC

        BackHandler(enabled = destination != MainDestination.CONFIGURATION) {
            currentDestination.value = if (isSettingsSubScreen) {
                MainDestination.SETTINGS
            } else {
                MainDestination.CONFIGURATION
            }
        }

        val bottomTabs = remember { MainDestination.entries.filter { it.inBottomBar } }
        val backdrop = rememberBlurBackdrop()
        val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomInset = if (!isSettingsSubScreen) {
            floatingTabBarSpace() + 92.dp
        } else {
            navBarInset + 16.dp
        }

        CompositionLocalProvider(
            LocalBottomInset provides bottomInset,
            LocalBlurBackdrop provides backdrop,
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blurBackdropSource(backdrop),
            ) {
                val backToSettings: () -> Unit = { currentDestination.value = MainDestination.SETTINGS }
                AnimatedContent(
                    targetState = destination,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "mainDestinationTransition",
                    modifier = Modifier.fillMaxSize(),
                ) { target ->
                    when (target) {
                        MainDestination.CONFIGURATION -> ConfigurationScreen(
                            refreshSignal = refreshTick,
                            onShowMessage = { showComposeSnackbar(it) },
                        )
                        MainDestination.GROUP -> GroupScreen(
                            onShowMessage = { showComposeSnackbar(it) },
                        )
                        MainDestination.ROUTE -> RouteScreen(
                            onShowMessage = { showComposeSnackbar(it) },
                        )
                        MainDestination.SETTINGS -> SettingsScreen(
                            clashApiEnabled = clashApiEnabled,
                            onOpenTools = { currentDestination.value = MainDestination.TOOLS },
                            onOpenLogcat = { currentDestination.value = MainDestination.LOGCAT },
                            onOpenDashboard = { currentDestination.value = MainDestination.TRAFFIC },
                            onOpenAbout = { currentDestination.value = MainDestination.ABOUT },
                            onClashApiChanged = { enableClashApiFlow.value = it },
                            onHideFromRecentAppsChanged = { applyHideFromRecentApps(it) },
                            onShowMessage = { msg, actionLabel, onAction ->
                                showComposeSnackbar(msg, actionLabel = actionLabel, onAction = onAction)
                            },
                        )
                        MainDestination.TRAFFIC -> WebviewScreen(onBack = backToSettings)
                        MainDestination.TOOLS -> ToolsScreen(
                            onBack = backToSettings,
                            onShowMessage = { showComposeSnackbar(it) },
                        )
                        MainDestination.LOGCAT -> LogcatScreen(
                            onBack = backToSettings,
                            onShowMessage = { showComposeSnackbar(it) },
                        )
                        MainDestination.ABOUT -> AboutScreen(
                            onBack = backToSettings,
                            onShowMessage = { showComposeSnackbar(it) },
                        )
                    }
                }
            }

            if (!isSettingsSubScreen) {
                val bg = MaterialTheme.colorScheme.background
                val fadeBrush = remember(bg) { bottomScrim(bg) }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(floatingTabBarSpace() + 104.dp)
                        .background(fadeBrush),
                )

                val density = LocalDensity.current
                var tabBarWidthPx by remember { mutableIntStateOf(0) }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = FloatingTabBarMargin),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ExpressiveConnectionDock(
                        state = serviceState,
                        txRate = txRate,
                        rxRate = rxRate,
                        onToggleConnection = {
                            if (DataStore.serviceState.canStop) {
                                SagerNet.stopService()
                            } else {
                                connect.launch(null)
                            }
                        },
                        onUrlTest = { urlTest() },
                        modifier = if (tabBarWidthPx > 0) {
                            Modifier.width(with(density) { tabBarWidthPx.toDp() })
                        } else {
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        },
                    )
                    FloatingTabBar(
                        items = bottomTabs,
                        selectedIndex = { bottomTabs.indexOf(destination).coerceAtLeast(0) },
                        onSelected = { idx -> currentDestination.value = bottomTabs[idx] },
                        modifier = Modifier.onSizeChanged { tabBarWidthPx = it.width },
                        iconContent = { dest, _ ->
                            Icon(dest.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                        },
                        labelContent = { dest, _ ->
                            Text(
                                text = stringResource(dest.titleRes),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }

            SnackbarHost(
                hostState = composeSnackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset + 4.dp),
            )
        }
        }

        subImport?.let { group ->
            val displayName = group.name.takeIf { !it.isNullOrBlank() } ?: "Subscription"
            AlertDialog(
                onDismissRequest = { pendingSubscriptionImport.value = null },
                shape = RoundedCornerShape(28.dp),
                title = { Text(stringResource(R.string.subscription_import)) },
                text = { Text(stringResource(R.string.subscription_import_message, displayName)) },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingSubscriptionImport.value = null
                            currentDestination.value = MainDestination.GROUP
                            runOnDefaultDispatcher { finishImportSubscription(group) }
                        },
                        shape = CircleShape,
                    ) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingSubscriptionImport.value = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }

        profImport?.let { profile ->
            AlertDialog(
                onDismissRequest = { pendingProfileImport.value = null },
                shape = RoundedCornerShape(28.dp),
                title = { Text(stringResource(R.string.profile_import)) },
                text = { Text(stringResource(R.string.profile_import_message, profile.displayName())) },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingProfileImport.value = null
                            runOnDefaultDispatcher { finishImportProfile(profile) }
                        },
                        shape = CircleShape,
                    ) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingProfileImport.value = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }

        pluginMissing?.let { (profileName, pluginEntity) ->
            AlertDialog(
                onDismissRequest = { missingPluginDialog.value = null },
                shape = RoundedCornerShape(28.dp),
                title = { Text(stringResource(R.string.missing_plugin)) },
                text = {
                    Text(stringResource(R.string.profile_requiring_plugin, profileName, pluginEntity.displayName))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            missingPluginDialog.value = null
                            launchCustomTab(pluginEntity.downloadSource.downloadLink)
                        },
                        shape = CircleShape,
                    ) { Text(stringResource(R.string.action_download)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        missingPluginDialog.value = null
                        launchCustomTab("https://matsuridayo.github.io/nb4a-plugin/")
                    }) { Text(stringResource(R.string.action_learn_more)) }
                },
            )
        }

        if (previewHint) {
            AlertDialog(
                onDismissRequest = { showPreviewDialog.value = false },
                shape = RoundedCornerShape(28.dp),
                title = { Text(BuildConfig.PRE_VERSION_NAME) },
                text = { Text(stringResource(R.string.preview_version_hint)) },
                confirmButton = {
                    Button(onClick = { showPreviewDialog.value = false }, shape = CircleShape) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            )
        }
    }

    @Composable
    private fun ExpressiveConnectionDock(
        state: BaseService.State,
        txRate: Long,
        rxRate: Long,
        onToggleConnection: () -> Unit,
        onUrlTest: () -> Int,
        modifier: Modifier = Modifier,
    ) {
        val isConnected = state == BaseService.State.Connected
        val isTransitioning = state == BaseService.State.Connecting || state == BaseService.State.Stopping
        var testing by remember { mutableStateOf(false) }
        var testStatusText by remember { mutableStateOf<String?>(null) }

        val dockContainerColor by animateColorAsState(
            targetValue = when {
                isConnected -> MaterialTheme.colorScheme.primaryContainer
                isTransitioning -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            label = "dockColor",
        )
        val backdrop = LocalBlurBackdrop.current
        Surface(
            onClick = {
                testing = true
                testStatusText = getString(R.string.connection_test_testing)
                runOnDefaultDispatcher {
                    val result = runCatching { onUrlTest() }
                    onMainDispatcher {
                        testing = false
                        testStatusText = result.fold(
                            onSuccess = { getString(R.string.connection_test_available, it) },
                            onFailure = { getString(R.string.connection_test_error, it.readableMessage) },
                        )
                    }
                }
            },
            enabled = isConnected && !testing,
            modifier = modifier
                .barGlass(
                    backdrop = backdrop,
                    shape = CircleShape,
                    containerColor = dockContainerColor,
                    drawFlatBackground = false,
                ),
            shape = CircleShape,
            color = if (backdrop != null) Color.Transparent else dockContainerColor,
            tonalElevation = if (backdrop != null) 0.dp else 6.dp,
            shadowElevation = if (backdrop != null) 0.dp else 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val statusLabel = when (state) {
                        BaseService.State.Connected -> testStatusText ?: stringResource(R.string.vpn_connected)
                        BaseService.State.Connecting -> stringResource(R.string.connecting)
                        BaseService.State.Stopping -> stringResource(R.string.stopping)
                        else -> stringResource(R.string.not_connected)
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val txStr = Formatter.formatFileSize(this@MainActivity, txRate)
                    val rxStr = Formatter.formatFileSize(this@MainActivity, rxRate)
                    Text(
                        text = if (isConnected) "↑ $txStr/s  •  ↓ $rxStr/s" else stringResource(R.string.connection_test_pending),
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = if (isConnected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = onToggleConnection,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                    containerColor = if (isConnected) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isConnected) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    modifier = Modifier.size(56.dp),
                ) {
                    if (isTransitioning) {
                        LoadingIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = if (isConnected) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = if (isConnected) stringResource(R.string.stop) else stringResource(R.string.connect),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MessageStore.setCurrentActivity(this)
        if (DataStore.hideFromRecentApps) {
            applyHideFromRecentApps(DataStore.hideFromRecentApps)
        }
    }

    fun applyHideFromRecentApps(hide: Boolean) {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = activityManager.appTasks
            if (tasks.isNotEmpty()) {
                tasks[0].setExcludeFromRecents(hide)
            }
        } catch (e: Exception) {
            Logs.w("Failed to set excludeFromRecents: ${e.message}")
        }
    }

    fun refreshNavMenu(clashApi: Boolean) {
        enableClashApiFlow.value = clashApi
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data ?: return
        runOnDefaultDispatcher {
            if (uri.scheme == "sn" && uri.host == "subscription" || uri.scheme == "clash") {
                importSubscription(uri)
            } else {
                importProfile(uri)
            }
        }
    }

    fun urlTest(): Int {
        if (!DataStore.serviceState.connected || connection.service == null) {
            error("not started")
        }
        return connection.service!!.urlTest()
    }

    suspend fun importSubscription(uri: Uri) {
        val group: ProxyGroup
        val url = uri.getQueryParameter("url")
        if (!url.isNullOrBlank()) {
            group = ProxyGroup(type = GroupType.SUBSCRIPTION)
            val subscription = SubscriptionBean()
            group.subscription = subscription
            subscription.link = url
            group.name = uri.getQueryParameter("name")
        } else {
            val data = uri.encodedQuery.takeIf { !it.isNullOrBlank() } ?: return
            try {
                group = KryoConverters.deserialize(
                    ProxyGroup().apply { export = true }, Util.zlibDecompress(Util.b64Decode(data))
                ).apply { export = false }
            } catch (e: Exception) {
                onMainDispatcher { showComposeSnackbar(e.readableMessage) }
                return
            }
        }

        val name = group.name.takeIf { !it.isNullOrBlank() }
            ?: group.subscription?.link
            ?: group.subscription?.token
        if (name.isNullOrBlank()) return

        group.name = group.name.takeIf { !it.isNullOrBlank() }
            ?: ("Subscription #" + System.currentTimeMillis())

        pendingSubscriptionImport.value = group
    }

    private suspend fun finishImportSubscription(subscription: ProxyGroup) {
        GroupManager.createGroup(subscription)
        GroupUpdater.startUpdate(subscription, true)
    }

    suspend fun importProfile(uri: Uri) {
        val profile = try {
            parseProxies(uri.toString()).getOrNull(0) ?: error(getString(R.string.no_proxies_found))
        } catch (e: Exception) {
            onMainDispatcher { showComposeSnackbar(e.readableMessage) }
            return
        }
        pendingProfileImport.value = profile
    }

    private suspend fun finishImportProfile(profile: AbstractBean) {
        val targetId = DataStore.selectedGroupForImport()
        ProfileManager.createProfile(targetId, profile)
        onMainDispatcher {
            currentDestination.value = MainDestination.CONFIGURATION
            profileRefreshSignal.value++
            showComposeSnackbar(resources.getQuantityString(R.plurals.added, 1, 1))
        }
    }

    override fun missingPlugin(profileName: String, pluginName: String) {
        val pluginEntity = PluginEntry.find(pluginName)
        if (pluginEntity == null) {
            showComposeSnackbar(getString(R.string.plugin_unknown, pluginName))
            return
        }
        missingPluginDialog.value = profileName to pluginEntity
    }

    private fun changeState(state: BaseService.State, msg: String? = null) {
        DataStore.serviceState = state
        serviceStateFlow.value = state
        profileRefreshSignal.value++
        if (state != BaseService.State.Connected) {
            txRateFlow.value = 0L
            rxRateFlow.value = 0L
        }
        if (msg != null) {
            showComposeSnackbar(getString(R.string.vpn_error, msg))
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state, msg)
    }

    override fun onServiceConnected(service: ISagerNetService) = changeState(
        try {
            BaseService.State.values()[service.state]
        } catch (_: RemoteException) {
            BaseService.State.Idle
        }
    )

    override fun onServiceDisconnected() = changeState(BaseService.State.Idle)

    override fun onBinderDied() {
        connection.disconnect(this)
        connection.connect(this, this)
    }

    override fun cbSpeedUpdate(stats: SpeedDisplayData) {
        txRateFlow.value = stats.txRateProxy
        rxRateFlow.value = stats.rxRateProxy
    }

    override suspend fun cbTrafficUpdate(data: TrafficDataBatch) {
        ProfileManager.postUpdate(data.items)
    }

    override fun cbSelectorUpdate(id: Long) {
        val old = DataStore.selectedProxy
        DataStore.selectedProxy = id
        DataStore.currentProfile = id
        profileRefreshSignal.value++
        runOnDefaultDispatcher {
            ProfileManager.postUpdate(old, true)
            ProfileManager.postUpdate(id, true)
        }
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.SERVICE_MODE -> onBinderDied()
            Key.ENABLE_CLASH_API -> enableClashApiFlow.value = DataStore.enableClashAPI
            Key.PROXY_APPS, Key.BYPASS_MODE, Key.INDIVIDUAL -> {
                if (DataStore.serviceState.canStop) {
                    showComposeSnackbar(
                        message = getString(R.string.need_reload),
                        actionLabel = getString(R.string.apply),
                        onAction = { SagerNet.reloadService() },
                    )
                }
            }
        }
    }

    override fun onStart() {
        connection.updateConnectionId(SagerConnection.CONNECTION_ID_MAIN_ACTIVITY_FOREGROUND)
        super.onStart()
    }

    override fun onStop() {
        connection.updateConnectionId(SagerConnection.CONNECTION_ID_MAIN_ACTIVITY_BACKGROUND)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        GroupManager.userInterface = null
        DataStore.configurationStore.unregisterChangeListener(this)
        connection.disconnect(this)
    }
}
