@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.nekohasekai.sagernet.ui.screens

import android.os.SystemClock
import android.text.format.Formatter
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.RuleFolder
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.aidl.SpeedDisplayData
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.bg.ClashApiClient
import io.nekohasekai.sagernet.bg.ClashConnectionItem
import io.nekohasekai.sagernet.bg.ClashConnectionsSnapshot
import io.nekohasekai.sagernet.bg.ClashRuleItem
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.ui.components.ExpressiveEmptyState
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupedContainer
import io.nekohasekai.sagernet.ui.components.ExpressiveIconBadge
import io.nekohasekai.sagernet.ui.components.ExpressivePillBadge
import io.nekohasekai.sagernet.ui.components.ExpressiveSectionHeader
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.LocalBottomInset
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class DashboardTab {
    OVERVIEW,
    CONNECTIONS,
    RULES,
}

private data class DashboardSnapshot(
    val activeProfile: ProxyEntity? = null,
    val activeGroupName: String? = null,
    val profileCount: Int = 0,
    val groupCount: Int = 0,
    val enabledRuleCount: Int = 0,
)

@Composable
fun DashboardScreen(
    serviceState: BaseService.State,
    speedData: SpeedDisplayData,
    rxHistory: List<Long>,
    txHistory: List<Long>,
    connectedSinceElapsed: Long?,
    urlTestRunning: Boolean,
    urlTestStatus: String?,
    urlTestLatencyMs: Int?,
    refreshSignal: Int,
    onToggleConnection: () -> Unit,
    onRunUrlTest: () -> Unit,
    onOpenConfiguration: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenRoutes: () -> Unit,
    onOpenLogcat: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isConnected = serviceState == BaseService.State.Connected

    var selectedTab by remember { mutableStateOf(DashboardTab.OVERVIEW) }
    var snapshot by remember { mutableStateOf(DashboardSnapshot()) }
    var clashConnections by remember { mutableStateOf(ClashConnectionsSnapshot()) }
    var clashRules by remember { mutableStateOf<List<ClashRuleItem>>(emptyList()) }
    var globalMode by remember { mutableStateOf(DataStore.globalMode) }
    var uptimeSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(refreshSignal, serviceState) {
        globalMode = DataStore.globalMode
        snapshot = withContext(Dispatchers.IO) {
            val selectedId = DataStore.selectedProxy
            val activeProfile = if (selectedId > 0L) SagerDatabase.proxyDao.getById(selectedId) else null
            val groups = SagerDatabase.groupDao.allGroups()
            val activeGroupName = activeProfile?.let { profile ->
                groups.firstOrNull { it.id == profile.groupId }?.displayName()
            }
            val profileCount = SagerDatabase.proxyDao.getAll().size
            val enabledRuleCount = SagerDatabase.rulesDao.enabledRules().size
            DashboardSnapshot(
                activeProfile = activeProfile,
                activeGroupName = activeGroupName,
                profileCount = profileCount,
                groupCount = groups.size,
                enabledRuleCount = enabledRuleCount,
            )
        }
    }

    LaunchedEffect(isConnected, connectedSinceElapsed) {
        if (isConnected && connectedSinceElapsed != null) {
            while (true) {
                uptimeSeconds = ((SystemClock.elapsedRealtime() - connectedSinceElapsed) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        } else {
            uptimeSeconds = 0L
        }
    }

    // Native Clash API polling for active connections and sing-box memory
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            clashConnections = ClashConnectionsSnapshot()
            clashRules = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            ClashApiClient.fetchConnections()?.let { clashConnections = it }
            delay(1500L)
        }
    }

    // Fetch live runtime rules from Clash API when connected or switching to Rules tab
    LaunchedEffect(isConnected, selectedTab, globalMode) {
        if (isConnected && selectedTab == DashboardTab.RULES) {
            clashRules = ClashApiClient.fetchRules()
        }
    }

    val topInset = floatingTopBarSpace()
    val bottomInset = LocalBottomInset.current
    val topBackdrop = rememberBlurBackdrop()

    val serviceModeLabel = when (DataStore.serviceMode) {
        Key.MODE_VPN -> stringResource(R.string.service_mode_vpn)
        Key.MODE_PROXY -> stringResource(R.string.service_mode_proxy)
        else -> stringResource(R.string.service_mode_transproxy)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blurBackdropSource(topBackdrop),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topInset + 6.dp, bottom = bottomInset + 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Native Dashboard Sub-Tab Switcher
                item(key = "subtabs") {
                    DashboardSubTabBar(
                        selectedTab = selectedTab,
                        connectionCount = clashConnections.connections.size,
                        onSelectTab = { selectedTab = it },
                    )
                }

                when (selectedTab) {
                    DashboardTab.OVERVIEW -> {
                        // Hero Connection Card
                        item(key = "hero") {
                            ConnectionHeroCard(
                                serviceState = serviceState,
                                serviceModeLabel = serviceModeLabel,
                                uptimeSeconds = uptimeSeconds,
                                activeProfile = snapshot.activeProfile,
                                onToggleConnection = onToggleConnection,
                                onOpenConfiguration = onOpenConfiguration,
                            )
                        }

                        // Live Traffic & Throughput Section
                        item(key = "traffic_header") {
                            ExpressiveSectionHeader(title = stringResource(R.string.dashboard_traffic))
                        }

                        item(key = "speed_cards") {
                            val rxTotal = if (clashConnections.downloadTotal > 0L) {
                                clashConnections.downloadTotal
                            } else {
                                speedData.rxTotal
                            }
                            val txTotal = if (clashConnections.uploadTotal > 0L) {
                                clashConnections.uploadTotal
                            } else {
                                speedData.txTotal
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SpeedMetricCard(
                                    label = stringResource(R.string.dashboard_download),
                                    rateText = "${Formatter.formatFileSize(context, speedData.rxRateProxy)}/s",
                                    totalText = "${stringResource(R.string.dashboard_session_total)}: ${
                                        Formatter.formatFileSize(context, rxTotal)
                                    }",
                                    icon = Icons.Rounded.ArrowDownward,
                                    badgeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                SpeedMetricCard(
                                    label = stringResource(R.string.dashboard_upload),
                                    rateText = "${Formatter.formatFileSize(context, speedData.txRateProxy)}/s",
                                    totalText = "${stringResource(R.string.dashboard_session_total)}: ${
                                        Formatter.formatFileSize(context, txTotal)
                                    }",
                                    icon = Icons.Rounded.ArrowUpward,
                                    badgeContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    badgeContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        item(key = "traffic_chart") {
                            TrafficSparklineCard(
                                rxHistory = rxHistory,
                                txHistory = txHistory,
                                showDirectSpeed = DataStore.showDirectSpeed && (speedData.txRateDirect > 0L || speedData.rxRateDirect > 0L),
                                directTxRate = speedData.txRateDirect,
                                directRxRate = speedData.rxRateDirect,
                            )
                        }

                        // Native Clash API Telemetry (Active Connections + Memory)
                        item(key = "core_telemetry") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TelemetryChipCard(
                                    icon = Icons.Rounded.Hub,
                                    label = stringResource(R.string.dashboard_active_connections),
                                    value = if (isConnected) clashConnections.connections.size.toString() else "—",
                                    onClick = { selectedTab = DashboardTab.CONNECTIONS },
                                    modifier = Modifier.weight(1f),
                                )
                                TelemetryChipCard(
                                    icon = Icons.Rounded.Memory,
                                    label = stringResource(R.string.dashboard_memory),
                                    value = if (isConnected && clashConnections.memoryBytes > 0L) {
                                        Formatter.formatFileSize(context, clashConnections.memoryBytes)
                                    } else {
                                        "—"
                                    },
                                    onClick = null,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        // Quick Controls & URL Test Section
                        item(key = "quick_controls") {
                            Column {
                                ExpressiveSectionHeader(title = stringResource(R.string.dashboard_quick_actions))
                                ExpressiveGroupedContainer {
                                    // Connection / URL Test Row
                                    ExpressiveSurfaceItem(
                                        position = ExpressiveGroupPosition.TOP,
                                        onClick = if (isConnected && !urlTestRunning) onRunUrlTest else null,
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            ExpressiveIconBadge(
                                                icon = Icons.Rounded.NetworkCheck,
                                                containerColor = if (isConnected) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                                },
                                                contentColor = if (isConnected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.connection_test),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val testSubtitle = when {
                                                    urlTestRunning -> stringResource(R.string.connection_test_testing)
                                                    !isConnected -> stringResource(R.string.dashboard_url_test_connect_first)
                                                    !urlTestStatus.isNullOrBlank() -> urlTestStatus
                                                    else -> stringResource(R.string.dashboard_url_test_idle)
                                                }
                                                Text(
                                                    text = testSubtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            when {
                                                urlTestRunning -> LoadingIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                                isConnected && urlTestLatencyMs != null -> ExpressivePillBadge(
                                                    text = "${urlTestLatencyMs} ms",
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                )
                                                isConnected -> Icon(
                                                    imageVector = Icons.Rounded.Speed,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }
                                    }

                                    // Active Profile Row
                                    ExpressiveSurfaceItem(
                                        position = ExpressiveGroupPosition.MIDDLE,
                                        onClick = onOpenConfiguration,
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            ExpressiveIconBadge(icon = Icons.Rounded.Router)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = snapshot.activeProfile?.displayName()
                                                        ?: stringResource(R.string.dashboard_no_profile),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val profileSub = snapshot.activeProfile?.let { profile ->
                                                    val parts = buildList {
                                                        add(profile.displayType())
                                                        snapshot.activeGroupName?.takeIf { it.isNotBlank() }?.let { add(it) }
                                                        if (profile.tx > 0L || profile.rx > 0L) {
                                                            add(
                                                                "↑ ${Formatter.formatFileSize(context, profile.tx)} ↓ ${
                                                                    Formatter.formatFileSize(context, profile.rx)
                                                                }"
                                                            )
                                                        }
                                                    }
                                                    parts.joinToString(" • ")
                                                } ?: stringResource(R.string.dashboard_tap_select_profile)
                                                Text(
                                                    text = profileSub,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    // Routing Mode Toggle Row
                                    ExpressiveSurfaceItem(
                                        position = ExpressiveGroupPosition.BOTTOM,
                                        onClick = {
                                            val next = !globalMode
                                            globalMode = next
                                            DataStore.globalMode = next
                                            if (DataStore.serviceState.started) {
                                                SagerNet.reloadService()
                                            }
                                        },
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            ExpressiveIconBadge(icon = Icons.Rounded.AltRoute)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.dashboard_routing_mode),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (globalMode) {
                                                        stringResource(R.string.dashboard_mode_global)
                                                    } else {
                                                        stringResource(R.string.dashboard_mode_rule)
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Switch(
                                                checked = globalMode,
                                                onCheckedChange = { next ->
                                                    globalMode = next
                                                    DataStore.globalMode = next
                                                    if (DataStore.serviceState.started) {
                                                        SagerNet.reloadService()
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Overview Stats Row
                        item(key = "overview") {
                            Column {
                                ExpressiveSectionHeader(title = stringResource(R.string.dashboard_overview))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OverviewStatTile(
                                        icon = Icons.Rounded.Router,
                                        count = snapshot.profileCount,
                                        label = stringResource(R.string.menu_configuration),
                                        onClick = onOpenConfiguration,
                                        modifier = Modifier.weight(1f),
                                    )
                                    OverviewStatTile(
                                        icon = Icons.Rounded.FolderCopy,
                                        count = snapshot.groupCount,
                                        label = stringResource(R.string.menu_group),
                                        onClick = onOpenGroups,
                                        modifier = Modifier.weight(1f),
                                    )
                                    OverviewStatTile(
                                        icon = Icons.Rounded.AccountTree,
                                        count = snapshot.enabledRuleCount,
                                        label = stringResource(R.string.menu_route),
                                        onClick = onOpenRoutes,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    DashboardTab.CONNECTIONS -> {
                        if (!isConnected) {
                            item(key = "conn_disconnected") {
                                ExpressiveEmptyState(
                                    icon = Icons.Rounded.Hub,
                                    title = stringResource(R.string.not_connected),
                                    subtitle = stringResource(R.string.dashboard_no_connections_disconnected),
                                )
                            }
                        } else if (clashConnections.connections.isEmpty()) {
                            item(key = "conn_empty") {
                                ExpressiveEmptyState(
                                    icon = Icons.Rounded.Hub,
                                    title = stringResource(R.string.dashboard_no_connections),
                                    subtitle = stringResource(R.string.dashboard_active_connections),
                                )
                            }
                        } else {
                            item(key = "conn_header") {
                                ExpressiveSectionHeader(
                                    title = "${stringResource(R.string.dashboard_active_connections)} (${clashConnections.connections.size})",
                                    trailing = {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    if (ClashApiClient.closeAllConnections()) {
                                                        clashConnections = clashConnections.copy(connections = emptyList())
                                                        onShowMessage(context.getString(R.string.dashboard_connections_closed))
                                                    }
                                                }
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteSweep,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.dashboard_close_all))
                                        }
                                    },
                                )
                            }
                            itemsIndexed(
                                items = clashConnections.connections,
                                key = { index, item -> item.id.ifBlank { "conn_$index" } },
                            ) { index, item ->
                                val pos = when {
                                    clashConnections.connections.size == 1 -> ExpressiveGroupPosition.SINGLE
                                    index == 0 -> ExpressiveGroupPosition.TOP
                                    index == clashConnections.connections.lastIndex -> ExpressiveGroupPosition.BOTTOM
                                    else -> ExpressiveGroupPosition.MIDDLE
                                }
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    ConnectionRowItem(
                                        item = item,
                                        position = pos,
                                        onClose = {
                                            scope.launch {
                                                if (ClashApiClient.closeConnection(item.id)) {
                                                    clashConnections = clashConnections.copy(
                                                        connections = clashConnections.connections.filterNot { it.id == item.id }
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    DashboardTab.RULES -> {
                        if (!isConnected) {
                            item(key = "rules_disconnected") {
                                ExpressiveEmptyState(
                                    icon = Icons.Rounded.RuleFolder,
                                    title = stringResource(R.string.not_connected),
                                    subtitle = stringResource(R.string.dashboard_no_connections_disconnected),
                                )
                            }
                        } else if (clashRules.isEmpty()) {
                            item(key = "rules_empty") {
                                ExpressiveEmptyState(
                                    icon = Icons.Rounded.RuleFolder,
                                    title = stringResource(R.string.dashboard_no_rules),
                                )
                            }
                        } else {
                            item(key = "rules_header") {
                                ExpressiveSectionHeader(
                                    title = "${stringResource(R.string.dashboard_tab_rules)} (${clashRules.size})",
                                )
                            }
                            itemsIndexed(
                                items = clashRules,
                                key = { index, rule -> "${rule.type}_${rule.payload}_${rule.proxy}_$index" },
                            ) { index, rule ->
                                val pos = when {
                                    clashRules.size == 1 -> ExpressiveGroupPosition.SINGLE
                                    index == 0 -> ExpressiveGroupPosition.TOP
                                    index == clashRules.lastIndex -> ExpressiveGroupPosition.BOTTOM
                                    else -> ExpressiveGroupPosition.MIDDLE
                                }
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    ExpressiveSurfaceItem(position = pos) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rule.payload.ifBlank { rule.type },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                if (rule.payload.isNotBlank() && rule.type.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = rule.type,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                            if (rule.proxy.isNotBlank()) {
                                                ExpressivePillBadge(
                                                    text = rule.proxy,
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingTopBar(
            backdrop = topBackdrop,
            title = null,
            actions = {
                IconButton(
                    onClick = onRunUrlTest,
                    enabled = isConnected && !urlTestRunning,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NetworkCheck,
                        contentDescription = stringResource(R.string.connection_test),
                    )
                }
                IconButton(onClick = onOpenLogcat) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = stringResource(R.string.menu_log),
                    )
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun DashboardSubTabBar(
    selectedTab: DashboardTab,
    connectionCount: Int,
    onSelectTab: (DashboardTab) -> Unit,
) {
    val tabs = remember { DashboardTab.entries }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        tabs.forEachIndexed { index, tab ->
            val checked = tab == selectedTab
            val label = when (tab) {
                DashboardTab.OVERVIEW -> stringResource(R.string.dashboard_tab_overview)
                DashboardTab.CONNECTIONS -> if (connectionCount > 0) {
                    "${stringResource(R.string.dashboard_tab_connections)} ($connectionCount)"
                } else {
                    stringResource(R.string.dashboard_tab_connections)
                }
                DashboardTab.RULES -> stringResource(R.string.dashboard_tab_rules)
            }
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = checked,
                onCheckedChange = { if (it) onSelectTab(tab) },
                shapes = shapes,
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ConnectionRowItem(
    item: ClashConnectionItem,
    position: ExpressiveGroupPosition,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    ExpressiveSurfaceItem(position = position) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayTarget,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (item.network.isNotBlank()) {
                        ExpressivePillBadge(
                            text = item.network,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    ExpressivePillBadge(
                        text = item.displayOutbound,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = "↑ ${Formatter.formatFileSize(context, item.upload)} ↓ ${
                            Formatter.formatFileSize(context, item.download)
                        }",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.displayRule.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.displayRule,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.dashboard_close_connection),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun TelemetryChipCard(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ConnectionHeroCard(
    serviceState: BaseService.State,
    serviceModeLabel: String,
    uptimeSeconds: Long,
    activeProfile: ProxyEntity?,
    onToggleConnection: () -> Unit,
    onOpenConfiguration: () -> Unit,
) {
    val isConnected = serviceState == BaseService.State.Connected
    val isTransitioning = serviceState == BaseService.State.Connecting || serviceState == BaseService.State.Stopping

    val containerColor by animateColorAsState(
        targetValue = when {
            isConnected -> MaterialTheme.colorScheme.primaryContainer
            isTransitioning -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "dashboardHeroColor",
    )
    val contentColor = when {
        isConnected -> MaterialTheme.colorScheme.onPrimaryContainer
        isTransitioning -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val secondaryTextColor = if (isConnected || isTransitioning) {
        contentColor.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusTitle = when (serviceState) {
        BaseService.State.Connected -> stringResource(R.string.dashboard_connection)
        BaseService.State.Connecting -> stringResource(R.string.connecting)
        BaseService.State.Stopping -> stringResource(R.string.stopping)
        else -> stringResource(R.string.not_connected)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        tonalElevation = if (isConnected) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExpressivePillBadge(
                            text = serviceModeLabel,
                            containerColor = if (isConnected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (isConnected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                        if (isConnected) {
                            Text(
                                text = formatUptime(uptimeSeconds),
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                color = secondaryTextColor,
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isConnected) {
                            activeProfile?.displayName() ?: statusTitle
                        } else {
                            statusTitle
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            isConnected -> activeProfile?.let { "${it.displayType()} • ${it.displayAddress()}" }
                                ?: stringResource(R.string.vpn_connected)
                            activeProfile != null -> "${activeProfile.displayName()} • ${activeProfile.displayType()}"
                            else -> stringResource(R.string.dashboard_tap_select_profile)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onOpenConfiguration),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                FloatingActionButton(
                    onClick = onToggleConnection,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                    ),
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
                    modifier = Modifier.size(60.dp),
                ) {
                    if (isTransitioning) {
                        LoadingIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = if (isConnected) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = if (isConnected) {
                                stringResource(R.string.stop)
                            } else {
                                stringResource(R.string.connect)
                            },
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedMetricCard(
    label: String,
    rateText: String,
    totalText: String,
    icon: ImageVector,
    badgeContainerColor: Color,
    badgeContentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(badgeContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeContentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = rateText,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = totalText,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrafficSparklineCard(
    rxHistory: List<Long>,
    txHistory: List<Long>,
    showDirectSpeed: Boolean,
    directTxRate: Long,
    directRxRate: Long,
) {
    val context = LocalContext.current
    val rxColor = MaterialTheme.colorScheme.primary
    val txColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    val peakRate = remember(rxHistory, txHistory) {
        maxOf(
            rxHistory.maxOrNull() ?: 0L,
            txHistory.maxOrNull() ?: 0L,
            1024L,
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LegendDot(color = rxColor, label = stringResource(R.string.dashboard_download))
                    LegendDot(color = txColor, label = stringResource(R.string.dashboard_upload))
                }
                Text(
                    text = "${Formatter.formatFileSize(context, peakRate)}/s",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
            ) {
                val width = size.width
                val height = size.height
                if (width <= 0f || height <= 0f) return@Canvas

                drawLine(
                    color = gridColor,
                    start = Offset(0f, height * 0.5f),
                    end = Offset(width, height * 0.5f),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height),
                    end = Offset(width, height),
                    strokeWidth = 1.dp.toPx(),
                )

                fun drawSeries(samples: List<Long>, color: Color, fillAlpha: Float) {
                    if (samples.size < 2) return
                    val stepX = width / (samples.size - 1).coerceAtLeast(1)
                    val maxVal = peakRate.toFloat().coerceAtLeast(1f)
                    val linePath = Path()
                    val fillPath = Path()

                    samples.forEachIndexed { index, value ->
                        val x = index * stepX
                        val normalized = (value.toFloat() / maxVal).coerceIn(0f, 1f)
                        val y = height - (normalized * (height - 6.dp.toPx())) - 3.dp.toPx()
                        if (index == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevNorm = (samples[index - 1].toFloat() / maxVal).coerceIn(0f, 1f)
                            val prevY = height - (prevNorm * (height - 6.dp.toPx())) - 3.dp.toPx()
                            val midX = (prevX + x) / 2f
                            linePath.cubicTo(midX, prevY, midX, y, x, y)
                            fillPath.cubicTo(midX, prevY, midX, y, x, y)
                        }
                    }
                    fillPath.lineTo(width, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = fillAlpha), Color.Transparent),
                        ),
                    )
                    drawPath(
                        path = linePath,
                        color = color,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                drawSeries(txHistory, txColor, fillAlpha = 0.14f)
                drawSeries(rxHistory, rxColor, fillAlpha = 0.22f)
            }

            if (showDirectSpeed) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_direct),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "↑ ${Formatter.formatFileSize(context, directTxRate)}/s  •  ↓ ${
                            Formatter.formatFileSize(context, directRxRate)
                        }/s",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OverviewStatTile(
    icon: ImageVector,
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatUptime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}
