package io.nekohasekai.sagernet.ui.profile

import io.nekohasekai.sagernet.ui.components.hapticClick

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import kotlin.properties.Delegates
import androidx.compose.material.icons.rounded.WarningAmber
import io.nekohasekai.sagernet.ui.components.AppDialog
import io.nekohasekai.sagernet.ui.components.DialogCancelButton
import io.nekohasekai.sagernet.ui.components.DialogConfirmButton
import io.nekohasekai.sagernet.ui.components.DialogDestructiveButton
import io.nekohasekai.sagernet.ui.components.DialogOptionRow
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.QrCode2
import io.nekohasekai.sagernet.fmt.buildConfig
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ui.components.QrCodeDialog

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
    open fun isPreferenceEnabled(key: String, store: ComposePreferenceStore): Boolean = true
    open fun resolvePreferenceTitleRes(key: String, store: ComposePreferenceStore): Int? = null
    open fun resolvePreferenceListOptions(key: String, store: ComposePreferenceStore): Pair<List<String>, List<String>>? = null

    val proxyEntity by lazy { SagerDatabase.proxyDao.getById(DataStore.editingId) }
    protected var isSubscription by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editingId = intent.getLongExtra(EXTRA_PROFILE_ID, 0L)
        isSubscription = intent.getBooleanExtra(EXTRA_IS_SUBSCRIPTION, false)
        DataStore.editingId = editingId

        val initialMoveGroups: List<ProxyGroup> = if (editingId == 0L) {
            DataStore.editingGroup = DataStore.selectedGroupForImport()
            createEntity().applyDefaultValues().init()
            emptyList()
        } else {
            val ent = proxyEntity
            if (ent == null) {
                finish()
                return
            }
            DataStore.editingGroup = ent.groupId
            (ent.requireBean() as T).init()
            SagerDatabase.groupDao.allGroups()
                .filter { it.type == GroupType.BASIC && it.id != ent.groupId }
        }
        DataStore.dirty = false

        setComposeContent {
            var showUnsavedDialog by remember { mutableStateOf(false) }
            var showDeleteDialog by remember { mutableStateOf(false) }
            var showMoveDialog by remember { mutableStateOf(false) }
            val moveCandidateGroups = remember { initialMoveGroups }
            var moreMenuOpen by remember { mutableStateOf(false) }
            var qrDialogData by remember { mutableStateOf<Pair<String, String>?>(null) }
            var pendingExportConfig by remember { mutableStateOf<String?>(null) }
            val exportConfigLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                val cfg = pendingExportConfig
                pendingExportConfig = null
                if (uri != null && !cfg.isNullOrBlank()) {
                    runOnDefaultDispatcher {
                        try {
                            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(cfg) }
                            onMainDispatcher { showComposeSnackbar(getString(R.string.action_export_msg)) }
                        } catch (e: Exception) {
                            onMainDispatcher { showComposeSnackbar(e.readableMessage) }
                        }
                    }
                }
            }

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

                FloatingTopBar(
                    backdrop = backdrop,
                    title = stringResource(R.string.profile_config),
                    onBack = {
                        if (DataStore.dirty) showUnsavedDialog = true else finish()
                    },
                    actions = {
                        IconButton(onClick = hapticClick {
                            if (DataStore.editingId == 0L) finish() else showDeleteDialog = true
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                        }
                        Box {
                            IconButton(onClick = hapticClick { moreMenuOpen = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = moreMenuOpen, onDismissRequest = { moreMenuOpen = false }) {
                                if (editingId != 0L && moveCandidateGroups.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.move)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.DriveFileMove, null) },
                                        onClick = hapticClick {
                                            moreMenuOpen = false
                                            showMoveDialog = true
                                        },
                                    )
                                }
                                if (editingId != 0L) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.share_qr_nfc)) },
                                        leadingIcon = { Icon(Icons.Rounded.QrCode2, null) },
                                        onClick = hapticClick {
                                            moreMenuOpen = false
                                            proxyEntity?.let { ent ->
                                                qrDialogData = ent.displayName() to ent.toStdLink()
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_export_clipboard)) },
                                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                                        onClick = hapticClick {
                                            moreMenuOpen = false
                                            proxyEntity?.let { ent ->
                                                SagerNet.trySetPrimaryClip(ent.toStdLink())
                                                showComposeSnackbar(getString(R.string.copy_success))
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_configuration)) },
                                        leadingIcon = { Icon(Icons.Rounded.DataObject, null) },
                                        onClick = hapticClick {
                                            moreMenuOpen = false
                                            proxyEntity?.let { ent ->
                                                runOnDefaultDispatcher {
                                                    try {
                                                        val json = buildConfig(ent).config
                                                        onMainDispatcher {
                                                            SagerNet.trySetPrimaryClip(json)
                                                            showComposeSnackbar(getString(R.string.copy_success))
                                                        }
                                                    } catch (e: Exception) {
                                                        onMainDispatcher { showComposeSnackbar(e.readableMessage) }
                                                    }
                                                }
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_export_file)) },
                                        leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                                        onClick = hapticClick {
                                            moreMenuOpen = false
                                            proxyEntity?.let { ent ->
                                                runOnDefaultDispatcher {
                                                    try {
                                                        val json = buildConfig(ent).config
                                                        onMainDispatcher {
                                                            pendingExportConfig = json
                                                            exportConfigLauncher.launch("${ent.displayName()}.json")
                                                        }
                                                    } catch (e: Exception) {
                                                        onMainDispatcher { showComposeSnackbar(e.readableMessage) }
                                                    }
                                                }
                                            }
                                        },
                                    )
                                }
                                if (Build.VERSION.SDK_INT >= 26 && editingId != 0L) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.create_shortcut)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.AddToHomeScreen, null) },
                                        onClick = hapticClick {
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
                                    onClick = hapticClick {
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
                                    onClick = hapticClick {
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
                    onClick = hapticClick { runOnDefaultDispatcher { saveAndExit() } },
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

            qrDialogData?.let { (title, link) ->
                QrCodeDialog(
                    title = title,
                    content = link,
                    onCopy = {
                        SagerNet.trySetPrimaryClip(link)
                        showComposeSnackbar(getString(R.string.copy_success))
                        qrDialogData = null
                    },
                    onDismiss = { qrDialogData = null },
                )
            }

            if (showUnsavedDialog) {
                AppDialog(
                    icon = Icons.Rounded.WarningAmber,
                    title = stringResource(R.string.unsaved_changes_prompt),
                    onDismiss = { showUnsavedDialog = false },
                    confirmButton = {
                        DialogConfirmButton(
                            label = stringResource(R.string.yes),
                            onClick = {
                                showUnsavedDialog = false
                                runOnDefaultDispatcher { saveAndExit() }
                            },
                        )
                    },
                    dismissButton = {
                        DialogCancelButton(
                            label = stringResource(R.string.no),
                            onClick = {
                                showUnsavedDialog = false
                                finish()
                            },
                        )
                    },
                )
            }

            if (showDeleteDialog) {
                AppDialog(
                    icon = Icons.Rounded.Delete,
                    title = stringResource(R.string.delete_confirm_prompt),
                    onDismiss = { showDeleteDialog = false },
                    confirmButton = {
                        DialogDestructiveButton(
                            label = stringResource(R.string.yes),
                            onClick = {
                                showDeleteDialog = false
                                runOnDefaultDispatcher {
                                    ProfileManager.deleteProfile(DataStore.editingGroup, DataStore.editingId)
                                    onMainDispatcher { finish() }
                                }
                            },
                        )
                    },
                    dismissButton = {
                        DialogCancelButton(
                            label = stringResource(R.string.no),
                            onClick = { showDeleteDialog = false },
                        )
                    },
                )
            }

            if (showMoveDialog) {
                AppDialog(
                    icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                    title = stringResource(R.string.move),
                    onDismiss = { showMoveDialog = false },
                    text = {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(moveCandidateGroups, key = { it.id }) { grp ->
                                DialogOptionRow(
                                    label = grp.displayName(),
                                    selected = false,
                                    onClick = {
                                        showMoveDialog = false
                                        val ent = proxyEntity ?: return@DialogOptionRow
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
                                )
                            }
                        }
                    },
                    confirmButton = {
                        DialogCancelButton(
                            label = stringResource(android.R.string.cancel),
                            onClick = { showMoveDialog = false },
                        )
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
            isKeyEnabled = ::isPreferenceEnabled,
            resolveTitleRes = ::resolvePreferenceTitleRes,
            resolveListOptions = ::resolvePreferenceListOptions,
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
