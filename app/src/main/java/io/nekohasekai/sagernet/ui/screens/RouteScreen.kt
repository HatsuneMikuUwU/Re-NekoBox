package io.nekohasekai.sagernet.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.RuleEntity
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.AssetsActivity
import io.nekohasekai.sagernet.ui.RouteSettingsActivity
import io.nekohasekai.sagernet.ui.components.ExpressiveEmptyState
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressivePillBadge
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RouteScreen(
    onOpenDrawer: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rules by remember { mutableStateOf<List<RuleEntity>>(emptyList()) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            rules = withContext(Dispatchers.IO) { ProfileManager.getRules() }
        }
    }

    LaunchedEffect(Unit) { reload() }

    DisposableEffect(Unit) {
        val listener = object : ProfileManager.RuleListener {
            override suspend fun onAdd(rule: RuleEntity) = reload()
            override suspend fun onUpdated(rule: RuleEntity) = reload()
            override suspend fun onRemoved(ruleId: Long) = reload()
            override suspend fun onCleared() = reload()
        }
        ProfileManager.addListener(listener)
        onDispose { ProfileManager.removeListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.menu_route),
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
                    IconButton(onClick = {
                        context.startActivity(Intent(context, AssetsActivity::class.java))
                    }) {
                        Icon(Icons.Rounded.FolderZip, contentDescription = stringResource(R.string.route_manage_assets))
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.route_reset))
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(context, RouteSettingsActivity::class.java))
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.route_add))
                    }
                },
            )
        },
    ) { padding ->
        if (rules.isEmpty()) {
            ExpressiveEmptyState(
                icon = Icons.Rounded.AccountTree,
                title = stringResource(R.string.menu_route),
                actionLabel = stringResource(R.string.route_add),
                onAction = { context.startActivity(Intent(context, RouteSettingsActivity::class.java)) },
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
                itemsIndexed(rules, key = { _, r -> r.id }) { idx, rule ->
                    val pos = when {
                        rules.size == 1 -> ExpressiveGroupPosition.SINGLE
                        idx == 0 -> ExpressiveGroupPosition.TOP
                        idx == rules.lastIndex -> ExpressiveGroupPosition.BOTTOM
                        else -> ExpressiveGroupPosition.MIDDLE
                    }
                    val outboundColor = when (rule.outbound) {
                        -2L -> Color(0xFFE53935) // Block
                        -1L -> Color(0xFF43A047) // Direct
                        0L -> Color(0xFF1E88E5)  // Proxy
                        else -> Color(0xFF8E24AA) // Custom / Profile
                    }

                    ExpressiveSurfaceItem(
                        position = pos,
                        onClick = {
                            context.startActivity(
                                Intent(context, RouteSettingsActivity::class.java).apply {
                                    putExtra(RouteSettingsActivity.EXTRA_ROUTE_ID, rule.id)
                                }
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
                                    text = rule.displayName(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val summary = rule.mkSummary()
                                if (summary.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                ExpressivePillBadge(
                                    text = rule.displayOutbound(),
                                    containerColor = outboundColor.copy(alpha = 0.14f),
                                    contentColor = outboundColor,
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { checked ->
                                    runOnDefaultDispatcher {
                                        rule.enabled = checked
                                        ProfileManager.updateRule(rule)
                                    }
                                },
                            )
                            IconButton(onClick = {
                                runOnDefaultDispatcher {
                                    ProfileManager.deleteRule(rule.id)
                                }
                            }) {
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

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.confirm)) },
            text = { Text(stringResource(R.string.clear_profiles_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        runOnDefaultDispatcher {
                            SagerDatabase.rulesDao.reset()
                            DataStore.rulesFirstCreate = false
                            reload()
                        }
                    },
                    shape = CircleShape,
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }
}
