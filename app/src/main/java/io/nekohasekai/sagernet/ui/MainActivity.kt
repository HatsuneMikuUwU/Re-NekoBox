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
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
import io.nekohasekai.sagernet.ui.components.AppDialog
import io.nekohasekai.sagernet.ui.components.DialogCancelButton
import io.nekohasekai.sagernet.ui.components.DialogConfirmButton
import io.nekohasekai.sagernet.ui.components.FloatingTabBar
import io.nekohasekai.sagernet.ui.components.FloatingTabBarMargin
import io.nekohasekai.sagernet.ui.components.LocalBlurBackdrop
import io.nekohasekai.sagernet.ui.components.LocalBottomInset
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.bottomScrim
import io.nekohasekai.sagernet.ui.components.floatingTabBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.screens.AboutScreen
import io.nekohasekai.sagernet.ui.screens.ConfigurationScreen
import io.nekohasekai.sagernet.ui.screens.DashboardScreen
import io.nekohasekai.sagernet.ui.screens.GroupScreen
import io.nekohasekai.sagernet.ui.screens.LogcatScreen
import io.nekohasekai.sagernet.ui.screens.RouteScreen
import io.nekohasekai.sagernet.ui.screens.SettingsScreen
import io.nekohasekai.sagernet.ui.screens.ToolsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import moe.matsuri.nb4a.utils.Util

enum class MainDestination(
    val titleRes: Int,
    val icon: ImageVector,
    val inBottomBar: Boolean = false,
) {
    DASHBOARD(R.string.dashboard_title, Icons.Rounded.Dashboard, inBottomBar = true),
    CONFIGURATION(R.string.menu_configuration, Icons.Rounded.Router, inBottomBar = true),
    GROUP(R.string.menu_group, Icons.Rounded.FolderCopy, inBottomBar = true),
    ROUTE(R.string.menu_route, Icons.Rounded.AccountTree, inBottomBar = true),
    SETTINGS(R.string.settings, Icons.Rounded.Settings, inBottomBar = true),
    TOOLS(R.string.menu_tools, Icons.Rounded.Build),
    LOGCAT(R.string.menu_log, Icons.Rounded.Terminal),
    ABOUT(R.string.menu_about, Icons.Rounded.Info),
}

private const val SPEED_HISTORY_CAPACITY = 30

