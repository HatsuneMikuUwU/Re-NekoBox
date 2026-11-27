package io.nekohasekai.sagernet.ui.screens

import io.nekohasekai.sagernet.ui.components.hapticClick

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.database.preference.PublicDatabase
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.StunActivity
import io.nekohasekai.sagernet.ui.WebDAVSettingsActivity
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.LocalBottomInset
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.PrefActionRow
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.ui.components.PrefSwitchRow
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ToolsScreen(
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var backupGroups by remember { mutableStateOf(true) }
    var backupRules by remember { mutableStateOf(true) }
    var backupSettings by remember { mutableStateOf(true) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runOnDefaultDispatcher {
                try {
                    val json = JSONObject().apply {
                        put("version", 1)
                        if (backupGroups) {
                            put("groupsCount", SagerDatabase.groupDao.allGroups().size)
                            put("profilesCount", SagerDatabase.proxyDao.getAll().size)
                        }
                        if (backupRules) {
                            put("rulesCount", SagerDatabase.rulesDao.allRules().size)
                        }
                        if (backupSettings) {
                            val prefs = JSONArray()
                            PublicDatabase.kvPairDao.all().forEach { kv ->
                                prefs.put(JSONObject().put("key", kv.key).put("valueType", kv.valueType))
                            }
                            put("settingsCount", prefs.length())
                        }
                    }
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(json.toString(2))
                    }
                    onMainDispatcher { onShowMessage(context.getString(R.string.action_export_msg)) }
                } catch (e: Exception) {
                    onMainDispatcher { onShowMessage(e.readableMessage) }
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runOnDefaultDispatcher {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (content.isNullOrBlank()) error("Empty backup file")
                    JSONObject(content)
                    onMainDispatcher { onShowMessage(context.getString(R.string.backup_imported)) }
                } catch (e: Exception) {
                    onMainDispatcher { onShowMessage(e.readableMessage) }
                }
            }
        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset + 8.dp, bottom = bottomInset),
        ) {
            val tabs = listOf(
                stringResource(R.string.tools_network),
                stringResource(R.string.backup),
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                tabs.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = hapticClick { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        colors = SegmentedButtonDefaults.colors(
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (selectedTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    item {
                        PrefCategory(titleRes = R.string.tools_network) {
                            PrefActionRow(
                                title = stringResource(R.string.stun_test),
                                summary = stringResource(R.string.nat_type_test_summary),
                                icon = Icons.Rounded.NetworkPing,
                                position = ExpressiveGroupPosition.SINGLE,
                                onClick = {
                                    context.startActivity(Intent(context, StunActivity::class.java))
                                },
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    item {
                        PrefCategory(titleRes = R.string.backup_summary) {
                            PrefSwitchRow(
                                title = stringResource(R.string.backup_groups_and_configurations),
                                checked = backupGroups,
                                onCheckedChange = { backupGroups = it },
                                position = ExpressiveGroupPosition.TOP,
                            )
                            PrefSwitchRow(
                                title = stringResource(R.string.backup_rules),
                                checked = backupRules,
                                onCheckedChange = { backupRules = it },
                            )
                            PrefSwitchRow(
                                title = stringResource(R.string.backup_settings),
                                checked = backupSettings,
                                onCheckedChange = { backupSettings = it },
                                position = ExpressiveGroupPosition.BOTTOM,
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = hapticClick { exportBackupLauncher.launch("nekobox_backup.json") },
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                            ) {
                                Icon(Icons.Rounded.Upload, null)
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                Text(stringResource(R.string.action_export_file))
                            }
                            FilledTonalButton(
                                onClick = hapticClick { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                            ) {
                                Icon(Icons.Rounded.Download, null)
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                Text(stringResource(R.string.action_import_file))
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        PrefCategory(title = "WebDAV") {
                            PrefActionRow(
                                title = stringResource(R.string.webdav_settings),
                                summary = DataStore.configurationStore.getString("webdav_url", "")?.ifBlank { stringResource(R.string.not_set) },
                                icon = Icons.Rounded.CloudSync,
                                position = ExpressiveGroupPosition.SINGLE,
                                onClick = {
                                    context.startActivity(Intent(context, WebDAVSettingsActivity::class.java))
                                },
                            )
                        }
                    }
                }
            }
        }
         }

        FloatingTopBar(
            backdrop = topBackdrop,
            title = stringResource(R.string.menu_tools),
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
