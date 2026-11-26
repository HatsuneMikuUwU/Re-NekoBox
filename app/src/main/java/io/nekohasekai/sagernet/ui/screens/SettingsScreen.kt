package io.nekohasekai.sagernet.ui.screens

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.RuleFolder
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.triggerFullRestart
import io.nekohasekai.sagernet.ui.AppManagerActivity
import io.nekohasekai.sagernet.ui.components.ColorThemePickerDialog
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.MtuPickerDialog
import io.nekohasekai.sagernet.ui.components.PrefActionRow
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.ui.components.StoreEditTextPref
import io.nekohasekai.sagernet.ui.components.StoreListPref
import io.nekohasekai.sagernet.ui.components.StoreSwitchPref
import io.nekohasekai.sagernet.ui.components.rememberComposePreferenceStore
import io.nekohasekai.sagernet.ui.profile.ConfigEditActivity
import io.nekohasekai.sagernet.ui.theme.AppThemeState
import io.nekohasekai.sagernet.ui.theme.ReNekoBoxThemeSeeds
import io.nekohasekai.sagernet.utils.AppLocale
import java.io.File

@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit,
    onClashApiChanged: (Boolean) -> Unit,
    onHideFromRecentAppsChanged: (Boolean) -> Unit,
    onShowMessage: (String, String?, (() -> Unit)?) -> Unit,
) {
    val context = LocalContext.current
    val store = rememberComposePreferenceStore(DataStore.configurationStore)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) { DataStore.initGlobal() }

    fun notifyNeedReload() {
        if (DataStore.serviceState.canStop) {
            onShowMessage(
                context.getString(R.string.need_reload),
                context.getString(R.string.apply),
                { SagerNet.reloadService() },
            )
        }
    }

    fun notifyNeedRestart() {
        onShowMessage(
            context.getString(R.string.need_restart),
            context.getString(R.string.apply),
            { triggerFullRestart(context) },
        )
    }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showMtuDialog by remember { mutableStateOf(false) }
    var showLocalProxyDialog by remember { mutableStateOf(false) }
    var showUrlTestDialog by remember { mutableStateOf(false) }
    var showLogBufDialog by remember { mutableStateOf(false) }
    var showAppendHttpConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    val currentThemeId = store.getInt(Key.APP_THEME, 2)
    val currentThemeSeed = ReNekoBoxThemeSeeds.firstOrNull { it.id == currentThemeId }
        ?: ReNekoBoxThemeSeeds.first()
    val currentNightMode = store.getString(Key.NIGHT_THEME, "0").toIntOrNull() ?: 0

    val enableHevTun = store.getBoolean(Key.ENABLE_HEV_TUN, false)
    val speedInterval = store.getString(Key.SPEED_INTERVAL, "1000")
    val rulesProvider = store.getString(Key.RULES_PROVIDER, "0").toIntOrNull() ?: 0
    val appendHttpProxy = store.getBoolean(Key.APPEND_HTTP_PROXY, false)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            // Material 3 Expressive Appearance Hero Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { showThemeDialog = true }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.theme),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = currentThemeSeed.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                ).forEach { swatch ->
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(swatch),
                                    )
                                }
                            }
                        }

                        val nightLabels = listOf("Auto", "Dark", "Light")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            nightLabels.forEachIndexed { idx, label ->
                                SegmentedButton(
                                    selected = currentNightMode == idx,
                                    onClick = {
                                        store.putString(Key.NIGHT_THEME, idx.toString())
                                        AppThemeState.updateNightTheme(idx)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(idx, nightLabels.size),
                                ) {
                                    Text(label, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // General Settings
            item {
                PrefCategory(titleRes = R.string.general_settings) {
                    StoreSwitchPref(
                        store = store,
                        key = Key.PERSIST_ACROSS_REBOOT,
                        titleRes = R.string.auto_connect,
                        summaryRes = R.string.auto_connect_summary,
                        icon = Icons.Rounded.RestartAlt,
                        position = ExpressiveGroupPosition.TOP,
                    )
                    StoreListPref(
                        store = store,
                        key = Key.APP_LANGUAGE,
                        titleRes = R.string.language,
                        entriesRes = R.array.language_entries,
                        entryValuesRes = R.array.language_values,
                        defaultValue = "",
                        icon = Icons.Rounded.Language,
                        onChanged = { AppLocale.apply(it) },
                    )
                    StoreListPref(
                        store = store,
                        key = Key.SERVICE_MODE,
                        titleRes = R.string.service_mode,
                        entriesRes = R.array.service_modes,
                        entryValuesRes = R.array.service_mode_values,
                        defaultValue = Key.MODE_VPN,
                        icon = Icons.Rounded.Tune,
                        onChanged = {
                            if (DataStore.serviceState.started) SagerNet.stopService()
                        },
                    )
                    StoreListPref(
                        store = store,
                        key = Key.TUN_IMPLEMENTATION,
                        titleRes = R.string.tun_implementation,
                        entriesRes = R.array.tun_implementation,
                        entryValuesRes = R.array.int_array_3,
                        defaultValue = "0",
                        enabled = !enableHevTun,
                        icon = Icons.Rounded.SettingsEthernet,
                        onChanged = { notifyNeedReload() },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.ENABLE_HEV_TUN,
                        titleRes = R.string.enable_hev_tun,
                        summaryRes = R.string.enable_hev_tun_summary,
                        icon = Icons.Rounded.Memory,
                        onChanged = { notifyNeedReload(); true },
                    )
                    PrefActionRow(
                        title = stringResource(R.string.mtu),
                        summary = store.getString(Key.MTU, "9000"),
                        icon = Icons.Rounded.Public,
                        onClick = { showMtuDialog = true },
                    )
                    StoreListPref(
                        store = store,
                        key = Key.SPEED_INTERVAL,
                        titleRes = R.string.speed_interval,
                        entriesRes = R.array.notification_entry,
                        entryValuesRes = R.array.notification_value,
                        defaultValue = "1000",
                        icon = Icons.Rounded.Speed,
                        onChanged = { notifyNeedReload() },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.PROFILE_TRAFFIC_STATISTICS,
                        titleRes = R.string.profile_traffic_statistics,
                        summaryRes = R.string.profile_traffic_statistics_summary,
                        defaultValue = true,
                        enabled = speedInterval != "0",
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.SHOW_DIRECT_SPEED,
                        titleRes = R.string.show_direct_speed,
                        summaryRes = R.string.show_direct_speed_sum,
                        defaultValue = true,
                        onChanged = { notifyNeedReload(); true },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = "showGroupInNotification",
                        titleRes = R.string.show_group_in_notification,
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.CONFIRM_PROFILE_DELETE,
                        titleRes = R.string.confirm_profile_delete,
                        defaultValue = true,
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.ALWAYS_SHOW_ADDRESS,
                        titleRes = R.string.always_show_address,
                        summaryRes = R.string.always_show_address_sum,
                    )
                    if (Build.VERSION.SDK_INT >= 28) {
                        StoreSwitchPref(
                            store = store,
                            key = Key.METERED_NETWORK,
                            titleRes = R.string.metered,
                            summaryRes = R.string.metered_summary,
                        )
                    }
                    StoreSwitchPref(
                        store = store,
                        key = Key.ACQUIRE_WAKE_LOCK,
                        titleRes = R.string.acquire_wake_lock,
                        summaryRes = R.string.acquire_wake_lock_summary,
                        onChanged = { notifyNeedReload(); true },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.HIDE_FROM_RECENT_APPS,
                        titleRes = R.string.hide_from_recent_apps,
                        summaryRes = R.string.hide_from_recent_apps_summary,
                        onChanged = { onHideFromRecentAppsChanged(it); true },
                    )
                    StoreListPref(
                        store = store,
                        key = Key.LOG_LEVEL,
                        titleRes = R.string.log_level,
                        entriesRes = R.array.log_level,
                        entryValuesRes = R.array.int_array_5,
                        defaultValue = "0",
                        icon = Icons.Rounded.BugReport,
                        onLongClick = { showLogBufDialog = true },
                        onChanged = { notifyNeedRestart() },
                    )
                    PrefActionRow(
                        title = stringResource(R.string.custom_config),
                        summary = if (store.getString(Key.GLOBAL_CUSTOM_CONFIG).isBlank()) {
                            stringResource(R.string.not_set)
                        } else {
                            stringResource(R.string.lines, store.getString(Key.GLOBAL_CUSTOM_CONFIG).lines().size)
                        },
                        icon = Icons.Rounded.Layers,
                        position = ExpressiveGroupPosition.BOTTOM,
                        onClick = {
                            context.startActivity(
                                Intent(context, ConfigEditActivity::class.java).apply {
                                    putExtra("key", Key.GLOBAL_CUSTOM_CONFIG)
                                    putExtra("useConfigStore", true)
                                }
                            )
                        },
                    )
                }
            }

            // Route Settings
            item {
                PrefCategory(titleRes = R.string.cag_route) {
                    StoreSwitchPref(
                        store = store,
                        key = Key.PROXY_APPS,
                        titleRes = R.string.proxied_apps,
                        summaryRes = R.string.proxied_apps_summary,
                        icon = Icons.Rounded.Apps,
                        position = ExpressiveGroupPosition.TOP,
                        onChanged = { newVal ->
                            context.startActivity(Intent(context, AppManagerActivity::class.java))
                            if (newVal) DataStore.dirty = true
                            true
                        },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.BYPASS_LAN,
                        titleRes = R.string.route_opt_bypass_lan,
                        onChanged = { notifyNeedReload(); true },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.BYPASS_LAN_IN_CORE,
                        titleRes = R.string.bypass_lan_in_core,
                        onChanged = { notifyNeedReload(); true },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.CONCURRENT_DIAL,
                        titleRes = R.string.concurrent_dial,
                        summaryRes = R.string.concurrent_dial_summary,
                        onChanged = { notifyNeedReload(); true },
                    )
                    StoreListPref(
                        store = store,
                        key = Key.TRAFFIC_SNIFFING,
                        titleRes = R.string.traffic_sniffing,
                        entriesRes = R.array.traffic_sniffing_values,
                        entryValuesRes = R.array.int_array_3,
                        defaultValue = "1",
                        onChanged = { notifyNeedReload() },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.RESOLVE_DESTINATION,
                        titleRes = R.string.resolve_destination,
                        summaryRes = R.string.resolve_destination_summary,
                        onChanged = { notifyNeedReload(); true },
                    )
                    StoreListPref(
                        store = store,
                        key = Key.IPV6_MODE,
                        titleRes = R.string.ipv6,
                        entriesRes = R.array.ipv6_mode,
                        entryValuesRes = R.array.int_array_4,
                        defaultValue = "0",
                        onChanged = { notifyNeedReload() },
                    )
                    StoreListPref(
                        store = store,
                        key = Key.RULES_PROVIDER,
                        titleRes = R.string.route_rules_provider,
                        entriesRes = R.array.rules_dat_provider,
                        entryValuesRes = R.array.int_array_5,
                        defaultValue = "0",
                        icon = Icons.Rounded.RuleFolder,
                    )
                    if (rulesProvider == 4) {
                        StoreEditTextPref(store = store, key = Key.RULES_GEOSITE_URL, titleRes = R.string.rules_geosite_url)
                        StoreEditTextPref(store = store, key = Key.RULES_GEOIP_URL, titleRes = R.string.rules_geoip_url)
                    }
                    StoreEditTextPref(
                        store = store,
                        key = Key.RULES_UPDATE_INTERVAL,
                        titleRes = R.string.ruleset_update_interval,
                        defaultValue = "0",
                        isNumber = true,
                        position = ExpressiveGroupPosition.BOTTOM,
                    )
                }
            }

            // TLS Fragment
            item {
                PrefCategory(titleRes = R.string.fragment_settings) {
                    StoreSwitchPref(
                        store = store,
                        key = Key.ENABLE_TLS_FRAGMENT,
                        titleRes = R.string.enable_tls_fragment,
                        icon = Icons.Rounded.Security,
                        position = ExpressiveGroupPosition.TOP,
                        onChanged = { notifyNeedReload(); true },
                    )
                    StoreEditTextPref(store = store, key = Key.FRAGMENT_LENGTH, titleRes = R.string.fragment_length, defaultValue = "100-200")
                    StoreEditTextPref(
                        store = store,
                        key = Key.FRAGMENT_INTERVAL,
                        titleRes = R.string.fragment_interval,
                        defaultValue = "10-20",
                        position = ExpressiveGroupPosition.BOTTOM,
                    )
                }
            }

            // DNS Settings
            item {
                PrefCategory(titleRes = R.string.cag_dns) {
                    StoreEditTextPref(
                        store = store,
                        key = Key.REMOTE_DNS,
                        titleRes = R.string.remote_dns,
                        defaultValue = "https://dns.google/dns-query",
                        icon = Icons.Rounded.Dns,
                        position = ExpressiveGroupPosition.TOP,
                        onChanged = { notifyNeedReload() },
                    )
                    StoreListPref(store = store, key = "domain_strategy_for_remote", titleRes = R.string.domain_strategy_for_remote, entriesRes = R.array.dns_network_select, defaultValue = "auto")
                    StoreEditTextPref(
                        store = store,
                        key = Key.DIRECT_DNS,
                        titleRes = R.string.direct_dns,
                        defaultValue = "https://223.5.5.5/dns-query",
                        icon = Icons.Rounded.Dns,
                        onChanged = { notifyNeedReload() },
                    )
                    StoreListPref(store = store, key = "domain_strategy_for_direct", titleRes = R.string.domain_strategy_for_direct, entriesRes = R.array.dns_network_select, defaultValue = "auto")
                    StoreListPref(store = store, key = "domain_strategy_for_server", titleRes = R.string.domain_strategy_for_server, entriesRes = R.array.dns_network_select, defaultValue = "auto")
                    StoreSwitchPref(
                        store = store,
                        key = Key.ENABLE_DNS_ROUTING,
                        titleRes = R.string.enable_dns_routing,
                        summaryRes = R.string.dns_routing_message,
                        defaultValue = true,
                        onChanged = { notifyNeedReload(); true },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.ENABLE_FAKEDNS,
                        titleRes = R.string.enable_fakedns,
                        summaryRes = R.string.fakedns_message,
                        defaultValue = true,
                        icon = Icons.Rounded.Lock,
                        onChanged = { notifyNeedReload(); true },
                    )
                    StoreEditTextPref(
                        store = store,
                        key = Key.DNS_HOSTS,
                        titleRes = R.string.dns_hosts,
                        dialogMessageRes = R.string.dns_hosts_dialog_message,
                        multiline = true,
                        position = ExpressiveGroupPosition.BOTTOM,
                        onChanged = { notifyNeedReload() },
                    )
                }
            }

            // Inbound Settings
            item {
                val host = if (DataStore.allowAccess) "0.0.0.0" else "127.0.0.1"
                PrefCategory(titleRes = R.string.inbound_settings) {
                    PrefActionRow(
                        title = stringResource(R.string.local_proxy_settings),
                        summary = "$host:${DataStore.mixedPort}",
                        icon = Icons.Rounded.Router,
                        position = ExpressiveGroupPosition.TOP,
                        onClick = { showLocalProxyDialog = true },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.APPEND_HTTP_PROXY,
                        titleRes = R.string.append_http_proxy,
                        summaryRes = R.string.append_http_proxy_sum,
                        onChanged = { newVal ->
                            if (newVal) {
                                showAppendHttpConfirm = true
                                false
                            } else {
                                notifyNeedReload()
                                true
                            }
                        },
                    )
                    StoreEditTextPref(
                        store = store,
                        key = Key.HTTP_PROXY_BYPASS,
                        titleRes = R.string.http_proxy_bypass,
                        dialogMessageRes = R.string.http_proxy_bypass_dialog_message,
                        enabled = appendHttpProxy,
                        multiline = true,
                        onChanged = { notifyNeedReload() },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.STRICT_ROUTE,
                        titleRes = R.string.strict_route,
                        defaultValue = true,
                        position = ExpressiveGroupPosition.BOTTOM,
                        onChanged = { notifyNeedReload(); true },
                    )
                }
            }

            // Misc Settings
            item {
                PrefCategory(titleRes = R.string.cag_misc) {
                    PrefActionRow(
                        title = stringResource(R.string.connection_test_url),
                        summary = "${DataStore.connectionTestURL} (${DataStore.connectionTestConcurrent}x, ${DataStore.connectionTestTimeout}ms)",
                        icon = Icons.Rounded.NetworkCheck,
                        position = ExpressiveGroupPosition.TOP,
                        onClick = { showUrlTestDialog = true },
                    )
                    StoreSwitchPref(
                        store = store,
                        key = Key.ENABLE_CLASH_API,
                        titleRes = R.string.enable_clash_api,
                        summaryRes = R.string.enable_clash_api_summary,
                        icon = Icons.Rounded.Construction,
                        onChanged = {
                            onClashApiChanged(it)
                            notifyNeedReload()
                            true
                        },
                    )
                    StoreSwitchPref(store = store, key = Key.NETWORK_CHANGE_RESET_CONNECTIONS, titleRes = R.string.network_change_reset_connections, defaultValue = true)
                    StoreSwitchPref(store = store, key = Key.WAKE_RESET_CONNECTIONS, titleRes = R.string.wake_reset_connections)
                    StoreSwitchPref(store = store, key = Key.GLOBAL_ALLOW_INSECURE, titleRes = R.string.global_allow_insecure, icon = Icons.Rounded.LockOpen)
                    StoreSwitchPref(store = store, key = Key.ALLOW_INSECURE_ON_REQUEST, titleRes = R.string.allow_insecure_on_request_sum)
                    StoreListPref(store = store, key = Key.APP_TLS_VERSION, titleRes = R.string.app_tls_version, entriesRes = R.array.app_tls_version, defaultValue = "1.2")
                    StoreSwitchPref(store = store, key = Key.SHOW_BOTTOM_BAR, titleRes = R.string.show_bottom_bar)
                    PrefActionRow(title = stringResource(R.string.reset_settings), icon = Icons.Rounded.RestartAlt, onClick = { showResetConfirm = true })
                    PrefActionRow(
                        title = stringResource(R.string.clear_cache),
                        summary = stringResource(R.string.clear_cache_summary),
                        icon = Icons.Rounded.CleaningServices,
                        position = ExpressiveGroupPosition.BOTTOM,
                        onClick = { showClearCacheConfirm = true },
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ColorThemePickerDialog(
            currentThemeId = currentThemeId,
            onSelect = { seed ->
                AppThemeState.updateAppTheme(seed.id)
                if (DataStore.serviceState.started) SagerNet.reloadService()
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showMtuDialog) {
        MtuPickerDialog(
            currentMtu = store.getString(Key.MTU, "9000"),
            presets = stringArrayResource(R.array.mtu_select).toList(),
            onConfirm = { newMtu ->
                store.putString(Key.MTU, newMtu)
                notifyNeedReload()
            },
            onDismiss = { showMtuDialog = false },
        )
    }

    if (showLocalProxyDialog) {
        var portText by remember { mutableStateOf(DataStore.mixedPort.toString()) }
        var allowLan by remember { mutableStateOf(DataStore.allowAccess) }
        var username by remember { mutableStateOf(DataStore.mixedUsername) }
        var password by remember { mutableStateOf(DataStore.mixedSecret) }

        AlertDialog(
            onDismissRequest = { showLocalProxyDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.local_proxy_settings)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it.filter(Char::isDigit) },
                        label = { Text("Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.allow_access), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = allowLan, onCheckedChange = { allowLan = it })
                    }
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.username)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = portText.toIntOrNull()
                        if (p == null || p !in 1..65535) {
                            Toast.makeText(context, R.string.port_out_of_range, Toast.LENGTH_SHORT).show()
                        } else {
                            DataStore.mixedPort = p
                            DataStore.allowAccess = allowLan
                            DataStore.mixedUsername = username.trim()
                            DataStore.configurationStore.putString(Key.MIXED_SECRET, password)
                            showLocalProxyDialog = false
                            notifyNeedReload()
                        }
                    },
                    shape = CircleShape,
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showLocalProxyDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (showUrlTestDialog) {
        var url by remember { mutableStateOf(DataStore.connectionTestURL) }
        var concurrent by remember { mutableStateOf(DataStore.connectionTestConcurrent.toString()) }
        var timeout by remember { mutableStateOf(DataStore.connectionTestTimeout.toString()) }

        AlertDialog(
            onDismissRequest = { showUrlTestDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.connection_test_url)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = concurrent, onValueChange = { concurrent = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.test_concurrency)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = timeout, onValueChange = { timeout = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.connection_test_timeout)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        DataStore.connectionTestURL = url.trim()
                        DataStore.connectionTestConcurrent = (concurrent.toIntOrNull() ?: 5).coerceIn(1, 64)
                        DataStore.connectionTestTimeout = (timeout.toIntOrNull() ?: 3000).coerceIn(500, 30000)
                        showUrlTestDialog = false
                    },
                    shape = CircleShape,
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showUrlTestDialog = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    if (showLogBufDialog) {
        var buf by remember { mutableStateOf((DataStore.logBufSize.takeIf { it > 0 } ?: 50).toString()) }
        AlertDialog(
            onDismissRequest = { showLogBufDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Log buffer size (KB)") },
            text = {
                OutlinedTextField(value = buf, onValueChange = { buf = it.filter(Char::isDigit) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = {
                        DataStore.logBufSize = (buf.toIntOrNull() ?: 50).coerceAtLeast(1)
                        showLogBufDialog = false
                        notifyNeedRestart()
                    },
                    shape = CircleShape,
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogBufDialog = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    if (showAppendHttpConfirm) {
        AlertDialog(
            onDismissRequest = { showAppendHttpConfirm = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.append_http_proxy_security_title)) },
            text = { Text(stringResource(R.string.append_http_proxy_security_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        store.putBoolean(Key.APPEND_HTTP_PROXY, true)
                        showAppendHttpConfirm = false
                        notifyNeedReload()
                    },
                    shape = CircleShape,
                ) { Text(stringResource(R.string.enable_anyway)) }
            },
            dismissButton = {
                TextButton(onClick = { showAppendHttpConfirm = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.confirm)) },
            text = { Text(stringResource(R.string.reset_settings_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        DataStore.configurationStore.reset()
                        triggerFullRestart(context)
                    },
                    shape = CircleShape,
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.no)) }
            },
        )
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.clear_cache)) },
            text = { Text(stringResource(R.string.clear_cache_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheConfirm = false
                        try {
                            clearDirFiles(SagerNet.application.cacheDir, setOf("neko.log"))
                            val rel = File(SagerNet.application.cacheDir.parentFile, "cache")
                            if (rel.exists() && rel.isDirectory) clearDirFiles(rel)
                            Toast.makeText(context, R.string.clear_cache_success, Toast.LENGTH_SHORT).show()
                            notifyNeedReload()
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.clear_cache_failed, e.message), Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = CircleShape,
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

private fun clearDirFiles(dir: File, skipFiles: Set<String> = emptySet()) {
    if (!dir.isDirectory) return
    dir.list()?.forEach { child ->
        val file = File(dir, child)
        if (child == "neko.log") {
            runCatching { file.writeText("") }
        } else if (child !in skipFiles) {
            if (file.isDirectory) clearDirFiles(file, skipFiles) else file.delete()
        }
    }
}
