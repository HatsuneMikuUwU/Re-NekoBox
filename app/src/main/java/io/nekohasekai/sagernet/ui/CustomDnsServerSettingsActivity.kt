package io.nekohasekai.sagernet.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.Http
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.CustomDnsServerEntity
import io.nekohasekai.sagernet.database.CustomDnsServerStore
import io.nekohasekai.sagernet.fmt.CUSTOM_DNS_RESOLVERS
import io.nekohasekai.sagernet.fmt.CUSTOM_DNS_STRATEGIES
import io.nekohasekai.sagernet.fmt.SUPPORTED_CUSTOM_DNS_TYPES
import io.nekohasekai.sagernet.fmt.TAG_DIRECT
import io.nekohasekai.sagernet.fmt.TAG_PROXY
import io.nekohasekai.sagernet.fmt.normalizeCustomDnsDetour
import io.nekohasekai.sagernet.fmt.validateCustomDnsServer
import io.nekohasekai.sagernet.ui.background.screenContainerColor
import io.nekohasekai.sagernet.ui.components.AppDialog
import io.nekohasekai.sagernet.ui.components.DialogCancelButton
import io.nekohasekai.sagernet.ui.components.DialogConfirmButton
import io.nekohasekai.sagernet.ui.components.DialogDestructiveButton
import io.nekohasekai.sagernet.ui.components.DialogOptionRow
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.PrefActionRow
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.hapticClick
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.rememberFabExpanded

class CustomDnsServerSettingsActivity : ThemedActivity() {

