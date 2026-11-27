package io.nekohasekai.sagernet.ui

import io.nekohasekai.sagernet.ui.components.hapticClick

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.Http
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.RuleFolder
import androidx.compose.material.icons.rounded.SettingsInputComponent
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.RuleEntity
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.components.AppDialog
import io.nekohasekai.sagernet.ui.components.DialogCancelButton
import io.nekohasekai.sagernet.ui.components.DialogConfirmButton
import io.nekohasekai.sagernet.ui.components.DialogDestructiveButton
import io.nekohasekai.sagernet.ui.components.DialogOptionRow
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.PrefActionRow
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.ui.components.StoreEditTextPref
import io.nekohasekai.sagernet.ui.components.StoreListPref
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.rememberComposePreferenceStore
import io.nekohasekai.sagernet.ui.components.rememberFabExpanded
import io.nekohasekai.sagernet.ui.profile.ConfigEditActivity
import androidx.compose.foundation.shape.RoundedCornerShape

class RouteSettingsActivity : ThemedActivity() {

    companion object {
        const val EXTRA_ROUTE_ID = "id"
        const val EXTRA_PACKAGE_NAME = "pkg"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editingId = intent.getLongExtra(EXTRA_ROUTE_ID, 0L)
        val pkgName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        DataStore.editingId = editingId

        val initialEntity = if (editingId == 0L) {
            RuleEntity().apply {
                if (!pkgName.isNullOrBlank()) packages = setOf(pkgName)
            }
        } else {
            SagerDatabase.rulesDao.getById(editingId) ?: RuleEntity()
        }
        DataStore.routeName = initialEntity.name
        DataStore.routeDomain = initialEntity.domains
        DataStore.routeIP = initialEntity.ip
        DataStore.routePort = initialEntity.port
        DataStore.routeSourcePort = initialEntity.sourcePort
        DataStore.routeNetwork = initialEntity.network
        DataStore.routeSource = initialEntity.source
        DataStore.routeProtocol = initialEntity.protocol
        DataStore.routeRuleset = initialEntity.ruleset
        val initialOutboundMode = when (initialEntity.outbound) {
            0L -> 0
            -1L -> 1
            -2L -> 2
            else -> 3
        }
        DataStore.routeOutbound = initialOutboundMode
        DataStore.routeOutboundRule = initialEntity.outbound
        DataStore.routePackages = initialEntity.packages.joinToString("\n")
        DataStore.serverConfig = initialEntity.config
        DataStore.dirty = false

        setComposeContent {
            val context = LocalContext.current
            val store = rememberComposePreferenceStore(DataStore.profileCacheStore, markProfileDirty = true)

            var outboundMode by remember { mutableIntStateOf(initialOutboundMode) }
            var outboundProfileId by remember { mutableLongStateOf(initialEntity.outbound) }
            var showOutboundDialog by remember { mutableStateOf(false) }
            var showUnsavedDialog by remember { mutableStateOf(false) }
            var showDeleteDialog by remember { mutableStateOf(false) }

            val selectProfileLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val id = result.data?.getLongExtra(ProfileSelectActivity.EXTRA_PROFILE_ID, 0L) ?: 0L
                    if (id > 0L) {
                        outboundMode = 3
                        outboundProfileId = id
                        DataStore.routeOutbound = 3
                        DataStore.routeOutboundRule = id
                        DataStore.dirty = true
                    }
                }
            }

            val outboundSummary = remember(outboundMode, outboundProfileId) {
                when (outboundMode) {
                    0 -> context.getString(R.string.route_proxy)
                    1 -> context.getString(R.string.route_bypass)
                    2 -> context.getString(R.string.route_block)
                    else -> ProfileManager.getProfile(outboundProfileId)?.displayName()
                        ?: context.getString(R.string.select_profile)
                }
            }

            val backdrop = rememberBlurBackdrop()
            val topInset = floatingTopBarSpace()
            val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val listState = rememberLazyListState()
            val fabExpanded = rememberFabExpanded(listState)

            fun saveRoute() {
                runOnDefaultDispatcher {
                    val entity = if (editingId == 0L) {
                        RuleEntity(enabled = true)
                    } else {
                        SagerDatabase.rulesDao.getById(editingId) ?: RuleEntity(enabled = true)
                    }
                    entity.name = DataStore.routeName
                    entity.domains = DataStore.routeDomain
                    entity.ip = DataStore.routeIP
                    entity.port = DataStore.routePort
                    entity.sourcePort = DataStore.routeSourcePort
                    entity.network = DataStore.routeNetwork
                    entity.source = DataStore.routeSource
                    entity.protocol = DataStore.routeProtocol
                    entity.ruleset = DataStore.routeRuleset
                    entity.packages = DataStore.routePackages.lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                    entity.config = DataStore.serverConfig
                    entity.outbound = when (outboundMode) {
                        0 -> 0L
                        1 -> -1L
                        2 -> -2L
                        else -> outboundProfileId
                    }
                    if (editingId == 0L) {
                        ProfileManager.createRule(entity)
                    } else {
                        ProfileManager.updateRule(entity)
                    }
                    onMainDispatcher { finish() }
                }
            }

