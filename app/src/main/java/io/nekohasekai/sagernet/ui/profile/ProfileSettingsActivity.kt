package io.nekohasekai.sagernet.ui.profile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.QuickToggleShortcut
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.fmt.AbstractBean
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.ThemedActivity
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.LocalEditorContentPadding
import io.nekohasekai.sagernet.ui.components.LocalEditorListState
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.rememberFabExpanded
import io.nekohasekai.sagernet.ui.components.ComposePreferenceStore
import io.nekohasekai.sagernet.ui.components.PrefSectionSpec
import io.nekohasekai.sagernet.ui.components.ProtocolPreferenceComposeScreen
import io.nekohasekai.sagernet.ui.components.rememberComposePreferenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

@Suppress("UNCHECKED_CAST")
abstract class ProfileSettingsActivity<T : AbstractBean> : ThemedActivity() {

    companion object {
        const val EXTRA_PROFILE_ID = "id"
        const val EXTRA_IS_SUBSCRIPTION = "sub"
    }

    abstract fun createEntity(): T
    abstract fun T.init()
    abstract fun T.serialize()

    open val preferenceSchema: List<PrefSectionSpec> = emptyList()
    open val passwordKeys: Set<String> = emptySet()

    open fun isPreferenceVisible(key: String, store: ComposePreferenceStore): Boolean = true

    val proxyEntity by lazy { SagerDatabase.proxyDao.getById(DataStore.editingId) }
    protected var isSubscription by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editingId = intent.getLongExtra(EXTRA_PROFILE_ID, 0L)
        isSubscription = intent.getBooleanExtra(EXTRA_IS_SUBSCRIPTION, false)
        DataStore.editingId = editingId

