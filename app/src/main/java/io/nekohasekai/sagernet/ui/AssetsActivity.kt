package io.nekohasekai.sagernet.ui

import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
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
import io.nekohasekai.sagernet.ui.components.ExpressiveEmptyState
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressiveIconBadge
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import java.io.File

class AssetsActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            val context = LocalContext.current
            val assetsDir = remember { getExternalFilesDir(null) ?: filesDir }
            var files by remember { mutableStateOf<List<File>>(emptyList()) }

            fun reload() {
                files = assetsDir.listFiles()
                    ?.filter { it.isFile && (it.name.endsWith(".db") || it.name.endsWith(".srs") || it.name.endsWith(".dat")) }
                    ?.sortedBy { it.name }
                    ?: emptyList()
            }

            LaunchedEffect(Unit) { reload() }

            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    runOnDefaultDispatcher {
                        try {
                            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "custom_rule.db"
                            val outFile = File(assetsDir, name)
                            contentResolver.openInputStream(uri)?.use { input ->
                                outFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            onMainDispatcher {
                                reload()
                                showComposeSnackbar("Imported $name")
                            }
                        } catch (e: Exception) {
                            onMainDispatcher { showComposeSnackbar(e.readableMessage) }
                        }
                    }
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(composeSnackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.route_manage_assets), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                                Icon(Icons.Rounded.FileOpen, contentDescription = stringResource(R.string.action_import_file))
                            }
                        },
                    )
                },
            ) { padding ->
                if (files.isEmpty()) {
                    ExpressiveEmptyState(
                        icon = Icons.Rounded.FolderZip,
                        title = stringResource(R.string.route_manage_assets),
                        actionLabel = stringResource(R.string.action_import_file),
                        onAction = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(files, key = { _, f -> f.absolutePath }) { idx, file ->
                            val pos = when {
                                files.size == 1 -> ExpressiveGroupPosition.SINGLE
                                idx == 0 -> ExpressiveGroupPosition.TOP
                                idx == files.lastIndex -> ExpressiveGroupPosition.BOTTOM
                                else -> ExpressiveGroupPosition.MIDDLE
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
                                        Text(file.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = Formatter.formatFileSize(context, file.length()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
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
        }
    }
}
