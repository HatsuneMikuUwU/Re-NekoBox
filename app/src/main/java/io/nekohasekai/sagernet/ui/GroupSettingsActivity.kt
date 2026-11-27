package io.nekohasekai.sagernet.ui

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
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.database.SubscriptionBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
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
import io.nekohasekai.sagernet.ui.components.StoreSwitchPref
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.rememberComposePreferenceStore
import io.nekohasekai.sagernet.ui.components.rememberFabExpanded
import androidx.compose.foundation.shape.RoundedCornerShape

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
        DataStore.frontProxy = initialEntity.frontProxy
        DataStore.landingProxy = initialEntity.landingProxy
        val initialSub = initialEntity.subscription ?: SubscriptionBean().applyDefaultValues()
        DataStore.subscriptionLink = initialSub.link ?: ""
        DataStore.subscriptionServerDns = initialSub.serverDnsResolver ?: ""
        DataStore.subscriptionForceResolve = initialSub.forceResolve ?: false
        DataStore.subscriptionDeduplication = initialSub.deduplication ?: false
        DataStore.subscriptionFilterMode = initialSub.filterMode ?: 0
        DataStore.subscriptionFilterRegex = initialSub.filterRegex ?: ""
        DataStore.subscriptionUpdateWhenConnectedOnly = initialSub.updateWhenConnectedOnly ?: false
        DataStore.subscriptionUserAgent = initialSub.customUserAgent ?: ""
        DataStore.subscriptionAutoUpdate = initialSub.autoUpdate ?: false
        DataStore.subscriptionAutoUpdateDelay = initialSub.autoUpdateDelay ?: 1440
        DataStore.dirty = false

        setComposeContent {
            val context = LocalContext.current
            val store = rememberComposePreferenceStore(DataStore.profileCacheStore, markProfileDirty = true)

            var frontProxyId by remember { mutableLongStateOf(initialEntity.frontProxy) }
            var landingProxyId by remember { mutableLongStateOf(initialEntity.landingProxy) }
            var showFrontProxyDialog by remember { mutableStateOf(false) }
            var showLandingProxyDialog by remember { mutableStateOf(false) }
            var showUnsavedDialog by remember { mutableStateOf(false) }
            var showDeleteDialog by remember { mutableStateOf(false) }

            val frontProxyLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val id = result.data?.getLongExtra(ProfileSelectActivity.EXTRA_PROFILE_ID, -1L) ?: -1L
                    if (id > 0L) {
                        frontProxyId = id
                        DataStore.frontProxy = id
                        DataStore.dirty = true
                    }
                }
            }
            val landingProxyLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val id = result.data?.getLongExtra(ProfileSelectActivity.EXTRA_PROFILE_ID, -1L) ?: -1L
                    if (id > 0L) {
                        landingProxyId = id
                        DataStore.landingProxy = id
                        DataStore.dirty = true
                    }
                }
            }

            val groupType = store.getString(Key.GROUP_TYPE, "0").toIntOrNull() ?: 0
            val autoUpdateEnabled = store.getBoolean(Key.SUBSCRIPTION_AUTO_UPDATE, false)

            val frontProxySummary = remember(frontProxyId) {
                if (frontProxyId > 0L) {
                    ProfileManager.getProfile(frontProxyId)?.displayName()
                        ?: context.getString(R.string.not_set)
                } else {
                    context.getString(R.string.disable)
                }
            }
            val landingProxySummary = remember(landingProxyId) {
                if (landingProxyId > 0L) {
                    ProfileManager.getProfile(landingProxyId)?.displayName()
                        ?: context.getString(R.string.not_set)
                } else {
                    context.getString(R.string.disable)
                }
            }

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
                    entity.name = DataStore.groupName.ifBlank { "My group" }
                    entity.type = DataStore.groupType
                    entity.order = DataStore.groupOrder
                    entity.isSelector = DataStore.groupIsSelector
                    entity.frontProxy = frontProxyId
                    entity.landingProxy = landingProxyId
                    if (entity.type == GroupType.SUBSCRIPTION) {
                        val sub = (entity.subscription ?: SubscriptionBean().applyDefaultValues()).apply {
                            link = DataStore.subscriptionLink
                            serverDnsResolver = DataStore.subscriptionServerDns
                            forceResolve = DataStore.subscriptionForceResolve
                            deduplication = DataStore.subscriptionDeduplication
                            filterMode = DataStore.subscriptionFilterMode
                            filterRegex = DataStore.subscriptionFilterRegex
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
                                )
                                PrefActionRow(
                                    title = stringResource(R.string.front_proxy),
                                    summary = frontProxySummary,
                                    icon = Icons.AutoMirrored.Rounded.Login,
                                    onClick = { showFrontProxyDialog = true },
                                )
                                PrefActionRow(
                                    title = stringResource(R.string.landing_proxy),
                                    summary = landingProxySummary,
                                    icon = Icons.AutoMirrored.Rounded.Logout,
                                    position = ExpressiveGroupPosition.BOTTOM,
                                    onClick = { showLandingProxyDialog = true },
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
                                        key = Key.SUBSCRIPTION_SERVER_DNS,
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
                                    val filterMode = store.getString(Key.SUBSCRIPTION_FILTER_MODE, "0").toIntOrNull() ?: 0
                                    val showFilterRegex = filterMode != 0
                                    StoreListPref(
                                        store = store,
                                        key = Key.SUBSCRIPTION_FILTER_MODE,
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
                                            key = Key.SUBSCRIPTION_FILTER_REGEX,
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
                                        enabled = autoUpdateEnabled,
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
                    onBack = { if (DataStore.dirty) showUnsavedDialog = true else finish() },
                    actions = if (editingId != 0L) {
                        {
                            IconButton(onClick = { showDeleteDialog = true }) {
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

            if (showFrontProxyDialog) {
                AppDialog(
                    icon = Icons.AutoMirrored.Rounded.Login,
                    title = stringResource(R.string.front_proxy),
                    onDismiss = { showFrontProxyDialog = false },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            DialogOptionRow(
                                label = stringResource(R.string.disable),
                                selected = frontProxyId <= 0L,
                                onClick = {
                                    frontProxyId = -1L
                                    DataStore.frontProxy = -1L
                                    DataStore.dirty = true
                                    showFrontProxyDialog = false
                                },
                            )
                            DialogOptionRow(
                                label = stringResource(R.string.select_profile),
                                selected = frontProxyId > 0L,
                                onClick = {
                                    showFrontProxyDialog = false
                                    frontProxyLauncher.launch(Intent(context, ProfileSelectActivity::class.java))
                                },
                            )
                        }
                    },
                    confirmButton = {
                        DialogCancelButton(stringResource(android.R.string.cancel)) {
                            showFrontProxyDialog = false
                        }
                    },
                )
            }

            if (showLandingProxyDialog) {
                AppDialog(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    title = stringResource(R.string.landing_proxy),
                    onDismiss = { showLandingProxyDialog = false },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            DialogOptionRow(
                                label = stringResource(R.string.disable),
                                selected = landingProxyId <= 0L,
                                onClick = {
                                    landingProxyId = -1L
                                    DataStore.landingProxy = -1L
                                    DataStore.dirty = true
                                    showLandingProxyDialog = false
                                },
                            )
                            DialogOptionRow(
                                label = stringResource(R.string.select_profile),
                                selected = landingProxyId > 0L,
                                onClick = {
                                    showLandingProxyDialog = false
                                    landingProxyLauncher.launch(Intent(context, ProfileSelectActivity::class.java))
                                },
                            )
                        }
                    },
                    confirmButton = {
                        DialogCancelButton(stringResource(android.R.string.cancel)) {
                            showLandingProxyDialog = false
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
                            saveGroup()
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
                    title = stringResource(R.string.delete_group_prompt),
                    onDismiss = { showDeleteDialog = false },
                    confirmButton = {
                        DialogDestructiveButton(stringResource(R.string.yes)) {
                            showDeleteDialog = false
                            runOnDefaultDispatcher {
                                GroupManager.deleteGroup(editingId)
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