        setComposeContent {
            var loaded by remember { mutableStateOf(false) }
            var showUnsavedDialog by remember { mutableStateOf(false) }
            var showDeleteDialog by remember { mutableStateOf(false) }
            var showMoveDialog by remember { mutableStateOf(false) }
            var moveCandidateGroups by remember { mutableStateOf<List<ProxyGroup>>(emptyList()) }
            var moreMenuOpen by remember { mutableStateOf(false) }

            val store = rememberComposePreferenceStore(DataStore.profileCacheStore, markProfileDirty = true)

            val customConfigLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                proxyEntity?.requireBean()?.customConfigJson = DataStore.serverCustom
            }
            val customOutboundLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                proxyEntity?.requireBean()?.customOutboundJson = DataStore.serverCustomOutbound
            }

            LaunchedEffect(editingId) {
                val ok = withContext(Dispatchers.IO) {
                    if (editingId == 0L) {
                        DataStore.editingGroup = DataStore.selectedGroupForImport()
                        createEntity().applyDefaultValues().init()
                        true
                    } else {
                        val ent = proxyEntity
                        if (ent == null) {
                            false
                        } else {
                            DataStore.editingGroup = ent.groupId
                            (ent.requireBean() as T).init()
                            moveCandidateGroups = SagerDatabase.groupDao.allGroups()
                                .filter { it.type == GroupType.BASIC && it.id != ent.groupId }
                            true
                        }
                    }
                }
                if (!ok) {
                    finish()
                } else {
                    DataStore.dirty = false
                    loaded = true
                }
            }

            BackHandler(enabled = DataStore.dirty) {
                showUnsavedDialog = true
            }

            val backdrop = rememberBlurBackdrop()
            val topInset = floatingTopBarSpace()
            val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val listState = rememberLazyListState()
            val fabExpanded = rememberFabExpanded(listState)

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
                    if (!loaded) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator()
                        }
                    } else {
                        CompositionLocalProvider(
                            LocalEditorListState provides listState,
                            LocalEditorContentPadding provides PaddingValues(
                                top = topInset + 6.dp,
                                bottom = navBarInset + 88.dp,
                            ),
                        ) {
                            ProfileSettingsContent(store)
                        }
                    }
                }

                FloatingTopBar(
                    backdrop = backdrop,
                    title = stringResource(R.string.profile_config),
                    onBack = {
                        if (DataStore.dirty) showUnsavedDialog = true else finish()
                    },
                    actions = {
                        IconButton(onClick = {
                            if (DataStore.editingId == 0L) finish() else showDeleteDialog = true
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                        }
                        Box {
                            IconButton(onClick = { moreMenuOpen = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = moreMenuOpen, onDismissRequest = { moreMenuOpen = false }) {
                                if (editingId != 0L && moveCandidateGroups.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.move)) },
                                        leadingIcon = { Icon(Icons.Rounded.DriveFileMove, null) },
                                        onClick = {
                                            moreMenuOpen = false
                                            showMoveDialog = true
                                        },
                                    )
                                }
                                if (Build.VERSION.SDK_INT >= 26 && editingId != 0L) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.create_shortcut)) },
                                        leadingIcon = { Icon(Icons.Rounded.AddToHomeScreen, null) },
                                        onClick = {
                                            moreMenuOpen = false
                                            proxyEntity?.let { ent ->
                                                val shortcut = ShortcutInfoCompat.Builder(
                                                    this@ProfileSettingsActivity,
                                                    "shortcut-profile-${ent.id}",
                                                )
                                                    .setShortLabel(ent.displayName())
                                                    .setLongLabel(ent.displayName())
                                                    .setIcon(IconCompat.createWithResource(this@ProfileSettingsActivity, R.drawable.ic_qu_shadowsocks_launcher))
                                                    .setIntent(
                                                        Intent(this@ProfileSettingsActivity, QuickToggleShortcut::class.java).apply {
                                                            action = Intent.ACTION_MAIN
                                                            putExtra("profile", ent.id)
                                                        }
                                                    )
                                                    .build()
                                                ShortcutManagerCompat.requestPinShortcut(this@ProfileSettingsActivity, shortcut, null)
                                            }
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.custom_outbound_json)) },
                                    leadingIcon = { Icon(Icons.Rounded.Code, null) },
                                    onClick = {
                                        moreMenuOpen = false
                                        proxyEntity?.let { ent ->
                                            DataStore.serverCustomOutbound = ent.requireBean().customOutboundJson
                                            customOutboundLauncher.launch(
                                                Intent(this@ProfileSettingsActivity, ConfigEditActivity::class.java).apply {
                                                    putExtra("key", Key.SERVER_CUSTOM_OUTBOUND)
                                                }
                                            )
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.custom_config_json)) },
                                    leadingIcon = { Icon(Icons.Rounded.DataObject, null) },
                                    onClick = {
                                        moreMenuOpen = false
                                        proxyEntity?.let { ent ->
                                            DataStore.serverCustom = ent.requireBean().customConfigJson
                                            customConfigLauncher.launch(
                                                Intent(this@ProfileSettingsActivity, ConfigEditActivity::class.java).apply {
                                                    putExtra("key", Key.SERVER_CUSTOM)
                                                }
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                ExtendedFloatingActionButton(
                    onClick = { runOnDefaultDispatcher { saveAndExit() } },
                    expanded = fabExpanded,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.apply)) },
                    text = { Text(stringResource(R.string.apply)) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )

                SnackbarHost(
                    hostState = composeSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 84.dp),
                )
            }

            if (showUnsavedDialog) {
                AlertDialog(
                    onDismissRequest = { showUnsavedDialog = false },
                    shape = RoundedCornerShape(28.dp),
                    title = { Text(stringResource(R.string.unsaved_changes_prompt)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showUnsavedDialog = false
                                runOnDefaultDispatcher { saveAndExit() }
                            },
                            shape = CircleShape,
                        ) { Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showUnsavedDialog = false
                            finish()
                        }) { Text(stringResource(R.string.no)) }
                    },
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    shape = RoundedCornerShape(28.dp),
                    title = { Text(stringResource(R.string.delete_confirm_prompt)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                runOnDefaultDispatcher {
                                    ProfileManager.deleteProfile(DataStore.editingGroup, DataStore.editingId)
                                    onMainDispatcher { finish() }
                                }
                            },
                            shape = CircleShape,
                        ) { Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.no))
                        }
                    },
                )
            }

            if (showMoveDialog) {
                AlertDialog(
                    onDismissRequest = { showMoveDialog = false },
                    shape = RoundedCornerShape(28.dp),
                    title = { Text(stringResource(R.string.move)) },
                    text = {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(moveCandidateGroups, key = { it.id }) { grp ->
                                Button(
                                    onClick = {
                                        showMoveDialog = false
                                        val ent = proxyEntity ?: return@Button
                                        runOnDefaultDispatcher {
                                            val oldGroup = ent.groupId
                                            ent.groupId = grp.id
                                            ProfileManager.updateProfile(ent)
                                            GroupManager.postUpdate(oldGroup)
                                            GroupManager.postUpdate(grp.id)
                                            DataStore.editingGroup = grp.id
                                            onMainDispatcher { finish() }
                                        }
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(grp.displayName())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMoveDialog = false }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    },
                )
            }
        }
    }

    @Composable
    open fun ProfileSettingsContent(store: ComposePreferenceStore) {
        ProtocolPreferenceComposeScreen(
            sections = preferenceSchema,
            store = store,
            passwordKeys = passwordKeys,
            isKeyVisible = ::isPreferenceVisible,
        )
    }

    open suspend fun saveAndExit() {
        val editingId = DataStore.editingId
        if (editingId == 0L) {
            val editingGroup = DataStore.editingGroup
            ProfileManager.createProfile(editingGroup, createEntity().apply { serialize() })
        } else {
            val ent = proxyEntity
            if (ent == null) {
                onMainDispatcher { finish() }
                return
            }
            if (ent.id == DataStore.selectedProxy) {
                SagerNet.stopService()
            }
            ProfileManager.updateProfile(ent.apply { (requireBean() as T).serialize() })
        }
        onMainDispatcher { finish() }
    }
}
