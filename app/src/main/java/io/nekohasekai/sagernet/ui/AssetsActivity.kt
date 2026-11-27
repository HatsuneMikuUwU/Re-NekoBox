package io.nekohasekai.sagernet.ui

import android.os.Bundle
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressiveIconBadge
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import java.io.File

class AssetsActivity : ThemedActivity() {

    private val builtInAssets = setOf("geoip.db", "geosite.db")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            val context = LocalContext.current
            val assetsDir = remember { getExternalFilesDir(null) ?: filesDir }

            fun loadAssets(): List<File> {
                val custom = assetsDir.listFiles()
                    ?.filter {
                        it.isFile &&
                            it.name !in builtInAssets &&
                            (it.name.endsWith(".db") || it.name.endsWith(".srs") || it.name.endsWith(".dat"))
                    }
                    ?.sortedBy { it.name }
                    ?: emptyList()
                return listOf(File(assetsDir, "geoip.db"), File(assetsDir, "geosite.db")) + custom
            }

            var files by remember { mutableStateOf(loadAssets()) }

            fun reload() {
                files = loadAssets()
            }

            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    runOnDefaultDispatcher {
                        try {
                            val displayName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                            }?.takeIf { it.isNotBlank() }
                                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfter(':')
                                ?: "custom_rule.db"
                            val outFile = File(assetsDir, displayName)
                            contentResolver.openInputStream(uri)?.use { input ->
                                outFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            onMainDispatcher {
                                reload()
                                showComposeSnackbar("Imported $displayName")
                            }
                        } catch (e: Exception) {
                            onMainDispatcher { showComposeSnackbar(e.readableMessage) }
                        }
                    }
                }
            }

            val backdrop = rememberBlurBackdrop()
            val topInset = floatingTopBarSpace()
            val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = topInset + 16.dp,
                            bottom = navBarInset + 16.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(files, key = { _, f -> f.absolutePath }) { idx, file ->
                            val pos = when {
                                files.size == 1 -> ExpressiveGroupPosition.SINGLE
                                idx == 0 -> ExpressiveGroupPosition.TOP
                                idx == files.lastIndex -> ExpressiveGroupPosition.BOTTOM
                                else -> ExpressiveGroupPosition.MIDDLE
                            }
                            val isBuiltIn = file.name in builtInAssets
                            val versionFile = File(assetsDir, "${file.nameWithoutExtension}.version.txt")
                            val versionText = remember(file.lastModified(), versionFile.lastModified()) {
                                if (versionFile.isFile) versionFile.readText().trim().takeIf { it.isNotBlank() } else null
                            }
                            val subtitle = when {
                                !file.exists() -> stringResource(R.string.not_set)
                                versionText != null -> "$versionText • ${Formatter.formatFileSize(context, file.length())}"
                                else -> Formatter.formatFileSize(context, file.length())
                            }
                            ExpressiveSurfaceItem(position = pos) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    ExpressiveIconBadge(icon = Icons.Rounded.FolderZip)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (!isBuiltIn && file.exists()) {
                                        IconButton(onClick = {
                                            file.delete()
                                            reload()
                                        }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FloatingTopBar(
                    backdrop = backdrop,
                    title = stringResource(R.string.route_manage_assets),
                    onBack = { finish() },
                    actions = {
                        IconButton(onClick = { reload() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Rounded.FileOpen, contentDescription = stringResource(R.string.action_import_file))
                        }
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                SnackbarHost(
                    hostState = composeSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}
