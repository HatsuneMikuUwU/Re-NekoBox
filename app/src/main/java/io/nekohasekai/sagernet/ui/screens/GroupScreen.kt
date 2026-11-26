package io.nekohasekai.sagernet.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressivePillBadge
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import io.nekohasekai.sagernet.ui.components.QrCodeDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GroupScreen(
    onOpenDrawer: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var groups by remember { mutableStateOf<List<Pair<ProxyGroup, Long>>>(emptyList()) }
    var showUpdateAllDialog by remember { mutableStateOf(false) }
    var deleteConfirmGroup by remember { mutableStateOf<ProxyGroup?>(null) }
    var qrData by remember { mutableStateOf<Pair<String, String>?>(null) }

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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.menu_group),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { showUpdateAllDialog = true }) {
                        Icon(Icons.Rounded.Sync, contentDescription = stringResource(R.string.update_all_subscription))
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(context, GroupSettingsActivity::class.java))
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.group_create))
                    }
                },
            )
        },
    ) { padding ->
        if (groups.isEmpty()) {
            ExpressiveEmptyState(
                icon = Icons.Rounded.FolderCopy,
                title = stringResource(R.string.menu_group),
                actionLabel = stringResource(R.string.group_create),
                onAction = { context.startActivity(Intent(context, GroupSettingsActivity::class.java)) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(16.dp),
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

    if (showUpdateAllDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateAllDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.confirm)) },
            text = { Text(stringResource(R.string.update_all_subscription)) },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateAllDialog = false
                        runOnDefaultDispatcher {
                            SagerDatabase.groupDao.allGroups()
                                .filter { it.type == GroupType.SUBSCRIPTION }
                                .forEach { GroupUpdater.startUpdate(it, true) }
                        }
                    },
                    shape = CircleShape,
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateAllDialog = false }) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }

    deleteConfirmGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { deleteConfirmGroup = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.delete_group_prompt)) },
            text = { Text(group.displayName()) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmGroup = null
                        runOnDefaultDispatcher { GroupManager.deleteGroup(group.id) }
                    },
                    shape = CircleShape,
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmGroup = null }) {
                    Text(stringResource(R.string.no))
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
                            text = if (isSubscription) stringResource(R.string.group_type_subscription) else stringResource(R.string.group_type_basic),
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
                            onClick = onUpdateSubscription,
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
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { menuExpanded = false; onEdit() },
                        )
                        if (isSubscription) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share_qr_nfc)) },
                                leadingIcon = { Icon(Icons.Rounded.QrCode2, null) },
                                onClick = { menuExpanded = false; onShowQr() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_export_clipboard)) },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                                onClick = { menuExpanded = false; onCopySubscriptionLink() },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clear_profiles)) },
                            onClick = { menuExpanded = false; onClearProfiles() },
                        )
                        if (!group.ungrouped) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; onDelete() },
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
