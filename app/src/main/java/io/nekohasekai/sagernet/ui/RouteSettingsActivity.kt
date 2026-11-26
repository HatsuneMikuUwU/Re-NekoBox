package io.nekohasekai.sagernet.ui

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.RuleFolder
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.RuleEntity
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.PrefActionRow
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.ui.components.StoreEditTextPref
import io.nekohasekai.sagernet.ui.components.StoreListPref
import io.nekohasekai.sagernet.ui.components.StoreListPrefRaw
import io.nekohasekai.sagernet.ui.components.rememberComposePreferenceStore
import io.nekohasekai.sagernet.ui.profile.ConfigEditActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

        setComposeContent {
            val context = LocalContext.current
            var loaded by remember { mutableStateOf(false) }
            val store = rememberComposePreferenceStore(DataStore.profileCacheStore, markProfileDirty = true)

            LaunchedEffect(editingId) {
                withContext(Dispatchers.IO) {
                    val entity = if (editingId == 0L) {
                        RuleEntity().apply {
                            if (!pkgName.isNullOrBlank()) packages = setOf(pkgName)
                        }
                    } else {
                        SagerDatabase.rulesDao.getById(editingId) ?: RuleEntity()
                    }
                    DataStore.routeName = entity.name
                    DataStore.routeDomain = entity.domains
                    DataStore.routeIP = entity.ip
                    DataStore.routePort = entity.port
                    DataStore.routeSourcePort = entity.sourcePort
                    DataStore.routeNetwork = entity.network
                    DataStore.routeSource = entity.source
                    DataStore.routeProtocol = entity.protocol
                    DataStore.routeRuleset = entity.ruleset
                    DataStore.routeOutbound = when (entity.outbound) {
                        0L -> 0
                        -1L -> 1
                        -2L -> 2
                        else -> 3
                    }
                    DataStore.routeOutboundRule = entity.outbound
                    DataStore.routePackages = entity.packages.joinToString("\n")
                    DataStore.serverConfig = entity.config
                    DataStore.dirty = false
                }
                loaded = true
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.menu_route), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            if (editingId != 0L) {
                                IconButton(onClick = {
                                    runOnDefaultDispatcher {
                                        ProfileManager.deleteRule(editingId)
                                        onMainDispatcher { finish() }
                                    }
                                }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                            IconButton(onClick = {
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
                                    entity.outbound = when (DataStore.routeOutbound) {
                                        0 -> 0L
                                        1 -> -1L
                                        2 -> -2L
                                        else -> DataStore.routeOutboundRule
                                    }
                                    if (editingId == 0L) {
                                        ProfileManager.createRule(entity)
                                    } else {
                                        ProfileManager.updateRule(entity)
                                    }
                                    onMainDispatcher { finish() }
                                }
                            }) {
                                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.apply))
                            }
                        },
                    )
                },
            ) { padding ->
                if (loaded) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(vertical = 8.dp),
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
                                    summary = if (store.getString(Key.SERVER_CONFIG).isBlank()) stringResource(R.string.not_set) else "JSON Configured",
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
                                PrefActionRow(
                                    title = stringResource(R.string.apps),
                                    summary = store.getString(Key.ROUTE_PACKAGES)
                                        .lineSequence().filter { it.isNotBlank() }.count()
                                        .let { if (it == 0) stringResource(R.string.not_set) else "$it apps" },
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
                                    titleRes = R.string.route_ip,
                                    multiline = true,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_PORT,
                                    titleRes = R.string.route_port,
                                    multiline = true,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_SOURCE,
                                    titleRes = R.string.route_source,
                                    multiline = true,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_SOURCE_PORT,
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
                                    titleRes = R.string.route_network,
                                    entriesRes = R.array.route_protocol_entry,
                                    entryValuesRes = R.array.route_protocol_value,
                                )
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.ROUTE_PROTOCOL,
                                    titleRes = R.string.route_protocol,
                                    multiline = true,
                                )
                                StoreListPrefRaw(
                                    store = store,
                                    key = Key.ROUTE_OUTBOUND,
                                    title = stringResource(R.string.route_outbound),
                                    entries = listOf(
                                        stringResource(R.string.route_proxy),
                                        stringResource(R.string.route_bypass),
                                        stringResource(R.string.route_block),
                                    ),
                                    entryValues = listOf("0", "1", "2"),
                                    defaultValue = "0",
                                    icon = Icons.Rounded.Router,
                                    position = ExpressiveGroupPosition.BOTTOM,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
