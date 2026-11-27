package io.nekohasekai.sagernet.ui.screens

import io.nekohasekai.sagernet.ui.components.hapticClick

import io.nekohasekai.sagernet.ui.components.AppDialog
import io.nekohasekai.sagernet.ui.components.DialogCancelButton
import io.nekohasekai.sagernet.ui.components.DialogConfirmButton
import io.nekohasekai.sagernet.ui.components.DialogDestructiveButton
import android.content.Intent
import android.text.format.Formatter
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.fmt.toUniversalLink
import io.nekohasekai.sagernet.group.GroupUpdater
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.GroupSettingsActivity
import io.nekohasekai.sagernet.ui.components.ExpressiveEmptyState
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.LocalBottomInset
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressivePillBadge
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import io.nekohasekai.sagernet.ui.components.QrCodeDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.FileDownload
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.readableMessage

@Composable
fun GroupScreen(
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var groups by remember {
        mutableStateOf(
            SagerDatabase.groupDao.allGroups().map { group ->
                group to SagerDatabase.proxyDao.countByGroup(group.id)
            }
        )
    }
    var showUpdateAllDialog by remember { mutableStateOf(false) }
    var deleteConfirmGroup by remember { mutableStateOf<ProxyGroup?>(null) }
    var qrData by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingExportText by remember { mutableStateOf<String?>(null) }
    val exportFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val content = pendingExportText
        pendingExportText = null
        if (uri != null && !content.isNullOrBlank()) {
            runOnDefaultDispatcher {
                try {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(content)
                    }
                    onMainDispatcher { onShowMessage(context.getString(R.string.action_export_msg)) }
                } catch (e: Exception) {
                    onMainDispatcher { onShowMessage(e.readableMessage) }
                }
            }
        }
    }

    fun reload() {
        scope.launch {
            groups = withContext(Dispatchers.IO) {
                SagerDatabase.groupDao.allGroups().map { group ->
                    group to SagerDatabase.proxyDao.countByGroup(group.id)
                }
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    DisposableEffect(Unit) {
        val listener = object : GroupManager.Listener {
            override suspend fun groupAdd(group: ProxyGroup) = reload()
            override suspend fun groupUpdated(group: ProxyGroup) = reload()
            override suspend fun groupRemoved(groupId: Long) = reload()
            override suspend fun groupUpdated(groupId: Long) = reload()
        }
        GroupManager.addListener(listener)
        onDispose { GroupManager.removeListener(listener) }
    }

    val topInset = floatingTopBarSpace()
    val bottomInset = LocalBottomInset.current

    val topBackdrop = rememberBlurBackdrop()
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blurBackdropSource(topBackdrop),
        ) {
        if (groups.isEmpty()) {
            ExpressiveEmptyState(
                icon = Icons.Rounded.FolderCopy,
                title = stringResource(R.string.menu_group),
                actionLabel = stringResource(R.string.group_create),
                onAction = { context.startActivity(Intent(context, GroupSettingsActivity::class.java)) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset, bottom = bottomInset),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = topInset + 16.dp,
                    bottom = bottomInset + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(groups, key = { _, (g, _) -> g.id }) { idx, (group, count) ->
                    val pos = when {
                        groups.size == 1 -> ExpressiveGroupPosition.SINGLE
                        idx == 0 -> ExpressiveGroupPosition.TOP
                        idx == groups.lastIndex -> ExpressiveGroupPosition.BOTTOM
                        else -> ExpressiveGroupPosition.MIDDLE
                    }
                    GroupExpressiveCard(
                        group = group,
                        profileCount = count,
                        position = pos,
                        onEdit = {
                            context.startActivity(
                                Intent(context, GroupSettingsActivity::class.java).apply {
                                    putExtra(GroupSettingsActivity.EXTRA_GROUP_ID, group.id)
                                }
                            )
                        },
                        onUpdateSubscription = {
                            GroupUpdater.startUpdate(group, true)
                            reload()
                        },
                        onCopySubscriptionLink = {
                            val link = group.toUniversalLink()
                            SagerNet.trySetPrimaryClip(link)
                            onShowMessage(context.getString(R.string.copy_success))
                        },
                        onShowQr = {
                            qrData = group.displayName() to group.toUniversalLink()
                        },
                        onExportProfilesClipboard = {
                            runOnDefaultDispatcher {
                                val links = SagerDatabase.proxyDao.getByGroup(group.id)
                                    .joinToString("\n") { it.toStdLink() }
                                onMainDispatcher {
                                    if (links.isBlank()) {
                                        onShowMessage(context.getString(R.string.no_proxies_found))
                                    } else {
                                        SagerNet.trySetPrimaryClip(links)
                                        onShowMessage(context.getString(R.string.copy_success))
                                    }
                                }
                            }
                        },
                        onExportProfilesFile = {
                            runOnDefaultDispatcher {
                                val links = SagerDatabase.proxyDao.getByGroup(group.id)
                                    .joinToString("\n") { it.toStdLink() }
                                onMainDispatcher {
                                    if (links.isBlank()) {
                                        onShowMessage(context.getString(R.string.no_proxies_found))
                                    } else {
                                        pendingExportText = links
                                        exportFileLauncher.launch("${group.displayName()}.txt")
                                    }
                                }
                            }
                        },
                        onClearProfiles = {
                            runOnDefaultDispatcher {
                                GroupManager.clearGroup(group.id)
                                reload()
                            }
                        },
                        onDelete = { deleteConfirmGroup = group },
                    )
                }
            }
        }

        }

        FloatingTopBar(
            backdrop = topBackdrop,
            title = null,
            actions = {
                IconButton(onClick = hapticClick { showUpdateAllDialog = true }) {
                    Icon(Icons.Rounded.Sync, contentDescription = stringResource(R.string.update_all_subscription))
                }
                IconButton(onClick = hapticClick {
                    context.startActivity(Intent(context, GroupSettingsActivity::class.java))
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.group_create))
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (showUpdateAllDialog) {
        AppDialog(
            icon = Icons.Rounded.Sync,
            title = stringResource(R.string.confirm),
            onDismiss = { showUpdateAllDialog = false },
            text = { Text(stringResource(R.string.update_all_subscription)) },
            confirmButton = {
                DialogConfirmButton(label = stringResource(R.string.yes)) {
                    showUpdateAllDialog = false
                    runOnDefaultDispatcher {
                        SagerDatabase.groupDao.allGroups()
                            .filter { it.type == GroupType.SUBSCRIPTION }
                            .forEach { GroupUpdater.startUpdate(it, true) }
                    }
                }
            },
            dismissButton = {
                DialogCancelButton(label = stringResource(R.string.no)) {
                    showUpdateAllDialog = false
                }
            },
        )
    }

    deleteConfirmGroup?.let { group ->
        AppDialog(
            icon = Icons.Rounded.Delete,
            title = stringResource(R.string.delete_group_prompt),
            onDismiss = { deleteConfirmGroup = null },
            text = { Text(group.displayName()) },
            confirmButton = {
                DialogDestructiveButton(label = stringResource(R.string.yes)) {
                    deleteConfirmGroup = null
                    runOnDefaultDispatcher { GroupManager.deleteGroup(group.id) }
                }
            },
            dismissButton = {
                DialogCancelButton(label = stringResource(R.string.no)) {
                    deleteConfirmGroup = null
                }
            },
        )
    }

    qrData?.let { (title, content) ->
        QrCodeDialog(
            title = title,
            content = content,
            onCopy = {
                SagerNet.trySetPrimaryClip(content)
                onShowMessage(context.getString(R.string.copy_success))
                qrData = null
            },
            onDismiss = { qrData = null },
        )
    }
}

@Composable
private fun GroupExpressiveCard(
    group: ProxyGroup,
    profileCount: Long,
    position: ExpressiveGroupPosition,
    onEdit: () -> Unit,
    onUpdateSubscription: () -> Unit,
    onCopySubscriptionLink: () -> Unit,
    onShowQr: () -> Unit,
    onExportProfilesClipboard: () -> Unit,
    onExportProfilesFile: () -> Unit,
    onClearProfiles: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val isSubscription = group.type == GroupType.SUBSCRIPTION
    val isUpdating = group.id in GroupUpdater.updating
    var menuExpanded by remember { mutableStateOf(false) }

    ExpressiveSurfaceItem(
        position = position,
        onClick = onEdit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExpressivePillBadge(
                            text = if (isSubscription) stringResource(R.string.subscription) else stringResource(R.string.group_basic),
                        )
                        ExpressivePillBadge(
                            text = stringResource(R.string.group_status_proxies, profileCount),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }

                if (isSubscription) {
                    if (isUpdating) {
                        LoadingIndicator(modifier = Modifier.size(32.dp))
                    } else {
                        FilledTonalButton(
                            onClick = hapticClick(block = onUpdateSubscription),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.group_update), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Box {
                    IconButton(onClick = hapticClick { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (isSubscription) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share_qr_nfc)) },
                                leadingIcon = { Icon(Icons.Rounded.QrCode2, null) },
                                onClick = hapticClick { menuExpanded = false; onShowQr() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_export_clipboard)) },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                                onClick = hapticClick { menuExpanded = false; onCopySubscriptionLink() },
                            )
                        }
                        if (profileCount > 0) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_export_clipboard)) },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                                onClick = hapticClick { menuExpanded = false; onExportProfilesClipboard() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_export_file)) },
                                leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                                onClick = hapticClick { menuExpanded = false; onExportProfilesFile() },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clear_profiles)) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteSweep, null) },
                            onClick = hapticClick { menuExpanded = false; onClearProfiles() },
                        )
                        if (!group.ungrouped) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = hapticClick { menuExpanded = false; onDelete() },
                            )
                        }
                    }
                }
            }

            val sub = group.subscription
            if (isSubscription && sub != null && (sub.bytesUsed > 0L || sub.bytesRemaining > 0L)) {
                val total = (sub.bytesUsed + sub.bytesRemaining).coerceAtLeast(1L)
                val progress = (sub.bytesUsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                Spacer(modifier = Modifier.height(12.dp))
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${Formatter.formatFileSize(context, sub.bytesUsed)} / ${Formatter.formatFileSize(context, total)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
