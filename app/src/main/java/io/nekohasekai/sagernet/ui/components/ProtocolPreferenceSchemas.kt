@file:Suppress("DEPRECATION")

package io.nekohasekai.sagernet.ui.components

import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.material.icons.rounded.Router
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

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
                            val enabled = item.dependency.isNullOrBlank() || store.getBoolean(item.dependency, false)
                            val titleStr = item.title.resolve()
                            val summaryStr = item.summary?.resolve()

                            when (item.type) {
                                PrefFieldType.SWITCH -> {
                                    val def = item.defaultValue.equals("true", ignoreCase = true)
                                    PrefSwitchRow(
                                        title = titleStr,
                                        summary = summaryStr,
                                        icon = iconForProtocolField(item.key),
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
                                        icon = Icons.Rounded.Code,
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
                                    val entries = item.entriesRes?.let { stringArrayResource(it).toList() } ?: emptyList()
                                    val values = item.entryValuesRes?.let { stringArrayResource(it).toList() } ?: entries
                                    StoreListPrefRaw(
                                        store = store,
                                        key = item.key,
                                        title = titleStr,
                                        entries = entries,
                                        entryValues = values,
                                        defaultValue = item.defaultValue,
                                        icon = iconForProtocolField(item.key),
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
                                        icon = iconForProtocolField(item.key),
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

private fun iconForProtocolField(key: String): ImageVector {
    val k = key.lowercase()
    return when {
        k == "name" || k == "profilename" -> Icons.Rounded.Title
        k.contains("address") || k == "host" || k == "serverhost" || k == "obfshost" -> Icons.Rounded.Dns
        k.contains("port") -> Icons.Rounded.Numbers
        k.contains("allowinsecure") || k.contains("insecure") -> Icons.Rounded.LockOpen
        k.contains("password") || k.contains("privatekey") || k.contains("pubkey") || k.contains("publickey") || k.contains("psk") || k.contains("presharedkey") || k.contains("userkey") || k.contains("shortid") -> Icons.Rounded.Key
        k.contains("uuid") || k.contains("userid") || k.contains("username") || k.contains("alterid") -> Icons.Rounded.Person
        k.contains("cert") || k.contains("pinned") -> Icons.Rounded.Security
        k.contains("sni") || k.contains("alpn") || k.contains("utls") || k.contains("ech") || k.contains("fingerprint") -> Icons.Rounded.VerifiedUser
        k.contains("encrypt") || k.contains("method") || k.contains("security") || k.contains("auth") -> Icons.Rounded.Lock
        k.contains("upload") || k.contains("download") -> Icons.Rounded.SwapVert
        k.contains("mux") || k.contains("brutal") || k.contains("concurrency") || k.contains("streams") || k.contains("reuse") -> Icons.Rounded.CallSplit
        k.contains("mtu") || k.contains("kcp") || k.contains("quic") || k.contains("congestion") || k.contains("window") || k.contains("rtt") || k.contains("hop") || k.contains("udp") || k.contains("suot") || k.contains("packet") -> Icons.Rounded.Speed
        k.contains("plugin") || k.contains("config") || k.contains("extra") -> Icons.Rounded.Extension
        k.contains("local") || k.contains("reserved") || k.contains("outbound") -> Icons.Rounded.Router
        k.contains("network") || k.contains("protocol") || k.contains("type") || k.contains("mode") || k.contains("obfs") || k.contains("path") || k.contains("header") || k.contains("earlydata") -> Icons.Rounded.Hub
        else -> Icons.Rounded.Tune
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
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.security_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "sni", title = PrefText.Res(R.string.sni), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "alpn", title = PrefText.Res(R.string.alpn), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "certificates", title = PrefText.Res(R.string.certificates), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "allowInsecure", title = PrefText.Res(R.string.allow_insecure), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.tls_camouflage_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "utlsFingerprint", title = PrefText.Res(R.string.utls_fingerprint), summary = null, defaultValue = "", entriesRes = R.array.utls_fingerprint_entry, entryValuesRes = R.array.utls_fingerprint_entry, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "realityPubKey", title = PrefText.Literal("Reality Public Key"), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "realityShortId", title = PrefText.Literal("Reality ShortId"), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Config: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "isOutboundOnly", title = PrefText.Res(R.string.is_outbound_only), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.CONFIG, key = "serverConfig", title = PrefText.Res(R.string.custom_config), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Hysteria: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "protocolVersion", title = PrefText.Res(R.string.protocol_version), summary = null, defaultValue = "2", entriesRes = R.array.hysteria_version, entryValuesRes = R.array.hysteria_version, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPorts", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverObfs", title = PrefText.Res(R.string.hysteria_obfs), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverAuthType", title = PrefText.Res(R.string.hysteria_auth_type), summary = null, defaultValue = "", entriesRes = R.array.hysteria_auth_type, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.hysteria_auth_payload), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverProtocol", title = PrefText.Res(R.string.protocol), summary = null, defaultValue = "https", entriesRes = R.array.hysteria_protocol, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverSNI", title = PrefText.Res(R.string.sni), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverALPN", title = PrefText.Res(R.string.alpn), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverCertificates", title = PrefText.Res(R.string.certificates), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverAllowInsecure", title = PrefText.Res(R.string.allow_insecure), summary = PrefText.Res(R.string.allow_insecure_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUploadSpeed", title = PrefText.Res(R.string.hysteria_upload_mbps), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverDownloadSpeed", title = PrefText.Res(R.string.hysteria_download_mbps), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverStreamReceiveWindow", title = PrefText.Res(R.string.hysteria_stream_receive_window), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverConnectionReceiveWindow", title = PrefText.Res(R.string.hysteria_connection_receive_window), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverDisableMtuDiscovery", title = PrefText.Res(R.string.hysteria_disable_mtu_discovery), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "hopInterval", title = PrefText.Res(R.string.hop_interval), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Juicity: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUserId", title = PrefText.Res(R.string.uuid), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverSNI", title = PrefText.Res(R.string.sni), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPinnedCertChainSha256", title = PrefText.Res(R.string.pinned_certchain_sha256), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverAllowInsecure", title = PrefText.Res(R.string.allow_insecure), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Mieru: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverProtocol", title = PrefText.Res(R.string.protocol), summary = null, defaultValue = "TCP", entriesRes = R.array.mieru_protocol, entryValuesRes = R.array.mieru_protocol, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUsername", title = PrefText.Res(R.string.username), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverMTU", title = PrefText.Res(R.string.mtu), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Naive: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUsername", title = PrefText.Res(R.string.username_opt), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password_opt), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverProtocol", title = PrefText.Res(R.string.protocol), summary = null, defaultValue = "https", entriesRes = R.array.naive_proto_entry, entryValuesRes = R.array.naive_proto_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverHeaders", title = PrefText.Res(R.string.extra_headers), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverSNI", title = PrefText.Res(R.string.sni), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverCertificates", title = PrefText.Res(R.string.certificates), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverInsecureConcurrency", title = PrefText.Res(R.string.naive_insecure_concurrency), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Literal("sing-box server"),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "sUoT", title = PrefText.Literal("UDP over TCP"), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Shadowsocks: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "method", title = PrefText.Res(R.string.enc_method), summary = null, defaultValue = "", entriesRes = R.array.ss_enc_method_value, entryValuesRes = R.array.ss_enc_method_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.plugin),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "pluginName", title = PrefText.Res(R.string.plugin), summary = null, defaultValue = "", entriesRes = R.array.box_shadowsocks_plugins, entryValuesRes = R.array.box_shadowsocks_plugins, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "pluginConfig", title = PrefText.Res(R.string.plugin_configure), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Literal("sing-box server"),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "sUoT", title = PrefText.Literal("UDP over TCP"), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverMuxCategory",
            title = PrefText.Res(R.string.mux_preference),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "enableMux", title = PrefText.Res(R.string.enable_mux), summary = PrefText.Res(R.string.mux_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "muxType", title = PrefText.Res(R.string.mux_type), summary = null, defaultValue = "0", entriesRes = R.array.mux_type, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "muxMode", title = PrefText.Res(R.string.mux_mode), summary = null, defaultValue = "0", entriesRes = R.array.mux_mode, entryValuesRes = R.array.int_array_2, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxConcurrency", title = PrefText.Res(R.string.mux_concurrency), summary = null, defaultValue = "8", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxMaxConnections", title = PrefText.Res(R.string.mux_max_connections), summary = null, defaultValue = "4", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxMinStreams", title = PrefText.Res(R.string.mux_min_streams), summary = null, defaultValue = "4", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "muxPadding", title = PrefText.Res(R.string.padding), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "muxBrutal", title = PrefText.Res(R.string.mux_brutal), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxBrutalUpMbps", title = PrefText.Res(R.string.mux_brutal_up_mbps), summary = null, defaultValue = "100", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxBrutalDownMbps", title = PrefText.Res(R.string.mux_brutal_down_mbps), summary = null, defaultValue = "100", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val ShadowsocksR: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverMethod", title = PrefText.Res(R.string.enc_method), summary = null, defaultValue = "", entriesRes = R.array.ssr_enc_method_value, entryValuesRes = R.array.ssr_enc_method_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverProtocol", title = PrefText.Res(R.string.protocol), summary = null, defaultValue = "", entriesRes = R.array.ssr_protocol_value, entryValuesRes = R.array.ssr_protocol_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverProtocolParam", title = PrefText.Res(R.string.protocol_param), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverObfs", title = PrefText.Res(R.string.obfs), summary = null, defaultValue = "", entriesRes = R.array.ssr_obfs_value, entryValuesRes = R.array.ssr_obfs_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverObfsParam", title = PrefText.Res(R.string.obfs_param), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val ShadowTLS: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "version", title = PrefText.Res(R.string.protocol_version), summary = null, defaultValue = "", entriesRes = R.array.shadowtls_version_value, entryValuesRes = R.array.shadowtls_version_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverSecurityCategory",
            title = PrefText.Res(R.string.security_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "sni", title = PrefText.Res(R.string.sni), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "alpn", title = PrefText.Res(R.string.alpn), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "certificates", title = PrefText.Res(R.string.certificates), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "allowInsecure", title = PrefText.Res(R.string.allow_insecure), summary = PrefText.Res(R.string.allow_insecure_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "utlsFingerprint", title = PrefText.Res(R.string.utls_fingerprint), summary = null, defaultValue = "", entriesRes = R.array.utls_fingerprint_entry, entryValuesRes = R.array.utls_fingerprint_entry, dependency = null),
            ),
        ),
    )

    val Snell: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "psk", title = PrefText.Res(R.string.snell_psk), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "userKey", title = PrefText.Res(R.string.snell_userkey), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "version", title = PrefText.Res(R.string.snell_version), summary = null, defaultValue = "4", entriesRes = R.array.snell_versions_entry, entryValuesRes = R.array.snell_versions_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "network", title = PrefText.Res(R.string.snell_network), summary = null, defaultValue = "", entriesRes = R.array.snell_network_entry, entryValuesRes = R.array.snell_network_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "obfsMode", title = PrefText.Res(R.string.snell_obfs_mode), summary = null, defaultValue = "", entriesRes = R.array.snell_obfs_modes_entry, entryValuesRes = R.array.snell_obfs_modes_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "obfsHost", title = PrefText.Res(R.string.snell_obfs_host), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "mode", title = PrefText.Res(R.string.snell_mode), summary = null, defaultValue = "default", entriesRes = R.array.snell_v6_modes_entry, entryValuesRes = R.array.snell_v6_modes_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "quicProxyMode", title = PrefText.Res(R.string.snell_quic_proxy), summary = PrefText.Res(R.string.snell_quic_proxy_summary), defaultValue = "false", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "reuse", title = PrefText.Res(R.string.snell_reuse), summary = PrefText.Res(R.string.snell_reuse_summary), defaultValue = "false", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Socks: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverProtocol", title = PrefText.Res(R.string.app_version), summary = null, defaultValue = "2", entriesRes = R.array.socks_versions, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUsername", title = PrefText.Res(R.string.username_opt), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password_opt), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Literal("sing-box server"),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "sUoT", title = PrefText.Literal("UDP over TCP"), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val SSH: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUsername", title = PrefText.Res(R.string.username), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverAuthType", title = PrefText.Res(R.string.hysteria_auth_type), summary = null, defaultValue = "", entriesRes = R.array.ssh_auth_type, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPrivateKey", title = PrefText.Res(R.string.ssh_private_key), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword1", title = PrefText.Res(R.string.ssh_private_key_passphrase), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverCertificates", title = PrefText.Res(R.string.ssh_public_key), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val StandardV2Ray: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "username", title = PrefText.Res(R.string.username_opt), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "password", title = PrefText.Res(R.string.password_opt), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "uuid", title = PrefText.Res(R.string.uuid), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "alterId", title = PrefText.Res(R.string.alter_id), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "encryption", title = PrefText.Res(R.string.encryption), summary = null, defaultValue = "", entriesRes = R.array.vmess_encryption_value, entryValuesRes = R.array.vmess_encryption_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "vlessEncryption", title = PrefText.Res(R.string.vless_encryption), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "packetEncoding", title = PrefText.Res(R.string.packet_encoding), summary = null, defaultValue = "", entriesRes = R.array.packet_encoding_entry, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "type", title = PrefText.Res(R.string.network), summary = null, defaultValue = "", entriesRes = R.array.networks_value, entryValuesRes = R.array.networks_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "host", title = PrefText.Res(R.string.http_host), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "path", title = PrefText.Res(R.string.http_path), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "headerType", title = PrefText.Res(R.string.kcp_header_type), summary = null, defaultValue = "none", entriesRes = R.array.kcp_headers_value, entryValuesRes = R.array.kcp_headers_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "kcpCwndMultiplier", title = PrefText.Res(R.string.kcp_cwnd_multiplier), summary = null, defaultValue = "20", entriesRes = R.array.kcp_cwnd_multiplier_entry, entryValuesRes = R.array.kcp_cwnd_multiplier_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "mKcpSeed", title = PrefText.Res(R.string.kcp_seed), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "kcpMtu", title = PrefText.Res(R.string.kcp_mtu), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "kcpTti", title = PrefText.Res(R.string.kcp_tti), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "security", title = PrefText.Res(R.string.security), summary = null, defaultValue = "", entriesRes = R.array.transport_layer_encryption_value, entryValuesRes = R.array.transport_layer_encryption_value, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverWsCategory",
            title = PrefText.Res(R.string.cag_ws),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "wsMaxEarlyData", title = PrefText.Res(R.string.ws_max_early_data), summary = null, defaultValue = "0", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "earlyDataHeaderName", title = PrefText.Res(R.string.early_data_header_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverXhttpCategory",
            title = PrefText.Res(R.string.xhttp_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "xhttpMode", title = PrefText.Res(R.string.xhttp_mode), summary = null, defaultValue = "auto", entriesRes = R.array.xhttp_mode_entry, entryValuesRes = R.array.xhttp_mode_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "xhttpExtra", title = PrefText.Res(R.string.xhttp_extra), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverSecurityCategory",
            title = PrefText.Res(R.string.security_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "sni", title = PrefText.Res(R.string.sni), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "alpn", title = PrefText.Res(R.string.alpn), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "certificates", title = PrefText.Res(R.string.certificates), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "allowInsecure", title = PrefText.Res(R.string.allow_insecure), summary = PrefText.Res(R.string.allow_insecure_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverTlsCamouflageCategory",
            title = PrefText.Res(R.string.tls_camouflage_settings),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "utlsFingerprint", title = PrefText.Res(R.string.utls_fingerprint), summary = null, defaultValue = "", entriesRes = R.array.utls_fingerprint_entry, entryValuesRes = R.array.utls_fingerprint_entry, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "realityPubKey", title = PrefText.Literal("Reality Public Key"), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "realityShortId", title = PrefText.Literal("Reality ShortId"), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverMuxCategory",
            title = PrefText.Res(R.string.mux_preference),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "enableMux", title = PrefText.Res(R.string.enable_mux), summary = PrefText.Res(R.string.mux_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "muxType", title = PrefText.Res(R.string.mux_type), summary = null, defaultValue = "0", entriesRes = R.array.mux_type, entryValuesRes = R.array.int_array_3, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "muxMode", title = PrefText.Res(R.string.mux_mode), summary = null, defaultValue = "0", entriesRes = R.array.mux_mode, entryValuesRes = R.array.int_array_2, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxConcurrency", title = PrefText.Res(R.string.mux_concurrency), summary = null, defaultValue = "8", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxMaxConnections", title = PrefText.Res(R.string.mux_max_connections), summary = null, defaultValue = "4", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxMinStreams", title = PrefText.Res(R.string.mux_min_streams), summary = null, defaultValue = "4", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "muxPadding", title = PrefText.Res(R.string.padding), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "muxBrutal", title = PrefText.Res(R.string.mux_brutal), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxBrutalUpMbps", title = PrefText.Res(R.string.mux_brutal_up_mbps), summary = null, defaultValue = "100", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "muxBrutalDownMbps", title = PrefText.Res(R.string.mux_brutal_down_mbps), summary = null, defaultValue = "100", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverECHCategory",
            title = PrefText.Literal("ECH"),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "enableECH", title = PrefText.Res(R.string.enable), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "echConfig", title = PrefText.Res(R.string.ech_config), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val TrojanGo: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverSNI", title = PrefText.Res(R.string.sni), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverAllowInsecure", title = PrefText.Res(R.string.allow_insecure), summary = PrefText.Res(R.string.allow_insecure_sum), defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverNetwork", title = PrefText.Res(R.string.network), summary = null, defaultValue = "none", entriesRes = R.array.trojan_go_networks_entry, entryValuesRes = R.array.trojan_go_networks_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverEncryption", title = PrefText.Res(R.string.encryption), summary = null, defaultValue = "", entriesRes = R.array.trojan_go_security_entry, entryValuesRes = R.array.trojan_go_security_value, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverWsCategory",
            title = PrefText.Res(R.string.cag_ws),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverHost", title = PrefText.Res(R.string.ws_host), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPath", title = PrefText.Res(R.string.ws_path), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = "serverSsCategory",
            title = PrefText.Res(R.string.ss_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverMethod", title = PrefText.Res(R.string.enc_method), summary = null, defaultValue = "AES-128-GCM", entriesRes = R.array.trojan_go_methods, entryValuesRes = R.array.trojan_go_methods, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword1", title = PrefText.Res(R.string.password), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val Tuic: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "profileName", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverUsername", title = PrefText.Res(R.string.uuid), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPassword", title = PrefText.Res(R.string.password), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverALPN", title = PrefText.Res(R.string.alpn), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverCertificates", title = PrefText.Res(R.string.certificates), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverUDPRelayMode", title = PrefText.Res(R.string.tuic_udp_relay_mode), summary = null, defaultValue = "https", entriesRes = R.array.tuic_udp_relay_mode_value, entryValuesRes = R.array.tuic_udp_relay_mode_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.LIST, key = "serverCongestionController", title = PrefText.Res(R.string.tuic_congestion_controller), summary = null, defaultValue = "https", entriesRes = R.array.tuic_congestion_controller_value, entryValuesRes = R.array.tuic_congestion_controller_value, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverDisableSNI", title = PrefText.Res(R.string.tuic_disable_sni), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverSNI", title = PrefText.Res(R.string.sni), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverReduceRTT", title = PrefText.Res(R.string.tuic_reduce_rtt), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.SWITCH, key = "serverAllowInsecure", title = PrefText.Res(R.string.allow_insecure), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

    val WireGuard: List<PrefSectionSpec> = listOf(
        PrefSectionSpec(
            key = null,
            title = null,
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "name", title = PrefText.Res(R.string.profile_name), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
        PrefSectionSpec(
            key = null,
            title = PrefText.Res(R.string.proxy_cat),
            items = listOf(
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverAddress", title = PrefText.Res(R.string.server_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "serverPort", title = PrefText.Res(R.string.server_port), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "localAddress", title = PrefText.Res(R.string.wireguard_local_address), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "privateKey", title = PrefText.Res(R.string.ssh_private_key), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "peerPublicKey", title = PrefText.Res(R.string.wireguard_public_key), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "peerPreSharedKey", title = PrefText.Res(R.string.wireguard_psk), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "mtu", title = PrefText.Res(R.string.mtu), summary = null, defaultValue = "1420", entriesRes = null, entryValuesRes = null, dependency = null),
            PrefFieldSpec(type = PrefFieldType.EDIT_TEXT, key = "reserved", title = PrefText.Literal("Reserved"), summary = null, defaultValue = "", entriesRes = null, entryValuesRes = null, dependency = null),
            ),
        ),
    )

}
