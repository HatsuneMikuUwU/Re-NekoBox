package io.nekohasekai.sagernet.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jakewharton.processphoenix.ProcessPhoenix
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.background.screenContainerColor
import io.nekohasekai.sagernet.ui.components.AppDialog
import io.nekohasekai.sagernet.ui.components.DialogCancelButton
import io.nekohasekai.sagernet.ui.components.DialogConfirmButton
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressiveIconBadge
import io.nekohasekai.sagernet.ui.components.ExpressivePillBadge
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.hapticBooleanChange
import io.nekohasekai.sagernet.ui.components.hapticClick
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.update.CoreChoice
import io.nekohasekai.sagernet.update.CoreRuntime
import io.nekohasekai.sagernet.update.CoreUpdate
import io.nekohasekai.sagernet.update.UpdateMessages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import libcore.Libcore

class KernelManagerActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val activeCore = remember { CoreRuntime.active }

            var channel by rememberSaveable { mutableStateOf(activeCore.channel) }
            var selectedChoice by remember { mutableStateOf<CoreChoice?>(null) }
            var availableUpdate by remember { mutableStateOf<CoreUpdate?>(null) }
            var downloadSameVersion by remember { mutableStateOf(false) }
            var statusMessage by remember {
                mutableStateOf(if (CoreRuntime.recovered) context.getString(R.string.core_recovered) else "")
            }
            var busy by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableIntStateOf(-1) }
            var showBoxVersionDialog by remember { mutableStateOf(false) }
            var pendingRestartRestore by remember { mutableStateOf<Boolean?>(null) }

            fun formatChoice(choice: CoreChoice): String {
                val chLabel = context.getString(if (choice.channel == "preview") R.string.core_preview else R.string.core_stable)
                val srcLabel = context.getString(if (choice.installed) R.string.core_downloaded else R.string.core_builtin)
                return context.getString(R.string.core_status, chLabel, choice.version, choice.revision, srcLabel)
            }

            fun refreshSelected(targetChannel: String = channel) {
                scope.launch {
                    val choice = withContext(Dispatchers.IO) { CoreRuntime.choice(targetChannel) }
                    if (targetChannel == channel) {
                        selectedChoice = choice
                    }
                }
            }

            LaunchedEffect(channel) {
                availableUpdate = null
                downloadSameVersion = false
                refreshSelected(channel)
            }

            fun checkUpdates() {
                if (busy) return
                val targetChannel = channel
                busy = true
                statusMessage = context.getString(R.string.update_checking)
                scope.launch {
                    try {
                        val candidate = withContext(Dispatchers.IO) { CoreRuntime.check(targetChannel) }
                        val current = withContext(Dispatchers.IO) { CoreRuntime.choice(targetChannel) }
                        if (targetChannel == channel) {
                            selectedChoice = current
                            availableUpdate = candidate
                            val same = candidate?.let {
                                it.manifest.version == current.version && it.manifest.revision == current.revision
                            } == true
                            downloadSameVersion = same
                            statusMessage = when {
                                same -> context.getString(R.string.core_current)
                                candidate != null -> context.getString(R.string.core_available, candidate.manifest.displayVersion)
                                else -> context.getString(R.string.core_latest)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        statusMessage = context.getString(UpdateMessages.resource(e))
                    } finally {
                        busy = false
                    }
                }
            }

            fun downloadUpdate() {
                val candidate = availableUpdate ?: return
                if (busy) return
                busy = true
                downloadProgress = 0
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            CoreRuntime.install(candidate) { percent ->
                                downloadProgress = percent
                                statusMessage = context.getString(R.string.update_downloading, percent)
                            }
                        }
                        availableUpdate = null
                        downloadSameVersion = false
                        statusMessage = context.getString(R.string.core_ready)
                        refreshSelected()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        statusMessage = context.getString(UpdateMessages.resource(e))
                    } finally {
                        busy = false
                        downloadProgress = -1
                    }
                }
            }

            fun applyAndRestart(restoreBundled: Boolean) {
                if (busy) return
                val appContext = context.applicationContext
                val targetChannel = channel
                busy = true
                scope.launch {
                    try {
                        if (DataStore.serviceState.canStop) SagerNet.stopService()
                        withTimeout(15000) {
                            while (DataStore.serviceState != BaseService.State.Stopped) delay(100)
                        }
                        withContext(Dispatchers.IO) { CoreRuntime.select(targetChannel, restoreBundled) }
                        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        manager.runningAppProcesses
                            ?.filter { it.uid == Process.myUid() && it.processName == "${appContext.packageName}:bg" }
                            ?.forEach { Process.killProcess(it.pid) }
                        ProcessPhoenix.triggerRebirth(
                            appContext,
                            Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } catch (_: TimeoutCancellationException) {
                        statusMessage = context.getString(R.string.update_stop_failed)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        statusMessage = context.getString(UpdateMessages.resource(e))
                    } finally {
                        busy = false
                    }
                }
            }

            val backdrop = rememberBlurBackdrop()
            val topInset = floatingTopBarSpace()
            val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = topInset + 16.dp,
                            bottom = navBarInset + 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Running Core summary card
                        item {
                            ExpressiveSurfaceItem(
                                position = ExpressiveGroupPosition.SINGLE,
                                onClick = { showBoxVersionDialog = true },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    ExpressiveIconBadge(
                                        icon = Icons.Rounded.Memory,
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.core_running),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = "sing-box ${activeCore.version}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        ExpressivePillBadge(
                                            text = formatChoice(activeCore),
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // Channel selector + actions card
                        item {
                            ExpressiveSurfaceItem(position = ExpressiveGroupPosition.SINGLE) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.core_channel_help),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    ) {
                                        ToggleButton(
                                            checked = channel == "stable",
                                            enabled = !busy,
                                            onCheckedChange = hapticBooleanChange {
                                                if (it && channel != "stable") {
                                                    channel = "stable"
                                                    statusMessage = ""
                                                }
                                            },
                                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                            colors = ToggleButtonDefaults.colors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            ),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.core_stable))
                                        }
                                        ToggleButton(
                                            checked = channel == "preview",
                                            enabled = !busy,
                                            onCheckedChange = hapticBooleanChange {
                                                if (it && channel != "preview") {
                                                    channel = "preview"
                                                    statusMessage = ""
                                                }
                                            },
                                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                            colors = ToggleButtonDefaults.colors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            ),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.core_preview))
                                        }
                                    }

                                    selectedChoice?.let { choice ->
                                        Text(
                                            text = formatChoice(choice),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }

                                    if (downloadProgress in 0..100) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress / 100f },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }

                                    if (statusMessage.isNotBlank()) {
                                        Text(
                                            text = statusMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        FilledTonalButton(
                                            onClick = hapticClick { checkUpdates() },
                                            enabled = !busy,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.core_check), maxLines = 1)
                                        }
                                        if (availableUpdate != null) {
                                            Button(
                                                onClick = hapticClick { downloadUpdate() },
                                                enabled = !busy,
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    stringResource(
                                                        if (downloadSameVersion) R.string.core_reinstall else R.string.core_download,
                                                    ),
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = hapticClick { pendingRestartRestore = false },
                                        enabled = !busy,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.core_switch))
                                    }

                                    OutlinedButton(
                                        onClick = hapticClick { pendingRestartRestore = true },
                                        enabled = !busy,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.core_restore))
                                    }
                                }
                            }
                        }
                    }
                }

                FloatingTopBar(
                    backdrop = backdrop,
                    title = stringResource(R.string.kernel_manager),
                    onBack = { finish() },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            if (showBoxVersionDialog) {
                AppDialog(
                    icon = Icons.Rounded.Memory,
                    title = stringResource(R.string.core_running),
                    onDismiss = { showBoxVersionDialog = false },
                    text = { Text(Libcore.versionBox()) },
                    confirmButton = {
                        DialogConfirmButton(stringResource(android.R.string.ok)) {
                            showBoxVersionDialog = false
                        }
                    },
                )
            }

            pendingRestartRestore?.let { restore ->
                val chLabel = stringResource(if (channel == "preview") R.string.core_preview else R.string.core_stable)
                AppDialog(
                    icon = Icons.Rounded.RestartAlt,
                    title = stringResource(R.string.core_restart_title),
                    onDismiss = { pendingRestartRestore = null },
                    text = { Text(stringResource(R.string.core_restart_message, chLabel)) },
                    confirmButton = {
                        DialogConfirmButton(stringResource(R.string.core_switch)) {
                            pendingRestartRestore = null
                            applyAndRestart(restore)
                        }
                    },
                    dismissButton = {
                        DialogCancelButton(stringResource(android.R.string.cancel)) {
                            pendingRestartRestore = null
                        }
                    },
                )
            }
        }
    }
}
