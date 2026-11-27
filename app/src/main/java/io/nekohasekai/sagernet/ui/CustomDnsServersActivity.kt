package io.nekohasekai.sagernet.ui

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.CustomDnsServerEntity
import io.nekohasekai.sagernet.database.CustomDnsServerStore
import io.nekohasekai.sagernet.fmt.TAG_PROXY
import io.nekohasekai.sagernet.fmt.normalizeCustomDnsDetour
import io.nekohasekai.sagernet.ui.background.screenContainerColor
import io.nekohasekai.sagernet.ui.components.AppDialog
import io.nekohasekai.sagernet.ui.components.DialogCancelButton
import io.nekohasekai.sagernet.ui.components.DialogDestructiveButton
import io.nekohasekai.sagernet.ui.components.DraggableItem
import io.nekohasekai.sagernet.ui.components.ExpressiveEmptyState
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressivePillBadge
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import io.nekohasekai.sagernet.ui.components.ExpressiveSwitch
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.dragContainer
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.hapticBooleanChange
import io.nekohasekai.sagernet.ui.components.hapticClick
import io.nekohasekai.sagernet.ui.components.moveItem
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.rememberDragDropState
import io.nekohasekai.sagernet.ui.components.rememberFabExpanded
import moe.matsuri.nb4a.utils.listByLineOrComma

class CustomDnsServersActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            var servers by remember { mutableStateOf(CustomDnsServerStore.allServers()) }
            var deleteConfirm by remember { mutableStateOf<CustomDnsServerEntity?>(null) }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        servers = CustomDnsServerStore.allServers()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val backdrop = rememberBlurBackdrop()
            val topInset = floatingTopBarSpace()
            val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val listState = rememberLazyListState()
            val fabExpanded = rememberFabExpanded(listState)
            val canReorder = servers.size > 1
            val dragDropState = rememberDragDropState(
                listState = listState,
                onMove = { from, to ->
                    if (canReorder) servers = servers.moveItem(from, to)
                },
                onDragEnd = {
                    if (canReorder) {
                        val updated = servers.mapIndexed { idx, item -> item.copy(userOrder = (idx + 1).toLong()) }
                        servers = updated
                        CustomDnsServerStore.replaceAll(updated)
                    }
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenContainerColor()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blurBackdropSource(backdrop),
                ) {
                    if (servers.isEmpty()) {
                        ExpressiveEmptyState(
                            icon = Icons.Rounded.Dns,
                            title = stringResource(R.string.custom_dns_servers),
                            subtitle = stringResource(R.string.custom_dns_servers_empty),
                            actionLabel = stringResource(R.string.custom_dns_server_add),
                            onAction = {
                                context.startActivity(Intent(context, CustomDnsServerSettingsActivity::class.java))
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topInset, bottom = navBarInset),
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .dragContainer(dragDropState, enabled = canReorder),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = topInset + 16.dp,
                                bottom = navBarInset + 88.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsIndexed(servers, key = { _, item -> item.id }) { idx, item ->
                                val pos = when {
                                    servers.size == 1 -> ExpressiveGroupPosition.SINGLE
                                    idx == 0 -> ExpressiveGroupPosition.TOP
                                    idx == servers.lastIndex -> ExpressiveGroupPosition.BOTTOM
                                    else -> ExpressiveGroupPosition.MIDDLE
                                }
                                val isProxy = normalizeCustomDnsDetour(item.detour) == TAG_PROXY
                                val domainCount = item.domains.listByLineOrComma().size

                                DraggableItem(dragDropState = dragDropState, index = idx) { _ ->
                                    ExpressiveSurfaceItem(
                                        position = pos,
                                        onClick = {
                                            context.startActivity(
                                                Intent(context, CustomDnsServerSettingsActivity::class.java).apply {
                                                    putExtra(CustomDnsServerSettingsActivity.EXTRA_SERVER_ID, item.id)
                                                },
                                            )
                                        },
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.tag,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = item.displayAddress(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    ExpressivePillBadge(
                                                        text = stringResource(
                                                            if (isProxy) R.string.route_proxy else R.string.route_bypass,
                                                        ),
                                                        containerColor = if (isProxy) {
                                                            MaterialTheme.colorScheme.primaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.secondaryContainer
                                                        },
                                                        contentColor = if (isProxy) {
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.onSecondaryContainer
                                                        },
                                                    )
                                                    if (domainCount > 0) {
                                                        ExpressivePillBadge(
                                                            text = stringResource(
                                                                R.string.custom_dns_domains_badge,
                                                                domainCount,
                                                            ),
                                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            ExpressiveSwitch(
                                                checked = item.enabled,
                                                onCheckedChange = hapticBooleanChange { checked ->
                                                    val updated = item.copy(enabled = checked)
                                                    CustomDnsServerStore.save(updated)
                                                    servers = CustomDnsServerStore.allServers()
                                                },
                                            )
                                            IconButton(onClick = hapticClick { deleteConfirm = item }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = stringResource(R.string.delete),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FloatingTopBar(
                    backdrop = backdrop,
                    title = stringResource(R.string.custom_dns_servers),
                    onBack = { finish() },
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                ExtendedFloatingActionButton(
                    onClick = hapticClick {
                        context.startActivity(Intent(context, CustomDnsServerSettingsActivity::class.java))
                    },
                    expanded = fabExpanded,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.custom_dns_server_add)) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )
            }

            deleteConfirm?.let { target ->
                AppDialog(
                    icon = Icons.Rounded.Delete,
                    title = stringResource(R.string.delete),
                    onDismiss = { deleteConfirm = null },
                    text = { Text(target.tag) },
                    confirmButton = {
                        DialogDestructiveButton(stringResource(R.string.yes)) {
                            CustomDnsServerStore.delete(target)
                            servers = CustomDnsServerStore.allServers()
                            deleteConfirm = null
                        }
                    },
                    dismissButton = {
                        DialogCancelButton(stringResource(R.string.no)) {
                            deleteConfirm = null
                        }
                    },
                )
            }
        }
    }
}