            BackHandler(enabled = DataStore.dirty) {
                showUnsavedDialog = true
            }

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
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = topInset + 6.dp,
                            bottom = navBarInset + 88.dp,
                        ),
                    ) {
                        item {
                            PrefCategory(titleRes = R.string.route_name) {
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_NAME,
                                    titleRes = R.string.route_name,
                                    icon = Icons.Rounded.Title,
                                    position = ExpressiveGroupPosition.TOP,
                                )
                                PrefActionRow(
                                    title = stringResource(R.string.custom_config),
                                    summary = store.getString(Key.SERVER_CONFIG, "").ifBlank { stringResource(R.string.not_set) },
                                    icon = Icons.Rounded.Layers,
                                    position = ExpressiveGroupPosition.BOTTOM,
                                    onClick = {
                                        context.startActivity(
                                            Intent(context, ConfigEditActivity::class.java).apply {
                                                putExtra("key", Key.SERVER_CONFIG)
                                            }
                                        )
                                    },
                                )
                            }
                        }

                        item {
                            PrefCategory(titleRes = R.string.cag_route) {
                                val pkgCount = store.getString(Key.ROUTE_PACKAGES, "")
                                    .lineSequence().count { it.isNotBlank() }
                                PrefActionRow(
                                    title = stringResource(R.string.apps),
                                    summary = if (pkgCount == 0) stringResource(R.string.not_set) else "$pkgCount selected",
                                    icon = Icons.Rounded.Apps,
                                    position = ExpressiveGroupPosition.TOP,
                                    onClick = {
                                        context.startActivity(Intent(context, AppListActivity::class.java))
                                    },
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_DOMAIN,
                                    titleRes = R.string.route_domain,
                                    icon = Icons.Rounded.Domain,
                                    multiline = true,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_IP,
                                    icon = Icons.Rounded.LocationOn,
                                    titleRes = R.string.route_ip,
                                    multiline = true,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_PORT,
                                    icon = Icons.Rounded.SettingsInputComponent,
                                    titleRes = R.string.route_port,
                                    multiline = true,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_SOURCE,
                                    icon = Icons.Rounded.MyLocation,
                                    titleRes = R.string.route_source,
                                    multiline = true,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_SOURCE_PORT,
                                    icon = Icons.AutoMirrored.Rounded.Input,
                                    titleRes = R.string.route_source_port,
                                    multiline = true,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_RULESET,
                                    titleRes = R.string.route_ruleset,
                                    icon = Icons.Rounded.RuleFolder,
                                    multiline = true,
                                )
                                StoreListPref(
                                    store = store,
                                    key = Key.ROUTE_NETWORK,
                                    icon = Icons.Rounded.Lan,
                                    titleRes = R.string.route_network,
                                    entriesRes = R.array.route_protocol_entry,
                                    entryValuesRes = R.array.route_protocol_value,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_PROTOCOL,
                                    icon = Icons.Rounded.Http,
                                    titleRes = R.string.route_protocol,
                                )
                                PrefActionRow(
                                    title = stringResource(R.string.route_outbound),
                                    summary = outboundSummary,
                                    icon = Icons.Rounded.Router,
                                    position = ExpressiveGroupPosition.BOTTOM,
                                    onClick = { showOutboundDialog = true },
                                )
                            }
                        }
                    }
                }

                FloatingTopBar(
                    backdrop = backdrop,
                    title = stringResource(R.string.menu_route),
                    onBack = { if (DataStore.dirty) showUnsavedDialog = true else finish() },
                    actions = if (editingId != 0L) {
                        {
                            IconButton(onClick = hapticClick { showDeleteDialog = true }) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    } else null,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                ExtendedFloatingActionButton(
                    onClick = hapticClick { saveRoute() },
                    expanded = fabExpanded,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.apply)) },
                    text = { Text(stringResource(R.string.apply)) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )
            }

            if (showOutboundDialog) {
                AppDialog(
                    icon = Icons.Rounded.Router,
                    title = stringResource(R.string.route_outbound),
                    onDismiss = { showOutboundDialog = false },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf(
                                0 to stringResource(R.string.route_proxy),
                                1 to stringResource(R.string.route_bypass),
                                2 to stringResource(R.string.route_block),
                            ).forEach { (mode, label) ->
                                DialogOptionRow(
                                    label = label,
                                    selected = outboundMode == mode,
                                    onClick = {
                                        outboundMode = mode
                                        DataStore.routeOutbound = mode
                                        DataStore.dirty = true
                                        showOutboundDialog = false
                                    },
                                )
                            }
                            DialogOptionRow(
                                label = stringResource(R.string.select_profile),
                                selected = outboundMode == 3,
                                onClick = {
                                    showOutboundDialog = false
                                    selectProfileLauncher.launch(Intent(context, ProfileSelectActivity::class.java))
                                },
                            )
                        }
                    },
                    confirmButton = {
                        DialogCancelButton(stringResource(android.R.string.cancel)) {
                            showOutboundDialog = false
                        }
                    },
                )
            }

            if (showUnsavedDialog) {
                AppDialog(
                    icon = Icons.Rounded.WarningAmber,
                    title = stringResource(R.string.unsaved_changes_prompt),
                    onDismiss = { showUnsavedDialog = false },
                    confirmButton = {
                        DialogConfirmButton(stringResource(R.string.yes)) {
                            showUnsavedDialog = false
                            saveRoute()
                        }
                    },
                    dismissButton = {
                        DialogCancelButton(stringResource(R.string.no)) {
                            showUnsavedDialog = false
                            finish()
                        }
                    },
                )
            }

            if (showDeleteDialog) {
                AppDialog(
                    icon = Icons.Rounded.Delete,
                    title = stringResource(R.string.delete_route_prompt),
                    onDismiss = { showDeleteDialog = false },
                    confirmButton = {
                        DialogDestructiveButton(stringResource(R.string.yes)) {
                            showDeleteDialog = false
                            runOnDefaultDispatcher {
                                ProfileManager.deleteRule(editingId)
                                onMainDispatcher { finish() }
                            }
                        }
                    },
                    dismissButton = {
                        DialogCancelButton(stringResource(R.string.no)) {
                            showDeleteDialog = false
                        }
                    },
                )
            }
        }
    }
}
