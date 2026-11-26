package io.nekohasekai.sagernet.ui.screens

import android.content.Intent
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.GroupOrder
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.aidl.TrafficData
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.parseProxies
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.ScannerActivity
import io.nekohasekai.sagernet.ui.components.ExpressiveEmptyState
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressivePillBadge
import io.nekohasekai.sagernet.ui.components.ExpressiveSearchField
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import io.nekohasekai.sagernet.ui.components.QrCodeDialog
import io.nekohasekai.sagernet.ui.profile.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.SystemClock
import java.net.InetSocketAddress
import java.net.Socket
import moe.matsuri.nb4a.proxy.anytls.AnyTLSSettingsActivity
import moe.matsuri.nb4a.proxy.config.ConfigSettingActivity
import moe.matsuri.nb4a.proxy.shadowtls.ShadowTLSSettingsActivity

@Composable
fun ConfigurationScreen(
    onOpenDrawer: () -> Unit,
    refreshSignal: Int,
    onShowMessage: (String) -> Unit,
    selectMode: Boolean = false,
    onProfileSelected: ((ProxyEntity) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var groups by remember { mutableStateOf<List<ProxyGroup>>(emptyList()) }
    var selectedGroupId by remember { mutableLongStateOf(DataStore.selectedGroup) }
    var profiles by remember { mutableStateOf<List<ProxyEntity>>(emptyList()) }
    var selectedProxyId by remember { mutableLongStateOf(DataStore.selectedProxy) }

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isTestingAll by remember { mutableStateOf(false) }

    var qrDialogData by remember { mutableStateOf<Pair<String, String>?>(null) }
    var deleteConfirmEntity by remember { mutableStateOf<ProxyEntity?>(null) }

    fun reloadData() {
        scope.launch {
            val loadedGroups = withContext(Dispatchers.IO) {
                var all = SagerDatabase.groupDao.allGroups()
                if (all.isEmpty()) {
                    SagerDatabase.groupDao.createGroup(ProxyGroup(ungrouped = true))
                    all = SagerDatabase.groupDao.allGroups()
                }
                all
            }
            groups = loadedGroups
            val activeGroup = loadedGroups.firstOrNull { it.id == selectedGroupId }
                ?: loadedGroups.firstOrNull()
            if (activeGroup != null) {
                selectedGroupId = activeGroup.id
                DataStore.selectedGroup = activeGroup.id
                val loadedProfiles = withContext(Dispatchers.IO) {
                    val list = SagerDatabase.proxyDao.getByGroup(activeGroup.id)
                    when (activeGroup.order) {
                        GroupOrder.BY_NAME -> list.sortedBy { it.displayName().lowercase() }
                        GroupOrder.BY_DELAY -> list.sortedBy { if (it.status == 1) it.ping else Int.MAX_VALUE }
                        else -> list.sortedBy { it.userOrder }
                    }
                }
                profiles = loadedProfiles
            }
            selectedProxyId = DataStore.selectedProxy
        }
    }

    fun runPingAll() {
        if (isTestingAll || profiles.isEmpty()) return
        isTestingAll = true
        runOnDefaultDispatcher {
            profiles.forEach { entity ->
                val bean = entity.requireBean()
                val rtt = runCatching {
                    val start = SystemClock.elapsedRealtime()
                    Socket().use { sock ->
                        sock.connect(InetSocketAddress(bean.serverAddress, bean.serverPort), 3000)
                    }
                    (SystemClock.elapsedRealtime() - start).toInt().coerceAtLeast(1)
                }.getOrDefault(-1)
                if (rtt >= 0) {
                    entity.status = 1
                    entity.ping = rtt
                    entity.error = null
                } else {
                    entity.status = 2
                    entity.ping = 0
                    entity.error = "timeout"
                }
                SagerDatabase.proxyDao.updateProxy(entity)
            }
            onMainDispatcher {
                isTestingAll = false
                reloadData()
                onShowMessage(context.getString(R.string.connection_test_finished))
            }
        }
    }

    LaunchedEffect(refreshSignal, selectedGroupId) { reloadData() }

    DisposableEffect(Unit) {
        val profileListener = object : ProfileManager.Listener {
            override suspend fun onAdd(profile: ProxyEntity) = reloadData()
            override suspend fun onUpdated(data: List<TrafficData>) = reloadData()
            override suspend fun onUpdated(profile: ProxyEntity, noTraffic: Boolean) = reloadData()
            override suspend fun onRemoved(groupId: Long, profileId: Long) = reloadData()
        }
        val groupListener = object : GroupManager.Listener {
            override suspend fun groupAdd(group: ProxyGroup) = reloadData()
            override suspend fun groupUpdated(group: ProxyGroup) = reloadData()
            override suspend fun groupRemoved(groupId: Long) = reloadData()
            override suspend fun groupUpdated(groupId: Long) = reloadData()
        }
        ProfileManager.addListener(profileListener)
        GroupManager.addListener(groupListener)
        onDispose {
            ProfileManager.removeListener(profileListener)
            GroupManager.removeListener(groupListener)
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runOnDefaultDispatcher {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: return@runOnDefaultDispatcher
                    val parsed = parseProxies(text)
                    if (parsed.isEmpty()) {
                        onMainDispatcher { onShowMessage(context.getString(R.string.no_proxies_found)) }
                    } else {
                        val targetGroup = DataStore.selectedGroupForImport()
                        parsed.forEach { ProfileManager.createProfile(targetGroup, it) }
                        onMainDispatcher {
                            onShowMessage(context.resources.getQuantityString(R.plurals.added, parsed.size, parsed.size))
                        }
                    }
                } catch (e: Exception) {
                    onMainDispatcher { onShowMessage(e.readableMessage) }
                }
            }
        }
    }

    val filteredProfiles = remember(profiles, searchQuery) {
        if (searchQuery.isBlank()) profiles
        else profiles.filter {
            it.displayName().contains(searchQuery, ignoreCase = true) ||
                it.displayType().contains(searchQuery, ignoreCase = true) ||
                it.displayAddress().contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = if (selectMode) stringResource(R.string.select_profile) else stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    if (!selectMode) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) searchQuery = ""
                    }) {
                        Icon(Icons.Rounded.Search, contentDescription = stringResource(android.R.string.search_go))
                    }
                    if (!selectMode) {
                        IconButton(onClick = { showAddSheet = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_profile))
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Rounded.Sort, contentDescription = stringResource(R.string.group_order))
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                val currentGroup = groups.firstOrNull { it.id == selectedGroupId }
                                listOf(
                                    GroupOrder.ORIGIN to R.string.group_order_origin,
                                    GroupOrder.BY_NAME to R.string.group_order_by_name,
                                    GroupOrder.BY_DELAY to R.string.group_order_by_delay,
                                ).forEach { (order, labelRes) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(labelRes)) },
                                        onClick = {
                                            showSortMenu = false
                                            if (currentGroup != null) {
                                                runOnDefaultDispatcher {
                                                    currentGroup.order = order
                                                    GroupManager.updateGroup(currentGroup)
                                                    reloadData()
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.connection_test_icmp_ping)) },
                                    onClick = {
                                        showMoreMenu = false
                                        runPingAll()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.connection_test_clear_results)) },
                                    onClick = {
                                        showMoreMenu = false
                                        runOnDefaultDispatcher {
                                            profiles.forEach {
                                                it.status = 0
                                                it.ping = 0
                                                it.error = null
                                            }
                                            SagerDatabase.proxyDao.updateProxy(profiles)
                                            reloadData()
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.connection_test_delete_unavailable)) },
                                    onClick = {
                                        showMoreMenu = false
                                        runOnDefaultDispatcher {
                                            val unavailable = profiles.filter { it.status == 2 || it.status == 3 }
                                            unavailable.forEach { ProfileManager.deleteProfile(it.groupId, it.id) }
                                            onMainDispatcher {
                                                onShowMessage(context.getString(R.string.removed, unavailable.size))
                                            }
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.clear_traffic_statistics)) },
                                    onClick = {
                                        showMoreMenu = false
                                        runOnDefaultDispatcher {
                                            ProfileManager.resetTraffic(profiles.map { it.id }.toLongArray())
                                            reloadData()
                                        }
                                    },
                                )
                            }
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            AnimatedVisibility(visible = searchVisible) {
                ExpressiveSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = stringResource(R.string.search),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            if (groups.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(groups, key = { it.id }) { group ->
                        val selected = group.id == selectedGroupId
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedGroupId = group.id
                                DataStore.selectedGroup = group.id
                            },
                            label = {
                                Text(
                                    text = group.displayName(),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }

            // Expressive quick action strip for active group
            if (!selectMode && profiles.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val reachable = profiles.count { it.status == 1 }
                    Text(
                        text = if (reachable > 0) "${profiles.size} nodes  •  $reachable reachable" else "${profiles.size} nodes",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = { runPingAll() },
                        enabled = !isTestingAll,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        if (isTestingAll) {
                            LoadingIndicator(modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.connection_test_testing), style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ping", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (filteredProfiles.isEmpty()) {
                ExpressiveEmptyState(
                    icon = Icons.Rounded.Router,
                    title = stringResource(R.string.no_proxies_found),
                    subtitle = stringResource(R.string.add_profile),
                    actionLabel = if (!selectMode) stringResource(R.string.add_profile) else null,
                    onAction = if (!selectMode) { { showAddSheet = true } } else null,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    itemsIndexed(filteredProfiles, key = { _, p -> p.id }) { index, entity ->
                        val position = when {
                            filteredProfiles.size == 1 -> ExpressiveGroupPosition.SINGLE
                            index == 0 -> ExpressiveGroupPosition.TOP
                            index == filteredProfiles.lastIndex -> ExpressiveGroupPosition.BOTTOM
                            else -> ExpressiveGroupPosition.MIDDLE
                        }
                        val isSelected = entity.id == selectedProxyId
                        ProfileExpressiveCard(
                            entity = entity,
                            selected = isSelected,
                            position = position,
                            selectMode = selectMode,
                            onSelect = {
                                if (selectMode) {
                                    onProfileSelected?.invoke(entity)
                                } else {
                                    val oldId = DataStore.selectedProxy
                                    DataStore.selectedProxy = entity.id
                                    selectedProxyId = entity.id
                                    if (DataStore.serviceState.canStop && oldId != entity.id) {
                                        SagerNet.reloadService()
                                    }
                                }
                            },
                            onEdit = {
                                context.startActivity(entity.settingIntent(context, false))
                            },
                            onShareQr = {
                                qrDialogData = entity.displayName() to entity.toStdLink()
                            },
                            onCopyLink = {
                                SagerNet.trySetPrimaryClip(entity.toStdLink())
                                onShowMessage(context.getString(R.string.copy_success))
                            },
                            onDuplicate = {
                                runOnDefaultDispatcher {
                                    ProfileManager.createProfile(entity.groupId, entity.requireBean())
                                }
                            },
                            onDelete = {
                                if (DataStore.confirmProfileDelete) {
                                    deleteConfirmEntity = entity
                                } else {
                                    runOnDefaultDispatcher {
                                        ProfileManager.deleteProfile(entity.groupId, entity.id)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_profile),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                val quickActions = listOf(
                    Triple(Icons.Rounded.QrCodeScanner, R.string.add_profile_methods_scan_qr_code) {
                        context.startActivity(Intent(context, ScannerActivity::class.java))
                    },
                    Triple(Icons.Rounded.ContentPaste, R.string.action_import) {
                        val text = SagerNet.getClipboardText()
                        if (text.isBlank()) {
                            onShowMessage(context.getString(R.string.action_import_err))
                        } else {
                            runOnDefaultDispatcher {
                                try {
                                    val parsed = parseProxies(text)
                                    if (parsed.isEmpty()) {
                                        onMainDispatcher { onShowMessage(context.getString(R.string.no_proxies_found)) }
                                    } else {
                                        val target = DataStore.selectedGroupForImport()
                                        parsed.forEach { ProfileManager.createProfile(target, it) }
                                        onMainDispatcher {
                                            onShowMessage(context.resources.getQuantityString(R.plurals.added, parsed.size, parsed.size))
                                        }
                                    }
                                } catch (e: Exception) {
                                    onMainDispatcher { onShowMessage(e.readableMessage) }
                                }
                            }
                        }
                    },
                    Triple(Icons.Rounded.FileOpen, R.string.action_import_file) {
                        importFileLauncher.launch("*/*")
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickActions.forEach { (icon, labelRes, action) ->
                        Button(
                            onClick = {
                                showAddSheet = false
                                action()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.add_profile_methods_manual_settings),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                val protocols = listOf(
                    "SOCKS" to SocksSettingsActivity::class.java,
                    "HTTP" to HttpSettingsActivity::class.java,
                    "Shadowsocks" to ShadowsocksSettingsActivity::class.java,
                    "ShadowsocksR" to ShadowsocksRSettingsActivity::class.java,
                    "VMess / VLESS" to VMessSettingsActivity::class.java,
                    "Trojan" to TrojanSettingsActivity::class.java,
                    "Trojan-Go" to TrojanGoSettingsActivity::class.java,
                    "Hysteria" to HysteriaSettingsActivity::class.java,
                    "TUIC" to TuicSettingsActivity::class.java,
                    "WireGuard" to WireGuardSettingsActivity::class.java,
                    "SSH" to SSHSettingsActivity::class.java,
                    "Naive" to NaiveSettingsActivity::class.java,
                    "Mieru" to MieruSettingsActivity::class.java,
                    "Juicity" to JuicitySettingsActivity::class.java,
                    "Snell" to SnellSettingsActivity::class.java,
                    "ShadowTLS" to ShadowTLSSettingsActivity::class.java,
                    "AnyTLS" to AnyTLSSettingsActivity::class.java,
                    "Chain" to ChainSettingsActivity::class.java,
                    "Custom Config" to ConfigSettingActivity::class.java,
                )

                LazyColumn(
                    modifier = Modifier.height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(protocols) { idx, (name, clazz) ->
                        val pos = when (idx) {
                            0 -> ExpressiveGroupPosition.TOP
                            protocols.lastIndex -> ExpressiveGroupPosition.BOTTOM
                            else -> ExpressiveGroupPosition.MIDDLE
                        }
                        ExpressiveSurfaceItem(
                            position = pos,
                            onClick = {
                                showAddSheet = false
                                context.startActivity(Intent(context, clazz))
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(name, style = MaterialTheme.typography.titleSmall)
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    qrDialogData?.let { (title, link) ->
        QrCodeDialog(
            title = title,
            content = link,
            onCopy = {
                SagerNet.trySetPrimaryClip(link)
                onShowMessage(context.getString(R.string.copy_success))
                qrDialogData = null
            },
            onDismiss = { qrDialogData = null },
        )
    }

    deleteConfirmEntity?.let { entity ->
        AlertDialog(
            onDismissRequest = { deleteConfirmEntity = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.delete_confirm_prompt)) },
            text = { Text(entity.displayName()) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmEntity = null
                        runOnDefaultDispatcher {
                            ProfileManager.deleteProfile(entity.groupId, entity.id)
                        }
                    },
                    shape = CircleShape,
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmEntity = null }) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }
}

@Composable
private fun ProfileExpressiveCard(
    entity: ProxyEntity,
    selected: Boolean,
    position: ExpressiveGroupPosition,
    selectMode: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onShareQr: () -> Unit,
    onCopyLink: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    ExpressiveSurfaceItem(
        position = position,
        selected = selected,
        onClick = onSelect,
        modifier = Modifier.animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Expressive active indicator pill
            if (selected) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.width(10.dp))
            }

            // Protocol monogram badge
            val protoTag = entity.displayType().take(2).uppercase()
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = protoTag,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (DataStore.alwaysShowAddress || selected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entity.displayAddress(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ExpressivePillBadge(
                        text = entity.displayType(),
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )

                    if (entity.status == 1 && entity.ping > 0) {
                        val pingColor = when {
                            entity.ping < 200 -> Color(0xFF2E7D32)
                            entity.ping < 500 -> Color(0xFFEF6C00)
                            else -> MaterialTheme.colorScheme.error
                        }
                        ExpressivePillBadge(
                            text = "${entity.ping} ms",
                            containerColor = pingColor.copy(alpha = 0.14f),
                            contentColor = pingColor,
                        )
                    } else if (entity.status == 2 || entity.status == 3) {
                        ExpressivePillBadge(
                            text = entity.error?.take(16) ?: "Error",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }

                    if (DataStore.profileTrafficStatistics && (entity.tx > 0 || entity.rx > 0)) {
                        val txStr = Formatter.formatFileSize(context, entity.tx)
                        val rxStr = Formatter.formatFileSize(context, entity.rx)
                        Text(
                            text = "↑$txStr ↓$rxStr",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (!selectMode) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_qr_nfc)) },
                            leadingIcon = { Icon(Icons.Rounded.QrCode2, null) },
                            onClick = { menuExpanded = false; onShareQr() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_export_clipboard)) },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                            onClick = { menuExpanded = false; onCopyLink() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.duplicate)) },
                            leadingIcon = { Icon(Icons.Rounded.Share, null) },
                            onClick = { menuExpanded = false; onDuplicate() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}