    companion object {
        const val EXTRA_SERVER_ID = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editingId = intent.getLongExtra(EXTRA_SERVER_ID, 0L)
        val initial = if (editingId != 0L) {
            CustomDnsServerStore.getById(editingId) ?: CustomDnsServerEntity()
        } else {
            CustomDnsServerEntity()
        }

        setComposeContent {
            var tag by remember { mutableStateOf(initial.tag) }
            var type by remember { mutableStateOf(initial.type.ifBlank { "udp" }) }
            var server by remember { mutableStateOf(initial.server) }
            var port by remember { mutableIntStateOf(initial.serverPort) }
            var path by remember { mutableStateOf(initial.path) }
            var detour by remember { mutableStateOf(normalizeCustomDnsDetour(initial.detour)) }
            var addressResolver by remember { mutableStateOf(initial.addressResolver) }
            var strategy by remember { mutableStateOf(initial.strategy) }
            var domains by remember { mutableStateOf(initial.domains) }

            var validationError by remember { mutableStateOf<String?>(null) }
            var showDeleteDialog by remember { mutableStateOf(false) }
            var textDialog by remember { mutableStateOf<TextDialogSpec?>(null) }
            var choiceDialog by remember { mutableStateOf<ChoiceDialogSpec?>(null) }

            val backdrop = rememberBlurBackdrop()
            val topInset = floatingTopBarSpace()
            val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val listState = rememberLazyListState()
            val fabExpanded = rememberFabExpanded(listState)
            val notSet = stringResource(R.string.not_set)
            val autoLabel = stringResource(R.string.auto)

            fun saveServer() {
                val candidate = initial.copy(
                    tag = tag.trim(),
                    type = type,
                    server = server.trim(),
                    serverPort = port,
                    path = path.trim(),
                    detour = detour,
                    addressResolver = addressResolver,
                    strategy = strategy,
                    domains = domains.trim(),
                )
                val err = validateCustomDnsServer(candidate, CustomDnsServerStore.allServers())
                if (err != null) {
                    validationError = err
                    return
                }
                CustomDnsServerStore.save(candidate)
                finish()
            }

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
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = topInset + 6.dp,
                            bottom = navBarInset + 88.dp,
                        ),
                    ) {
                        item {
                            val isLocal = type == "local"
                            val hasPath = type == "https" || type == "h3"
                            PrefCategory(titleRes = R.string.custom_dns_server_settings) {
                                PrefActionRow(
                                    title = stringResource(R.string.custom_dns_tag),
                                    summary = tag.ifBlank { notSet },
                                    icon = Icons.Rounded.Tag,
                                    position = ExpressiveGroupPosition.TOP,
                                    onClick = {
                                        textDialog = TextDialogSpec(
                                            titleRes = R.string.custom_dns_tag,
                                            initialValue = tag,
                                            onSave = { tag = it },
                                        )
                                    },
                                )
                                PrefActionRow(
                                    title = stringResource(R.string.custom_dns_type),
                                    summary = type.uppercase(),
                                    icon = Icons.Rounded.Dns,
                                    position = if (isLocal) ExpressiveGroupPosition.BOTTOM else ExpressiveGroupPosition.MIDDLE,
                                    onClick = {
                                        choiceDialog = ChoiceDialogSpec(
                                            titleRes = R.string.custom_dns_type,
                                            options = SUPPORTED_CUSTOM_DNS_TYPES.map { it to it.uppercase() },
                                            selected = type,
                                            onSelect = { type = it },
                                        )
                                    },
                                )
                                if (!isLocal) {
                                    PrefActionRow(
                                        title = stringResource(R.string.server_address),
                                        summary = server.ifBlank { notSet },
                                        icon = Icons.Rounded.Lan,
                                        onClick = {
                                            textDialog = TextDialogSpec(
                                                titleRes = R.string.server_address,
                                                initialValue = server,
                                                onSave = { server = it },
                                            )
                                        },
                                    )
                                    PrefActionRow(
                                        title = stringResource(R.string.server_port),
                                        summary = if (port > 0) port.toString() else autoLabel,
                                        icon = Icons.Rounded.Numbers,
                                        position = if (hasPath) ExpressiveGroupPosition.MIDDLE else ExpressiveGroupPosition.BOTTOM,
                                        onClick = {
                                            textDialog = TextDialogSpec(
                                                titleRes = R.string.server_port,
                                                initialValue = if (port > 0) port.toString() else "",
                                                digitsOnly = true,
                                                onSave = { port = it.toIntOrNull() ?: 0 },
                                            )
                                        },
                                    )
                                }
                                if (hasPath) {
                                    PrefActionRow(
                                        title = stringResource(R.string.custom_dns_path),
                                        summary = path.ifBlank { "/dns-query" },
                                        icon = Icons.Rounded.Http,
                                        position = ExpressiveGroupPosition.BOTTOM,
                                        onClick = {
                                            textDialog = TextDialogSpec(
                                                titleRes = R.string.custom_dns_path,
                                                initialValue = path,
                                                onSave = { path = it },
                                            )
                                        },
                                    )
                                }
                            }
                        }

                        item {
                            val directLabel = stringResource(R.string.route_bypass)
                            val proxyLabel = stringResource(R.string.route_proxy)
                            PrefCategory(titleRes = R.string.cag_route) {
                                PrefActionRow(
                                    title = stringResource(R.string.custom_dns_detour),
                                    summary = if (detour == TAG_PROXY) proxyLabel else directLabel,
                                    icon = Icons.AutoMirrored.Rounded.AltRoute,
                                    position = ExpressiveGroupPosition.TOP,
                                    onClick = {
                                        choiceDialog = ChoiceDialogSpec(
                                            titleRes = R.string.custom_dns_detour,
                                            options = listOf(TAG_DIRECT to directLabel, TAG_PROXY to proxyLabel),
                                            selected = detour,
                                            onSelect = { detour = it },
                                        )
                                    },
                                )
                                PrefActionRow(
                                    title = stringResource(R.string.custom_dns_resolver),
                                    summary = addressResolver.ifBlank { autoLabel },
                                    icon = Icons.Rounded.Tune,
                                    onClick = {
                                        choiceDialog = ChoiceDialogSpec(
                                            titleRes = R.string.custom_dns_resolver,
                                            options = CUSTOM_DNS_RESOLVERS.map { it to it.ifBlank { autoLabel } },
                                            selected = addressResolver,
                                            onSelect = { addressResolver = it },
                                        )
                                    },
                                )
                                PrefActionRow(
                                    title = stringResource(R.string.custom_dns_strategy),
                                    summary = strategy.ifBlank { autoLabel },
                                    icon = Icons.Rounded.Route,
                                    onClick = {
                                        choiceDialog = ChoiceDialogSpec(
                                            titleRes = R.string.custom_dns_strategy,
                                            options = CUSTOM_DNS_STRATEGIES.map { it to it.ifBlank { autoLabel } },
                                            selected = strategy,
                                            onSelect = { strategy = it },
                                        )
                                    },
                                )
                                PrefActionRow(
                                    title = stringResource(R.string.custom_dns_domains),
                                    summary = domains.ifBlank { stringResource(R.string.custom_dns_domains_sum) },
                                    icon = Icons.Rounded.Domain,
                                    position = ExpressiveGroupPosition.BOTTOM,
                                    onClick = {
                                        textDialog = TextDialogSpec(
                                            titleRes = R.string.custom_dns_domains,
                                            initialValue = domains,
                                            multiline = true,
                                            onSave = { domains = it },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                FloatingTopBar(
                    backdrop = backdrop,
                    title = stringResource(R.string.custom_dns_server_settings),
                    onBack = { finish() },
                    actions = if (editingId != 0L) {
                        {
                            IconButton(onClick = hapticClick { showDeleteDialog = true }) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    } else null,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                ExtendedFloatingActionButton(
                    onClick = hapticClick { saveServer() },
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

            textDialog?.let { spec ->
                var draft by remember(spec) { mutableStateOf(spec.initialValue) }
                AppDialog(
                    icon = Icons.Rounded.Dns,
                    title = stringResource(spec.titleRes),
                    onDismiss = { textDialog = null },
                    text = {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { input ->
                                draft = if (spec.digitsOnly) input.filter { it.isDigit() } else input
                            },
                            singleLine = !spec.multiline,
                            minLines = if (spec.multiline) 3 else 1,
                            maxLines = if (spec.multiline) 6 else 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        DialogConfirmButton(stringResource(android.R.string.ok)) {
                            spec.onSave(draft)
                            textDialog = null
                        }
                    },
                    dismissButton = {
                        DialogCancelButton(stringResource(android.R.string.cancel)) {
                            textDialog = null
                        }
                    },
                )
            }

            choiceDialog?.let { spec ->
                AppDialog(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(spec.titleRes),
                    onDismiss = { choiceDialog = null },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            spec.options.forEach { (value, label) ->
                                DialogOptionRow(
                                    label = label,
                                    selected = value == spec.selected,
                                    onClick = {
                                        spec.onSelect(value)
                                        choiceDialog = null
                                    },
                                )
                            }
                        }
                    },
                    confirmButton = {
                        DialogCancelButton(stringResource(android.R.string.cancel)) {
                            choiceDialog = null
                        }
                    },
                )
            }

            validationError?.let { msg ->
                AppDialog(
                    icon = Icons.Rounded.WarningAmber,
                    title = stringResource(R.string.custom_dns_server_settings),
                    onDismiss = { validationError = null },
                    text = { Text(msg) },
                    confirmButton = {
                        DialogConfirmButton(stringResource(android.R.string.ok)) {
                            validationError = null
                        }
                    },
                )
            }

            if (showDeleteDialog) {
                AppDialog(
                    icon = Icons.Rounded.Delete,
                    title = stringResource(R.string.delete),
                    onDismiss = { showDeleteDialog = false },
                    text = { Text(initial.tag) },
                    confirmButton = {
                        DialogDestructiveButton(stringResource(R.string.yes)) {
                            CustomDnsServerStore.delete(initial)
                            showDeleteDialog = false
                            finish()
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

    private data class TextDialogSpec(
        val titleRes: Int,
        val initialValue: String,
        val digitsOnly: Boolean = false,
        val multiline: Boolean = false,
        val onSave: (String) -> Unit,
    )

    private data class ChoiceDialogSpec(
        val titleRes: Int,
        val options: List<Pair<String, String>>,
        val selected: String,
        val onSelect: (String) -> Unit,
    )
}
