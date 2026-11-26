package io.nekohasekai.sagernet.ui

import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Update
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.database.SubscriptionBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.ui.components.StoreEditTextPref
import io.nekohasekai.sagernet.ui.components.StoreListPref
import io.nekohasekai.sagernet.ui.components.StoreSwitchPref
import io.nekohasekai.sagernet.ui.components.rememberComposePreferenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GroupSettingsActivity : ThemedActivity() {

    companion object {
        const val EXTRA_GROUP_ID = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editingId = intent.getLongExtra(EXTRA_GROUP_ID, 0L)
        DataStore.editingId = editingId

        setComposeContent {
            var loaded by remember { mutableStateOf(false) }
            val store = rememberComposePreferenceStore(DataStore.profileCacheStore, markProfileDirty = true)

            LaunchedEffect(editingId) {
                withContext(Dispatchers.IO) {
                    val entity = if (editingId == 0L) {
                        ProxyGroup().applyDefaultValues()
                    } else {
                        SagerDatabase.groupDao.getById(editingId) ?: ProxyGroup().applyDefaultValues()
                    }
                    DataStore.groupName = entity.name ?: ""
                    DataStore.groupType = entity.type
                    DataStore.groupOrder = entity.order
                    DataStore.groupIsSelector = entity.isSelector
                    val sub = entity.subscription ?: SubscriptionBean().applyDefaultValues()
                    DataStore.subscriptionLink = sub.link ?: ""
                    DataStore.subscriptionForceResolve = sub.forceResolve ?: false
                    DataStore.subscriptionDeduplication = sub.deduplication ?: false
                    DataStore.subscriptionUpdateWhenConnectedOnly = sub.updateWhenConnectedOnly ?: false
                    DataStore.subscriptionUserAgent = sub.customUserAgent ?: ""
                    DataStore.subscriptionAutoUpdate = sub.autoUpdate ?: false
                    DataStore.subscriptionAutoUpdateDelay = sub.autoUpdateDelay ?: 1440
                    DataStore.dirty = false
                }
                loaded = true
            }

            val groupType = store.getString(Key.GROUP_TYPE, "0").toIntOrNull() ?: 0

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.group_settings), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            if (editingId != 0L) {
                                IconButton(onClick = {
                                    runOnDefaultDispatcher {
                                        GroupManager.deleteGroup(editingId)
                                        onMainDispatcher { finish() }
                                    }
                                }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                            IconButton(onClick = {
                                runOnDefaultDispatcher {
                                    val entity = if (editingId == 0L) {
                                        ProxyGroup().applyDefaultValues()
                                    } else {
                                        SagerDatabase.groupDao.getById(editingId) ?: ProxyGroup().applyDefaultValues()
                                    }
                                    entity.name = DataStore.groupName.ifBlank { "Group" }
                                    entity.type = DataStore.groupType
                                    entity.order = DataStore.groupOrder
                                    entity.isSelector = DataStore.groupIsSelector
                                    if (entity.type == GroupType.SUBSCRIPTION) {
                                        val sub = (entity.subscription ?: SubscriptionBean().applyDefaultValues()).apply {
                                            link = DataStore.subscriptionLink
                                            forceResolve = DataStore.subscriptionForceResolve
                                            deduplication = DataStore.subscriptionDeduplication
                                            updateWhenConnectedOnly = DataStore.subscriptionUpdateWhenConnectedOnly
                                            customUserAgent = DataStore.subscriptionUserAgent
                                            autoUpdate = DataStore.subscriptionAutoUpdate
                                            autoUpdateDelay = DataStore.subscriptionAutoUpdateDelay
                                        }
                                        entity.subscription = sub
                                    }
                                    if (editingId == 0L) {
                                        GroupManager.createGroup(entity)
                                    } else {
                                        GroupManager.updateGroup(entity)
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
                            PrefCategory(titleRes = R.string.group_settings) {
                                StoreEditTextPref(
                                    store = store,
                                    key = Key.GROUP_NAME,
                                    titleRes = R.string.group_name,
                                    icon = Icons.Rounded.Title,
                                    position = ExpressiveGroupPosition.TOP,
                                )
                                StoreListPref(
                                    store = store,
                                    key = Key.GROUP_TYPE,
                                    titleRes = R.string.group_type,
                                    entriesRes = R.array.group_types,
                                    entryValuesRes = R.array.int_array_2,
                                    defaultValue = "0",
                                    icon = Icons.Rounded.Layers,
                                )
                                StoreListPref(
                                    store = store,
                                    key = Key.GROUP_ORDER,
                                    titleRes = R.string.group_order,
                                    entriesRes = R.array.group_orders,
                                    entryValuesRes = R.array.int_array_3,
                                    defaultValue = "0",
                                    icon = Icons.AutoMirrored.Rounded.Sort,
                                )
                                StoreSwitchPref(
                                    store = store,
                                    key = Key.GROUP_IS_SELECTOR,
                                    titleRes = R.string.use_selector,
                                    position = ExpressiveGroupPosition.BOTTOM,
                                )
                            }
                        }

                        if (groupType == GroupType.SUBSCRIPTION) {
                            item {
                                PrefCategory(titleRes = R.string.subscription_settings) {
                                    StoreEditTextPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_LINK,
                                        titleRes = R.string.group_subscription_link,
                                        icon = Icons.Rounded.Link,
                                        multiline = true,
                                        position = ExpressiveGroupPosition.TOP,
                                    )
                                    StoreEditTextPref(
                                        store = store,
                                        key = "subscriptionServerDns",
                                        titleRes = R.string.server_dns,
                                        icon = Icons.Rounded.Dns,
                                    )
                                    StoreSwitchPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_FORCE_RESOLVE,
                                        titleRes = R.string.force_resolve,
                                        summaryRes = R.string.force_resolve_sum,
                                    )
                                    StoreSwitchPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_DEDUPLICATION,
                                        titleRes = R.string.deduplication,
                                        summaryRes = R.string.deduplication_sum,
                                    )
                                    StoreListPref(
                                        store = store,
                                        key = "subscriptionFilterMode",
                                        titleRes = R.string.filter,
                                        entriesRes = R.array.filter_modes,
                                        entryValuesRes = R.array.int_array_3,
                                        defaultValue = "0",
                                        icon = Icons.Rounded.FilterList,
                                    )
                                    StoreEditTextPref(
                                        store = store,
                                        key = "subscriptionFilterRegex",
                                        titleRes = R.string.filter_regex,
                                        position = ExpressiveGroupPosition.BOTTOM,
                                    )
                                }
                            }

                            item {
                                PrefCategory(titleRes = R.string.update_settings) {
                                    StoreSwitchPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_UPDATE_WHEN_CONNECTED_ONLY,
                                        titleRes = R.string.update_when_connected_only,
                                        summaryRes = R.string.update_when_connected_only_sum,
                                        position = ExpressiveGroupPosition.TOP,
                                    )
                                    StoreEditTextPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_USER_AGENT,
                                        titleRes = R.string.subscription_user_agent,
                                    )
                                    StoreSwitchPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_AUTO_UPDATE,
                                        titleRes = R.string.auto_update,
                                        icon = Icons.Rounded.Update,
                                    )
                                    StoreEditTextPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_AUTO_UPDATE_DELAY,
                                        titleRes = R.string.auto_update_delay,
                                        defaultValue = "1440",
                                        isNumber = true,
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
}
