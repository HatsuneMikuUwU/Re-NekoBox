@file:Suppress("DEPRECATION")

package io.nekohasekai.sagernet.ui.components

import android.content.Intent
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ui.profile.ConfigEditActivity
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.DeviceHub
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Http
import androidx.compose.material.icons.rounded.Https
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.SettingsInputComponent
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShortText
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Stream
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.Edit

sealed interface PrefText {
    data class Res(@StringRes val id: Int) : PrefText
    data class Literal(val value: String) : PrefText

    @Composable
    fun resolve(): String = when (this) {
        is Res -> stringResource(id)
        is Literal -> value
    }
}

enum class PrefFieldType {
    SWITCH, EDIT_TEXT, LIST, CONFIG
}

data class PrefFieldSpec(
    val type: PrefFieldType,
    val key: String,
    val title: PrefText,
    val icon: ImageVector = Icons.Rounded.Tune,
    val summary: PrefText? = null,
    val defaultValue: String = "",
    @ArrayRes val entriesRes: Int? = null,
    @ArrayRes val entryValuesRes: Int? = null,
    val dependency: String? = null,
)

data class PrefSectionSpec(
    val key: String? = null,
    val title: PrefText? = null,
    val items: List<PrefFieldSpec>,
)

@Composable
fun ProtocolPreferenceComposeScreen(
    sections: List<PrefSectionSpec>,
    store: ComposePreferenceStore,
    passwordKeys: Set<String> = emptySet(),
    isKeyVisible: (String, ComposePreferenceStore) -> Boolean = { _, _ -> true },
    isKeyEnabled: (String, ComposePreferenceStore) -> Boolean = { _, _ -> true },
    resolveTitleRes: (String, ComposePreferenceStore) -> Int? = { _, _ -> null },
    resolveListOptions: (String, ComposePreferenceStore) -> Pair<List<String>, List<String>>? = { _, _ -> null },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    @Suppress("UNUSED_VARIABLE")
    val revision = store.revision

    val listState = LocalEditorListState.current ?: rememberLazyListState()
    val contentPadding = LocalEditorContentPadding.current ?: PaddingValues(vertical = 8.dp)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        sections.forEachIndexed { secIdx, section ->
            val sectionVisible = section.key == null || isKeyVisible(section.key, store)
            val visibleItems = if (sectionVisible) {
                section.items.filter { item ->
                    item.key.isBlank() || isKeyVisible(item.key, store)
                }
            } else emptyList()

            if (visibleItems.isNotEmpty()) {
                item(key = "${secIdx}_${section.key ?: visibleItems.first().key}") {
                    val headerTitle = section.title?.resolve() ?: stringResource(R.string.profile_config)
                    PrefCategory(title = headerTitle) {
                        visibleItems.forEachIndexed { idx, item ->
                            val pos = when {
                                visibleItems.size == 1 -> ExpressiveGroupPosition.SINGLE
                                idx == 0 -> ExpressiveGroupPosition.TOP
                                idx == visibleItems.lastIndex -> ExpressiveGroupPosition.BOTTOM
                                else -> ExpressiveGroupPosition.MIDDLE
                            }
                            val enabled = (item.dependency.isNullOrBlank() || store.getBoolean(item.dependency, false)) &&
                                (item.key.isBlank() || isKeyEnabled(item.key, store))
                            val overrideTitleRes = if (item.key.isBlank()) null else resolveTitleRes(item.key, store)
                            val titleStr = overrideTitleRes?.let { stringResource(it) } ?: item.title.resolve()
                            val summaryStr = item.summary?.resolve()

                            when (item.type) {
                                PrefFieldType.SWITCH -> {
                                    val def = item.defaultValue.equals("true", ignoreCase = true)
                                    PrefSwitchRow(
                                        title = titleStr,
                                        summary = summaryStr,
                                        icon = item.icon,
                                        checked = store.getBoolean(item.key, def),
                                        onCheckedChange = { store.putBoolean(item.key, it) },
                                        enabled = enabled,
                                        position = pos,
                                    )
                                }
                                PrefFieldType.CONFIG -> {
                                    val value = store.getString(item.key, item.defaultValue)
                                    PrefActionRow(
                                        title = titleStr,
                                        summary = if (value.isBlank()) stringResource(R.string.not_set) else "JSON (${value.lines().size} lines)",
                                        icon = item.icon,
                                        enabled = enabled,
                                        position = pos,
                                        onClick = {
                                            context.startActivity(
                                                Intent(context, ConfigEditActivity::class.java).apply {
                                                    putExtra("key", item.key)
                                                }
                                            )
                                        },
                                    )
                                }
                                PrefFieldType.LIST -> {
                                    val dynamicOptions = if (item.key.isBlank()) null else resolveListOptions(item.key, store)
                                    val entries = dynamicOptions?.first ?: item.entriesRes?.let { stringArrayResource(it).toList() } ?: emptyList()
                                    val values = dynamicOptions?.second ?: item.entryValuesRes?.let { stringArrayResource(it).toList() } ?: entries
                                    StoreListPrefRaw(
                                        store = store,
                                        key = item.key,
                                        title = titleStr,
                                        entries = entries,
                                        entryValues = values,
                                        defaultValue = item.defaultValue,
                                        icon = item.icon,
                                        enabled = enabled,
                                        position = pos,
                                    )
                                }
                                PrefFieldType.EDIT_TEXT -> {
                                    val isPassword = item.key in passwordKeys ||
                                        item.key.contains("Password", ignoreCase = true) ||
                                        item.key.contains("PrivateKey", ignoreCase = true)
                                    val isNumber = item.key.endsWith("Port", ignoreCase = true) ||
                                        item.key.endsWith("MTU", ignoreCase = true) ||
                                        item.key.contains("Mbps", ignoreCase = true) ||
                                        item.key.contains("Concurrency", ignoreCase = true)
                                    val isMultiline = item.key.contains("Certificate", ignoreCase = true) ||
                                        item.key.contains("Headers", ignoreCase = true) ||
                                        item.key.contains("PrivateKey", ignoreCase = true) ||
                                        item.key.contains("Config", ignoreCase = true)

                                    PureComposeEditTextRow(
                                        store = store,
                                        key = item.key,
                                        title = titleStr,
                                        defaultValue = item.defaultValue,
                                        icon = item.icon,
                                        isPassword = isPassword,
                                        isNumber = isNumber,
                                        multiline = isMultiline,
                                        enabled = enabled,
                                        position = pos,
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

@Composable
private fun PureComposeEditTextRow(
    store: ComposePreferenceStore,
    key: String,
    title: String,
    defaultValue: String,
    icon: ImageVector?,
    isPassword: Boolean,
    isNumber: Boolean,
    multiline: Boolean,
    enabled: Boolean,
    position: ExpressiveGroupPosition,
) {
    val currentValue = store.getString(key, defaultValue)
    var showDialog by remember { mutableStateOf(false) }

    val displaySummary = when {
        currentValue.isBlank() -> stringResource(R.string.not_set)
        isPassword -> "•".repeat(currentValue.length.coerceAtMost(16))
        else -> currentValue.lineSequence().firstOrNull { it.isNotBlank() } ?: currentValue
    }

    PrefActionRow(
        title = title,
        summary = displaySummary,
        icon = icon,
        enabled = enabled,
        position = position,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        var draft by remember(currentValue) { mutableStateOf(currentValue) }
        AppDialog(
            icon = icon ?: Icons.Rounded.Edit,
            title = title,
            onDismiss = { showDialog = false },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = !multiline,
                    minLines = if (multiline) 4 else 1,
                    maxLines = if (multiline) 8 else 1,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when {
                            isNumber -> KeyboardType.Number
                            isPassword -> KeyboardType.Password
                            else -> KeyboardType.Text
                        }
                    ),
                )
            },
            confirmButton = {
                DialogConfirmButton(label = stringResource(android.R.string.ok)) {
                    store.putString(key, draft)
                    showDialog = false
                }
            },
            dismissButton = {
                DialogCancelButton(label = stringResource(android.R.string.cancel)) {
                    showDialog = false
                }
            },
        )
    }
}

object ProtocolPreferenceSchemas {
    val AnyTLS: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.security_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "sni", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "alpn", title = PrefText.Res(R.string.alpn), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "certificates", title = PrefText.Res(R.string.certificates), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "allowInsecure", title = PrefText.Res(R.string.allow_insecure), icon = Icons.Rounded.LockOpen, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.tls_camouflage_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "utlsFingerprint", title = PrefText.Res(R.string.utls_fingerprint), icon = Icons.Rounded.VerifiedUser, summary = null, defaultValue = "", entriesRes = R.array.utls_fingerprint_entry, entryValuesRes = R.array.utls_fingerprint_entry, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "realityPubKey", title = PrefText.Literal("Reality Public Key"), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "realityShortId", title = PrefText.Literal("Reality ShortId"), icon = Icons.Rounded.ShortText, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Config: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "isOutboundOnly", title = PrefText.Res(R.string.is_outbound_only), icon = Icons.Rounded.CallMade, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.CONFIG, key = "serverConfig", title = PrefText.Res(R.string.custom_config), icon = Icons.Rounded.Code, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Hysteria: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "protocolVersion", title = PrefText.Res(R.string.protocol_version), icon = Icons.Rounded.Numbers, summary = null, defaultValue = "2", entriesRes = R.array.hysteria_version, entryValuesRes = R.array.hysteria_version, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPorts", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.Dialpad, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverObfs", title = PrefText.Res(R.string.hysteria_obfs), icon = Icons.Rounded.BlurOn, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverAuthType", title = PrefText.Res(R.string.hysteria_auth_type), icon = Icons.Rounded.AdminPanelSettings, summary = null, defaultValue = "", entriesRes = R.array.hysteria_auth_type, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.hysteria_auth_payload), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverProtocol", title = PrefText.Res(R.string.protocol), icon = Icons.Rounded.Http, summary = null, defaultValue = "https", entriesRes = R.array.hysteria_protocol, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverSNI", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverALPN", title = PrefText.Res(R.string.alpn), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverCertificates", title = PrefText.Res(R.string.certificates), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverAllowInsecure", title = PrefText.Res(R.string.allow_insecure), icon = Icons.Rounded.LockOpen, summary = PrefText.Res(R.string.allow_insecure_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUploadSpeed", title = PrefText.Res(R.string.hysteria_upload_mbps), icon = Icons.Rounded.Upload, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverDownloadSpeed", title = PrefText.Res(R.string.hysteria_download_mbps), icon = Icons.Rounded.Download, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverStreamReceiveWindow", title = PrefText.Res(R.string.hysteria_stream_receive_window), icon = Icons.Rounded.ViewStream, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverConnectionReceiveWindow", title = PrefText.Res(R.string.hysteria_connection_receive_window), icon = Icons.Rounded.SettingsEthernet, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverDisableMtuDiscovery", title = PrefText.Res(R.string.hysteria_disable_mtu_discovery), icon = Icons.Rounded.DoNotDisturbOn, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "hopInterval", title = PrefText.Res(R.string.hop_interval), icon = Icons.Rounded.AvTimer, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Juicity: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUserId", title = PrefText.Res(R.string.uuid), icon = Icons.Rounded.Fingerprint, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverSNI", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPinnedCertChainSha256", title = PrefText.Res(R.string.pinned_certchain_sha256), icon = Icons.Rounded.PushPin, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverAllowInsecure", title = PrefText.Res(R.string.allow_insecure), icon = Icons.Rounded.LockOpen, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Mieru: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverProtocol", title = PrefText.Res(R.string.protocol), icon = Icons.Rounded.Http, summary = null, defaultValue = "TCP", entriesRes = R.array.mieru_protocol, entryValuesRes = R.array.mieru_protocol, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUsername", title = PrefText.Res(R.string.username), icon = Icons.Rounded.Person, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverMTU", title = PrefText.Res(R.string.mtu), icon = Icons.Rounded.Straighten, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Naive: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUsername", title = PrefText.Res(R.string.username_opt), icon = Icons.Rounded.Person, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password_opt), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverProtocol", title = PrefText.Res(R.string.protocol), icon = Icons.Rounded.Http, summary = null, defaultValue = "https", entriesRes = R.array.naive_proto_entry, entryValuesRes = R.array.naive_proto_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverHeaders", title = PrefText.Res(R.string.extra_headers), icon = Icons.Rounded.ListAlt, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverSNI", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverCertificates", title = PrefText.Res(R.string.certificates), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverInsecureConcurrency", title = PrefText.Res(R.string.naive_insecure_concurrency), icon = Icons.Rounded.Speed, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Literal("sing-box server"),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "sUoT", title = PrefText.Literal("UDP over TCP"), icon = Icons.Rounded.SwapVert, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Shadowsocks: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "method", title = PrefText.Res(R.string.enc_method), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = R.array.ss_enc_method_value, entryValuesRes = R.array.ss_enc_method_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.plugin),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "pluginName", title = PrefText.Res(R.string.plugin), icon = Icons.Rounded.Extension, summary = null, defaultValue = "", entriesRes = R.array.box_shadowsocks_plugins, entryValuesRes = R.array.box_shadowsocks_plugins, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "pluginConfig", title = PrefText.Res(R.string.plugin_configure), icon = Icons.Rounded.Build, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Literal("sing-box server"),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "sUoT", title = PrefText.Literal("UDP over TCP"), icon = Icons.Rounded.SwapVert, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverMuxCategory",
            title = PrefText.Res(R.string.mux_preference),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "enableMux", title = PrefText.Res(R.string.enable_mux), icon = Icons.Rounded.CallSplit, summary = PrefText.Res(R.string.mux_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "muxType", title = PrefText.Res(R.string.mux_type), icon = Icons.Rounded.Category, summary = null, defaultValue = "0", entriesRes = R.array.mux_type, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "muxMode", title = PrefText.Res(R.string.mux_mode), icon = Icons.Rounded.AltRoute, summary = null, defaultValue = "0", entriesRes = R.array.mux_mode, entryValuesRes = R.array.int_array_2, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxConcurrency", title = PrefText.Res(R.string.mux_concurrency), icon = Icons.Rounded.GridView, summary = null, defaultValue = "8", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxMaxConnections", title = PrefText.Res(R.string.mux_max_connections), icon = Icons.Rounded.DeviceHub, summary = null, defaultValue = "4", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxMinStreams", title = PrefText.Res(R.string.mux_min_streams), icon = Icons.Rounded.Stream, summary = null, defaultValue = "4", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "muxPadding", title = PrefText.Res(R.string.padding), icon = Icons.Rounded.SpaceBar, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "muxBrutal", title = PrefText.Res(R.string.mux_brutal), icon = Icons.Rounded.FlashOn, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxBrutalUpMbps", title = PrefText.Res(R.string.mux_brutal_up_mbps), icon = Icons.Rounded.Upload, summary = null, defaultValue = "100", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxBrutalDownMbps", title = PrefText.Res(R.string.mux_brutal_down_mbps), icon = Icons.Rounded.Download, summary = null, defaultValue = "100", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val ShadowsocksR: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverMethod", title = PrefText.Res(R.string.enc_method), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = R.array.ssr_enc_method_value, entryValuesRes = R.array.ssr_enc_method_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverProtocol", title = PrefText.Res(R.string.protocol), icon = Icons.Rounded.Http, summary = null, defaultValue = "", entriesRes = R.array.ssr_protocol_value, entryValuesRes = R.array.ssr_protocol_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverProtocolParam", title = PrefText.Res(R.string.protocol_param), icon = Icons.Rounded.Subject, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverObfs", title = PrefText.Res(R.string.obfs), icon = Icons.Rounded.BlurOn, summary = null, defaultValue = "", entriesRes = R.array.ssr_obfs_value, entryValuesRes = R.array.ssr_obfs_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverObfsParam", title = PrefText.Res(R.string.obfs_param), icon = Icons.Rounded.Notes, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val ShadowTLS: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "version", title = PrefText.Res(R.string.protocol_version), icon = Icons.Rounded.Numbers, summary = null, defaultValue = "", entriesRes = R.array.shadowtls_version_value, entryValuesRes = R.array.shadowtls_version_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverSecurityCategory",
            title = PrefText.Res(R.string.security_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "sni", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "alpn", title = PrefText.Res(R.string.alpn), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "certificates", title = PrefText.Res(R.string.certificates), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "allowInsecure", title = PrefText.Res(R.string.allow_insecure), icon = Icons.Rounded.LockOpen, summary = PrefText.Res(R.string.allow_insecure_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "utlsFingerprint", title = PrefText.Res(R.string.utls_fingerprint), icon = Icons.Rounded.VerifiedUser, summary = null, defaultValue = "", entriesRes = R.array.utls_fingerprint_entry, entryValuesRes = R.array.utls_fingerprint_entry, dependency = null),
            ),
        ),
    )

    val Snell: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "psk", title = PrefText.Res(R.string.snell_psk), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "userKey", title = PrefText.Res(R.string.snell_userkey), icon = Icons.Rounded.VpnKey, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "version", title = PrefText.Res(R.string.snell_version), icon = Icons.Rounded.Numbers, summary = null, defaultValue = "4", entriesRes = R.array.snell_versions_entry, entryValuesRes = R.array.snell_versions_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "network", title = PrefText.Res(R.string.snell_network), icon = Icons.Rounded.Lan, summary = null, defaultValue = "", entriesRes = R.array.snell_network_entry, entryValuesRes = R.array.snell_network_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "obfsMode", title = PrefText.Res(R.string.snell_obfs_mode), icon = Icons.Rounded.BlurOn, summary = null, defaultValue = "", entriesRes = R.array.snell_obfs_modes_entry, entryValuesRes = R.array.snell_obfs_modes_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "obfsHost", title = PrefText.Res(R.string.snell_obfs_host), icon = Icons.Rounded.Domain, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "mode", title = PrefText.Res(R.string.snell_mode), icon = Icons.Rounded.Tune, summary = null, defaultValue = "default", entriesRes = R.array.snell_v6_modes_entry, entryValuesRes = R.array.snell_v6_modes_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "quicProxyMode", title = PrefText.Res(R.string.snell_quic_proxy), icon = Icons.Rounded.RocketLaunch, summary = PrefText.Res(R.string.snell_quic_proxy_summary), defaultValue = "false", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "reuse", title = PrefText.Res(R.string.snell_reuse), icon = Icons.Rounded.Repeat, summary = PrefText.Res(R.string.snell_reuse_summary), defaultValue = "false", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Socks: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverProtocol", title = PrefText.Res(R.string.app_version), icon = Icons.Rounded.Http, summary = null, defaultValue = "2", entriesRes = R.array.socks_versions, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUsername", title = PrefText.Res(R.string.username_opt), icon = Icons.Rounded.Person, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password_opt), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Literal("sing-box server"),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "sUoT", title = PrefText.Literal("UDP over TCP"), icon = Icons.Rounded.SwapVert, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val SSH: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUsername", title = PrefText.Res(R.string.username), icon = Icons.Rounded.Person, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverAuthType", title = PrefText.Res(R.string.hysteria_auth_type), icon = Icons.Rounded.AdminPanelSettings, summary = null, defaultValue = "", entriesRes = R.array.ssh_auth_type, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPrivateKey", title = PrefText.Res(R.string.ssh_private_key), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword1", title = PrefText.Res(R.string.ssh_private_key_passphrase), icon = Icons.Rounded.VpnKey, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverCertificates", title = PrefText.Res(R.string.ssh_public_key), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val StandardV2Ray: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "username", title = PrefText.Res(R.string.username_opt), icon = Icons.Rounded.Person, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password_opt), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "uuid", title = PrefText.Res(R.string.uuid), icon = Icons.Rounded.Fingerprint, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "alterId", title = PrefText.Res(R.string.alter_id), icon = Icons.Rounded.Tag, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "encryption", title = PrefText.Res(R.string.encryption), icon = Icons.Rounded.EnhancedEncryption, summary = null, defaultValue = "", entriesRes = R.array.vmess_encryption_value, entryValuesRes = R.array.vmess_encryption_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "vlessEncryption", title = PrefText.Res(R.string.vless_encryption), icon = Icons.Rounded.Shield, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "packetEncoding", title = PrefText.Res(R.string.packet_encoding), icon = Icons.Rounded.Inventory2, summary = null, defaultValue = "", entriesRes = R.array.packet_encoding_entry, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "type", title = PrefText.Res(R.string.network), icon = Icons.Rounded.Hub, summary = null, defaultValue = "", entriesRes = R.array.networks_value, entryValuesRes = R.array.networks_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "host", title = PrefText.Res(R.string.http_host), icon = Icons.Rounded.Language, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "path", title = PrefText.Res(R.string.http_path), icon = Icons.Rounded.Route, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "headerType", title = PrefText.Res(R.string.kcp_header_type), icon = Icons.Rounded.ViewAgenda, summary = null, defaultValue = "none", entriesRes = R.array.kcp_headers_value, entryValuesRes = R.array.kcp_headers_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "kcpCwndMultiplier", title = PrefText.Res(R.string.kcp_cwnd_multiplier), icon = Icons.Rounded.Speed, summary = null, defaultValue = "20", entriesRes = R.array.kcp_cwnd_multiplier_entry, entryValuesRes = R.array.kcp_cwnd_multiplier_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "mKcpSeed", title = PrefText.Res(R.string.kcp_seed), icon = Icons.Rounded.Grain, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "kcpMtu", title = PrefText.Res(R.string.kcp_mtu), icon = Icons.Rounded.Straighten, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "kcpTti", title = PrefText.Res(R.string.kcp_tti), icon = Icons.Rounded.Timer, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "security", title = PrefText.Res(R.string.security), icon = Icons.Rounded.Security, summary = null, defaultValue = "", entriesRes = R.array.transport_layer_encryption_value, entryValuesRes = R.array.transport_layer_encryption_value, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverWsCategory",
            title = PrefText.Res(R.string.cag_ws),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "wsMaxEarlyData", title = PrefText.Res(R.string.ws_max_early_data), icon = Icons.Rounded.Bolt, summary = null, defaultValue = "0", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "earlyDataHeaderName", title = PrefText.Res(R.string.early_data_header_name), icon = Icons.Rounded.Title, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverXhttpCategory",
            title = PrefText.Res(R.string.xhttp_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "xhttpMode", title = PrefText.Res(R.string.xhttp_mode), icon = Icons.Rounded.Tune, summary = null, defaultValue = "auto", entriesRes = R.array.xhttp_mode_entry, entryValuesRes = R.array.xhttp_mode_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "xhttpExtra", title = PrefText.Res(R.string.xhttp_extra), icon = Icons.Rounded.DataObject, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverSecurityCategory",
            title = PrefText.Res(R.string.security_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "sni", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "alpn", title = PrefText.Res(R.string.alpn), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "certificates", title = PrefText.Res(R.string.certificates), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "allowInsecure", title = PrefText.Res(R.string.allow_insecure), icon = Icons.Rounded.LockOpen, summary = PrefText.Res(R.string.allow_insecure_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverTlsCamouflageCategory",
            title = PrefText.Res(R.string.tls_camouflage_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "utlsFingerprint", title = PrefText.Res(R.string.utls_fingerprint), icon = Icons.Rounded.VerifiedUser, summary = null, defaultValue = "", entriesRes = R.array.utls_fingerprint_entry, entryValuesRes = R.array.utls_fingerprint_entry, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "realityPubKey", title = PrefText.Literal("Reality Public Key"), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "realityShortId", title = PrefText.Literal("Reality ShortId"), icon = Icons.Rounded.ShortText, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverMuxCategory",
            title = PrefText.Res(R.string.mux_preference),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "enableMux", title = PrefText.Res(R.string.enable_mux), icon = Icons.Rounded.CallSplit, summary = PrefText.Res(R.string.mux_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "muxType", title = PrefText.Res(R.string.mux_type), icon = Icons.Rounded.Category, summary = null, defaultValue = "0", entriesRes = R.array.mux_type, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "muxMode", title = PrefText.Res(R.string.mux_mode), icon = Icons.Rounded.AltRoute, summary = null, defaultValue = "0", entriesRes = R.array.mux_mode, entryValuesRes = R.array.int_array_2, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxConcurrency", title = PrefText.Res(R.string.mux_concurrency), icon = Icons.Rounded.GridView, summary = null, defaultValue = "8", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxMaxConnections", title = PrefText.Res(R.string.mux_max_connections), icon = Icons.Rounded.DeviceHub, summary = null, defaultValue = "4", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxMinStreams", title = PrefText.Res(R.string.mux_min_streams), icon = Icons.Rounded.Stream, summary = null, defaultValue = "4", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "muxPadding", title = PrefText.Res(R.string.padding), icon = Icons.Rounded.SpaceBar, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "muxBrutal", title = PrefText.Res(R.string.mux_brutal), icon = Icons.Rounded.FlashOn, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxBrutalUpMbps", title = PrefText.Res(R.string.mux_brutal_up_mbps), icon = Icons.Rounded.Upload, summary = null, defaultValue = "100", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxBrutalDownMbps", title = PrefText.Res(R.string.mux_brutal_down_mbps), icon = Icons.Rounded.Download, summary = null, defaultValue = "100", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverECHCategory",
            title = PrefText.Literal("ECH"),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "enableECH", title = PrefText.Res(R.string.enable), icon = Icons.Rounded.Https, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "echConfig", title = PrefText.Res(R.string.ech_config), icon = Icons.Rounded.Code, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val TrojanGo: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverSNI", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverAllowInsecure", title = PrefText.Res(R.string.allow_insecure), icon = Icons.Rounded.LockOpen, summary = PrefText.Res(R.string.allow_insecure_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverNetwork", title = PrefText.Res(R.string.network), icon = Icons.Rounded.Lan, summary = null, defaultValue = "none", entriesRes = R.array.trojan_go_networks_entry, entryValuesRes = R.array.trojan_go_networks_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverEncryption", title = PrefText.Res(R.string.encryption), icon = Icons.Rounded.EnhancedEncryption, summary = null, defaultValue = "", entriesRes = R.array.trojan_go_security_entry, entryValuesRes = R.array.trojan_go_security_value, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverWsCategory",
            title = PrefText.Res(R.string.cag_ws),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverHost", title = PrefText.Res(R.string.ws_host), icon = Icons.Rounded.Language, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPath", title = PrefText.Res(R.string.ws_path), icon = Icons.Rounded.Route, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverSsCategory",
            title = PrefText.Res(R.string.ss_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverMethod", title = PrefText.Res(R.string.enc_method), icon = Icons.Rounded.Key, summary = null, defaultValue = "AES-128-GCM", entriesRes = R.array.trojan_go_methods, entryValuesRes = R.array.trojan_go_methods, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword1", title = PrefText.Res(R.string.password), icon = Icons.Rounded.VpnKey, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Tuic: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUsername", title = PrefText.Res(R.string.uuid), icon = Icons.Rounded.Person, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverALPN", title = PrefText.Res(R.string.alpn), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverCertificates", title = PrefText.Res(R.string.certificates), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverUDPRelayMode", title = PrefText.Res(R.string.tuic_udp_relay_mode), icon = Icons.Rounded.SyncAlt, summary = null, defaultValue = "https", entriesRes = R.array.tuic_udp_relay_mode_value, entryValuesRes = R.array.tuic_udp_relay_mode_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverCongestionController", title = PrefText.Res(R.string.tuic_congestion_controller), icon = Icons.Rounded.Traffic, summary = null, defaultValue = "https", entriesRes = R.array.tuic_congestion_controller_value, entryValuesRes = R.array.tuic_congestion_controller_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverDisableSNI", title = PrefText.Res(R.string.tuic_disable_sni), icon = Icons.Rounded.VisibilityOff, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverSNI", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverReduceRTT", title = PrefText.Res(R.string.tuic_reduce_rtt), icon = Icons.Rounded.Bolt, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverAllowInsecure", title = PrefText.Res(R.string.allow_insecure), icon = Icons.Rounded.LockOpen, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val WireGuard: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "localAddress", title = PrefText.Res(R.string.wireguard_local_address), icon = Icons.Rounded.MyLocation, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "privateKey", title = PrefText.Res(R.string.ssh_private_key), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "peerPublicKey", title = PrefText.Res(R.string.wireguard_public_key), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "peerPreSharedKey", title = PrefText.Res(R.string.wireguard_psk), icon = Icons.Rounded.VpnKey, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "mtu", title = PrefText.Res(R.string.mtu), icon = Icons.Rounded.Straighten, summary = null, defaultValue = "1420", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "reserved", title = PrefText.Literal("Reserved"), icon = Icons.Rounded.BookmarkBorder, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Direct: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Tailscale: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "authKey", title = PrefText.Literal("Auth Key"), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "controlURL", title = PrefText.Literal("Control URL"), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "hostname", title = PrefText.Literal("Hostname"), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "stateDirectory", title = PrefText.Literal("State Directory"), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "ephemeral", title = PrefText.Literal("Ephemeral Node"), icon = Icons.Rounded.Bolt, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "acceptRoutes", title = PrefText.Literal("Accept Routes"), icon = Icons.Rounded.SyncAlt, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "exitNode", title = PrefText.Literal("Exit Node"), icon = Icons.Rounded.MyLocation, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "exitNodeAllowLANAccess", title = PrefText.Literal("Exit Node Allow LAN"), icon = Icons.Rounded.LockOpen, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "advertiseRoutes", title = PrefText.Literal("Advertise Routes"), icon = Icons.Rounded.Traffic, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "advertiseExitNode", title = PrefText.Literal("Advertise Exit Node"), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "advertiseTags", title = PrefText.Literal("Advertise Tags"), icon = Icons.Rounded.BookmarkBorder, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "listenPort", title = PrefText.Literal("Listen Port"), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "0", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Tailcat: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "privateKey", title = PrefText.Res(R.string.ssh_private_key), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPublicKey", title = PrefText.Literal("Server Public Key"), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverDiscoKey", title = PrefText.Literal("Server Disco Key"), icon = Icons.Rounded.VpnKey, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "preSharedKey", title = PrefText.Res(R.string.wireguard_psk), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "derpMapURL", title = PrefText.Literal("DERP Map URL"), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "derpRegion", title = PrefText.Literal("DERP Region"), icon = Icons.Rounded.MyLocation, summary = null, defaultValue = "0", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "derpServers", title = PrefText.Literal("DERP Servers"), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val MASQUE: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "443", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "username", title = PrefText.Res(R.string.username_opt), icon = Icons.Rounded.Person, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password_opt), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "path", title = PrefText.Literal("Path"), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "httpVersion", title = PrefText.Literal("HTTP Version (1/2/3)"), icon = Icons.Rounded.Traffic, summary = null, defaultValue = "3", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "disableVersionFallback", title = PrefText.Literal("Disable Version Fallback"), icon = Icons.Rounded.Bolt, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "mtu", title = PrefText.Res(R.string.mtu), icon = Icons.Rounded.Straighten, summary = null, defaultValue = "1280", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "enableTLS", title = PrefText.Literal("TLS"), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "sni", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "allowInsecure", title = PrefText.Res(R.string.allow_insecure), icon = Icons.Rounded.LockOpen, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "disableSNI", title = PrefText.Res(R.string.tuic_disable_sni), icon = Icons.Rounded.VisibilityOff, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "alpn", title = PrefText.Res(R.string.alpn), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "certificates", title = PrefText.Res(R.string.certificates), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "utlsFingerprint", title = PrefText.Literal("uTLS Fingerprint"), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "echConfig", title = PrefText.Literal("ECH Config"), icon = Icons.Rounded.VpnKey, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val OpenVPN: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "1194", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "network", title = PrefText.Literal("Network (udp / tcp)"), icon = Icons.Rounded.SyncAlt, summary = null, defaultValue = "udp", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "username", title = PrefText.Res(R.string.username_opt), icon = Icons.Rounded.Person, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password_opt), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "cipher", title = PrefText.Literal("Cipher"), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "dataCiphers", title = PrefText.Literal("Data Ciphers"), icon = Icons.Rounded.VpnKey, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "auth", title = PrefText.Literal("Auth Digest"), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "compression", title = PrefText.Literal("Compression"), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "redirectGateway", title = PrefText.Literal("Redirect Gateway"), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "mtu", title = PrefText.Res(R.string.mtu), icon = Icons.Rounded.Straighten, summary = null, defaultValue = "1500", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverName", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "certificate", title = PrefText.Literal("CA Certificate"), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "clientCertificate", title = PrefText.Literal("Client Certificate"), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "clientKey", title = PrefText.Literal("Client Private Key"), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "peerFingerprint", title = PrefText.Literal("Peer Fingerprint"), icon = Icons.Rounded.BookmarkBorder, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "controlWrapType", title = PrefText.Literal("Control Wrap (tls_auth / tls_crypt / tls_crypt_v2)"), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "controlWrapKey", title = PrefText.Literal("Control Wrap Key"), icon = Icons.Rounded.VpnKey, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "controlWrapDirection", title = PrefText.Literal("Control Wrap Direction (0 / 1)"), icon = Icons.Rounded.SyncAlt, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val OpenConnect: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "server", title = PrefText.Literal("Server URL / Host"), icon = Icons.Rounded.Dns, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "flavor", title = PrefText.Literal("Protocol Flavor (anyconnect / gp / pulse / fortinet)"), icon = Icons.Rounded.Layers, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "username", title = PrefText.Res(R.string.username_opt), icon = Icons.Rounded.Person, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password_opt), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "authGroup", title = PrefText.Literal("Auth Group"), icon = Icons.Rounded.BookmarkBorder, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "reportedOS", title = PrefText.Literal("Reported OS"), icon = Icons.Rounded.SettingsInputComponent, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "userAgent", title = PrefText.Literal("User Agent"), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "allowInsecureCrypto", title = PrefText.Literal("Allow Insecure Crypto"), icon = Icons.Rounded.LockOpen, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.SWITCH, key = "tlsInsecure", title = PrefText.Res(R.string.allow_insecure), icon = Icons.Rounded.LockOpen, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "tlsServerName", title = PrefText.Res(R.string.sni), icon = Icons.Rounded.Public, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "tlsPeerFingerprint", title = PrefText.Literal("TLS Peer Fingerprint"), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "certificateAuthority", title = PrefText.Literal("CA Certificate"), icon = Icons.Rounded.WorkspacePremium, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "clientCertificate", title = PrefText.Literal("Client Certificate"), icon = Icons.Rounded.Badge, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "clientKey", title = PrefText.Literal("Client Private Key"), icon = Icons.Rounded.Key, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
                PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "clientKeyPassword", title = PrefText.Literal("Client Key Password"), icon = Icons.Rounded.Password, summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

}
