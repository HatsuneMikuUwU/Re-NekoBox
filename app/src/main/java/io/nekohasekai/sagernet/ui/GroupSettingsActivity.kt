package io.nekohasekai.sagernet.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.rememberFabExpanded
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.ui.components.StoreEditTextPref
import io.nekohasekai.sagernet.ui.components.StoreListPref
import io.nekohasekai.sagernet.ui.components.StoreSwitchPref
import io.nekohasekai.sagernet.ui.components.rememberComposePreferenceStore
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.TravelExplore

class GroupSettingsActivity : ThemedActivity() {

    companion object {
        const val EXTRA_GROUP_ID = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editingId = intent.getLongExtra(EXTRA_GROUP_ID, 0L)
        DataStore.editingId = editingId

        val initialEntity = if (editingId == 0L) {
            ProxyGroup().applyDefaultValues()
        } else {
            SagerDatabase.groupDao.getById(editingId) ?: ProxyGroup().applyDefaultValues()
        }
        DataStore.groupName = initialEntity.name ?: ""
        DataStore.groupType = initialEntity.type
        DataStore.groupOrder = initialEntity.order
        DataStore.groupIsSelector = initialEntity.isSelector
        val initialSub = initialEntity.subscription ?: SubscriptionBean().applyDefaultValues()
        DataStore.subscriptionLink = initialSub.link ?: ""
        DataStore.subscriptionForceResolve = initialSub.forceResolve ?: false
        DataStore.subscriptionDeduplication = initialSub.deduplication ?: false
        DataStore.subscriptionUpdateWhenConnectedOnly = initialSub.updateWhenConnectedOnly ?: false
        DataStore.subscriptionUserAgent = initialSub.customUserAgent ?: ""
        DataStore.subscriptionAutoUpdate = initialSub.autoUpdate ?: false
        DataStore.subscriptionAutoUpdateDelay = initialSub.autoUpdateDelay ?: 1440
        DataStore.dirty = false

        setComposeContent {
            val store = rememberComposePreferenceStore(DataStore.profileCacheStore, markProfileDirty = true)

            val groupType = store.getString(Key.GROUP_TYPE, "0").toIntOrNull() ?: 0

            val backdrop = rememberBlurBackdrop()
            val topInset = floatingTopBarSpace()
            val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val listState = rememberLazyListState()
            val fabExpanded = rememberFabExpanded(listState)

            fun saveGroup() {
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
                                    icon = Icons.Rounded.Checklist,
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
                                        icon = Icons.Rounded.TravelExplore,
                                        titleRes = R.string.force_resolve,
                                        summaryRes = R.string.force_resolve_sum,
                                    )
                                    StoreSwitchPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_DEDUPLICATION,
                                        icon = Icons.Rounded.LayersClear,
                                        titleRes = R.string.deduplication,
                                        summaryRes = R.string.deduplication_sum,
                                    )
                                    val filterMode = store.getString("subscriptionFilterMode", "0").toIntOrNull() ?: 0
                                    val showFilterRegex = filterMode != 0
                                    StoreListPref(
                                        store = store,
                                        key = "subscriptionFilterMode",
                                        titleRes = R.string.filter,
                                        entriesRes = R.array.filter_modes,
                                        entryValuesRes = R.array.int_array_3,
                                        defaultValue = "0",
                                        icon = Icons.Rounded.FilterList,
                                        position = if (showFilterRegex) ExpressiveGroupPosition.MIDDLE else ExpressiveGroupPosition.BOTTOM,
                                    )
                                    if (showFilterRegex) {
                                        StoreEditTextPref(
                                            store = store,
                                            key = "subscriptionFilterRegex",
                                            icon = Icons.Rounded.FilterAlt,
                                            titleRes = R.string.filter_regex,
                                            position = ExpressiveGroupPosition.BOTTOM,
                                        )
                                    }
                                }
                            }

                            item {
                                PrefCategory(titleRes = R.string.update_settings) {
                                    StoreSwitchPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_UPDATE_WHEN_CONNECTED_ONLY,
                                        icon = Icons.Rounded.VpnLock,
                                        titleRes = R.string.update_when_connected_only,
                                        summaryRes = R.string.update_when_connected_only_sum,
                                        position = ExpressiveGroupPosition.TOP,
                                    )
                                    StoreEditTextPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_USER_AGENT,
                                        icon = Icons.Rounded.Devices,
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
                                        icon = Icons.Rounded.Timer,
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

                FloatingTopBar(
                    backdrop = backdrop,
                    title = stringResource(R.string.group_settings),
                    onBack = { finish() },
                    actions = if (editingId != 0L) {
                        {
                            IconButton(onClick = {
                                runOnDefaultDispatcher {
                                    GroupManager.deleteGroup(editingId)
                                    onMainDispatcher { finish() }
                                }
                            }) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    } else null,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                ExtendedFloatingActionButton(
                    onClick = { saveGroup() },
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
        }
    }
}