class MainActivity : ThemedActivity(),
    SagerConnection.Callback,
    OnPreferenceDataStoreChangeListener {

    val connection = SagerConnection(SagerConnection.CONNECTION_ID_MAIN_ACTIVITY_FOREGROUND, true)

    private val currentDestination = MutableStateFlow(MainDestination.DASHBOARD)
    private val subScreenReturnDestination = MutableStateFlow(MainDestination.SETTINGS)
    private val serviceStateFlow = MutableStateFlow(BaseService.State.Idle)
    private val speedDataFlow = MutableStateFlow(SpeedDisplayData())
    private val rxHistoryFlow = MutableStateFlow(List(SPEED_HISTORY_CAPACITY) { 0L })
    private val txHistoryFlow = MutableStateFlow(List(SPEED_HISTORY_CAPACITY) { 0L })
    private val connectedSinceFlow = MutableStateFlow<Long?>(null)
    private val urlTestRunningFlow = MutableStateFlow(false)
    private val urlTestStatusFlow = MutableStateFlow<String?>(null)
    private val urlTestLatencyMsFlow = MutableStateFlow<Int?>(null)
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

    private fun openSubScreen(target: MainDestination, returnTo: MainDestination) {
        subScreenReturnDestination.value = returnTo
        currentDestination.value = target
    }

    private fun toggleConnection() {
        if (DataStore.serviceState.canStop) {
            SagerNet.stopService()
        } else {
            connect.launch(null)
        }
    }

    private fun runConnectionUrlTest() {
        if (serviceStateFlow.value != BaseService.State.Connected || urlTestRunningFlow.value) return
        urlTestRunningFlow.value = true
        urlTestStatusFlow.value = getString(R.string.connection_test_testing)
        runOnDefaultDispatcher {
            val result = runCatching { urlTest() }
            onMainDispatcher {
                urlTestRunningFlow.value = false
                result.fold(
                    onSuccess = { ms ->
                        urlTestLatencyMsFlow.value = ms
                        urlTestStatusFlow.value = getString(R.string.connection_test_available, ms)
                    },
                    onFailure = { err ->
                        urlTestLatencyMsFlow.value = null
                        urlTestStatusFlow.value = getString(R.string.connection_test_error, err.readableMessage)
                    },
                )
            }
        }
    }

    @Composable
    private fun MainExpressiveShell() {
        val destination by currentDestination.collectAsState()
        val returnDest by subScreenReturnDestination.collectAsState()
        val serviceState by serviceStateFlow.collectAsState()
        val speedData by speedDataFlow.collectAsState()
        val rxHistory by rxHistoryFlow.collectAsState()
        val txHistory by txHistoryFlow.collectAsState()
        val connectedSince by connectedSinceFlow.collectAsState()
        val urlTestRunning by urlTestRunningFlow.collectAsState()
        val urlTestStatus by urlTestStatusFlow.collectAsState()
        val urlTestLatencyMs by urlTestLatencyMsFlow.collectAsState()
        val refreshTick by profileRefreshSignal.collectAsState()

        val subImport by pendingSubscriptionImport.collectAsState()
        val profImport by pendingProfileImport.collectAsState()
        val pluginMissing by missingPluginDialog.collectAsState()
        val previewHint by showPreviewDialog.collectAsState()

        val isSubScreen = destination == MainDestination.TOOLS ||
            destination == MainDestination.ABOUT ||
            destination == MainDestination.LOGCAT

        BackHandler(enabled = destination != MainDestination.DASHBOARD) {
            currentDestination.value = if (isSubScreen) {
                returnDest
            } else {
                MainDestination.DASHBOARD
            }
        }

        val bottomTabs = remember { MainDestination.entries.filter { it.inBottomBar } }
        val backdrop = rememberBlurBackdrop()
        val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomInset = if (!isSubScreen) {
            floatingTabBarSpace()
        } else {
            navBarInset
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
                    val navigateBack: () -> Unit = { currentDestination.value = returnDest }
                    AnimatedContent(
                        targetState = destination,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "mainDestinationTransition",
                        modifier = Modifier.fillMaxSize(),
                    ) { target ->
                        when (target) {
                            MainDestination.DASHBOARD -> DashboardScreen(
                                serviceState = serviceState,
                                speedData = speedData,
                                rxHistory = rxHistory,
                                txHistory = txHistory,
                                connectedSinceElapsed = connectedSince,
                                urlTestRunning = urlTestRunning,
                                urlTestStatus = urlTestStatus,
                                urlTestLatencyMs = urlTestLatencyMs,
                                refreshSignal = refreshTick,
                                onToggleConnection = { toggleConnection() },
                                onRunUrlTest = { runConnectionUrlTest() },
                                onOpenConfiguration = { currentDestination.value = MainDestination.CONFIGURATION },
                                onOpenGroups = { currentDestination.value = MainDestination.GROUP },
                                onOpenRoutes = { currentDestination.value = MainDestination.ROUTE },
                                onOpenLogcat = { openSubScreen(MainDestination.LOGCAT, MainDestination.DASHBOARD) },
                                onShowMessage = { showComposeSnackbar(it) },
                            )
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
                                onOpenTools = { openSubScreen(MainDestination.TOOLS, MainDestination.SETTINGS) },
                                onOpenLogcat = { openSubScreen(MainDestination.LOGCAT, MainDestination.SETTINGS) },
                                onOpenAbout = { openSubScreen(MainDestination.ABOUT, MainDestination.SETTINGS) },
                                onHideFromRecentAppsChanged = { applyHideFromRecentApps(it) },
                                onShowMessage = { msg, actionLabel, onAction ->
                                    showComposeSnackbar(msg, actionLabel = actionLabel, onAction = onAction)
                                },
                            )
                            MainDestination.TOOLS -> ToolsScreen(
                                onBack = navigateBack,
                                onShowMessage = { showComposeSnackbar(it) },
                            )
                            MainDestination.LOGCAT -> LogcatScreen(
                                onBack = navigateBack,
                                onShowMessage = { showComposeSnackbar(it) },
                            )
                            MainDestination.ABOUT -> AboutScreen(
                                onBack = navigateBack,
                                onShowMessage = { showComposeSnackbar(it) },
                            )
                        }
                    }
                }

                if (!isSubScreen) {
                    val bg = MaterialTheme.colorScheme.background
                    val fadeBrush = remember(bg) { bottomScrim(bg) }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(floatingTabBarSpace() + 24.dp)
                            .background(fadeBrush),
                    )

                    FloatingTabBar(
                        items = bottomTabs,
                        selectedIndex = { bottomTabs.indexOf(destination).coerceAtLeast(0) },
                        onSelected = { idx -> currentDestination.value = bottomTabs[idx] },
                        tabMinWidth = 64.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = FloatingTabBarMargin),
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

                SnackbarHost(
                    hostState = composeSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomInset + 12.dp),
                )
            }
        }

        subImport?.let { group ->
            val displayName = group.name.takeIf { !it.isNullOrBlank() } ?: "Subscription"
            AppDialog(
                icon = Icons.Rounded.CloudDownload,
                title = stringResource(R.string.subscription_import),
                onDismiss = { pendingSubscriptionImport.value = null },
                text = { Text(stringResource(R.string.subscription_import_message, displayName)) },
                confirmButton = {
                    DialogConfirmButton(
                        label = stringResource(R.string.yes),
                        onClick = {
                            pendingSubscriptionImport.value = null
                            currentDestination.value = MainDestination.GROUP
                            runOnDefaultDispatcher { finishImportSubscription(group) }
                        },
                    )
                },
                dismissButton = {
                    DialogCancelButton(
                        label = stringResource(android.R.string.cancel),
                        onClick = { pendingSubscriptionImport.value = null },
                    )
                },
            )
        }

        profImport?.let { profile ->
            AppDialog(
                icon = Icons.Rounded.Download,
                title = stringResource(R.string.profile_import),
                onDismiss = { pendingProfileImport.value = null },
                text = { Text(stringResource(R.string.profile_import_message, profile.displayName())) },
                confirmButton = {
                    DialogConfirmButton(
                        label = stringResource(R.string.yes),
                        onClick = {
                            pendingProfileImport.value = null
                            runOnDefaultDispatcher { finishImportProfile(profile) }
                        },
                    )
                },
                dismissButton = {
                    DialogCancelButton(
                        label = stringResource(android.R.string.cancel),
                        onClick = { pendingProfileImport.value = null },
                    )
                },
            )
        }

        pluginMissing?.let { (profileName, pluginEntity) ->
            AppDialog(
                icon = Icons.Rounded.Extension,
                title = stringResource(R.string.missing_plugin),
                onDismiss = { missingPluginDialog.value = null },
                text = {
                    Text(stringResource(R.string.profile_requiring_plugin, profileName, pluginEntity.displayName))
                },
                confirmButton = {
                    DialogConfirmButton(
                        label = stringResource(R.string.action_download),
                        onClick = {
                            missingPluginDialog.value = null
                            launchCustomTab(pluginEntity.downloadSource.downloadLink)
                        },
                    )
                },
                dismissButton = {
                    DialogCancelButton(
                        label = stringResource(R.string.action_learn_more),
                        onClick = {
                            missingPluginDialog.value = null
                            launchCustomTab("https://matsuridayo.github.io/nb4a-plugin/")
                        },
                    )
                },
            )
        }

        if (previewHint) {
            AppDialog(
                icon = Icons.Rounded.Info,
                title = BuildConfig.PRE_VERSION_NAME,
                onDismiss = { showPreviewDialog.value = false },
                text = { Text(stringResource(R.string.preview_version_hint)) },
                confirmButton = {
                    DialogConfirmButton(
                        label = stringResource(android.R.string.ok),
                        onClick = { showPreviewDialog.value = false },
                    )
                },
            )
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
        if (state == BaseService.State.Connected) {
            if (connectedSinceFlow.value == null) {
                connectedSinceFlow.value = SystemClock.elapsedRealtime()
            }
        } else {
            connectedSinceFlow.value = null
            speedDataFlow.value = SpeedDisplayData()
            rxHistoryFlow.value = List(SPEED_HISTORY_CAPACITY) { 0L }
            txHistoryFlow.value = List(SPEED_HISTORY_CAPACITY) { 0L }
            urlTestRunningFlow.value = false
            urlTestStatusFlow.value = null
            urlTestLatencyMsFlow.value = null
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
        speedDataFlow.value = stats
        rxHistoryFlow.value = (rxHistoryFlow.value.drop(1) + stats.rxRateProxy)
        txHistoryFlow.value = (txHistoryFlow.value.drop(1) + stats.txRateProxy)
    }

    override suspend fun cbTrafficUpdate(data: TrafficDataBatch) {
        ProfileManager.postUpdate(data.items)
        profileRefreshSignal.value++
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
