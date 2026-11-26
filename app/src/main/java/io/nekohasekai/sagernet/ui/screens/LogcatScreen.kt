package io.nekohasekai.sagernet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.ui.components.ExpressiveSearchField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import libcore.Libcore
import moe.matsuri.nb4a.utils.SendLog

@Composable
fun LogcatScreen(
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var logLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var filterQuery by remember { mutableStateOf("") }

    fun reloadLogs() {
        scope.launch {
            val raw = withContext(Dispatchers.IO) {
                runCatching { String(SendLog.getNekoLog(50 * 1024)) }.getOrDefault("")
            }
            logLines = raw.lines().filter { it.isNotBlank() }
            if (logLines.isNotEmpty()) {
                listState.scrollToItem(logLines.lastIndex)
            }
        }
    }

    LaunchedEffect(Unit) { reloadLogs() }

    val visibleLines = remember(logLines, filterQuery) {
        if (filterQuery.isBlank()) logLines
        else logLines.filter { it.contains(filterQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.menu_log),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { reloadLogs() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        runOnDefaultDispatcher {
                            SendLog.sendLog(context, "NB4A")
                        }
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Send Log")
                    }
                    IconButton(onClick = {
                        runOnDefaultDispatcher {
                            try {
                                Libcore.nekoLogClear()
                                Runtime.getRuntime().exec("/system/bin/logcat -c")
                                onMainDispatcher { logLines = emptyList() }
                            } catch (e: Exception) {
                                onMainDispatcher { onShowMessage(e.readableMessage) }
                            }
                        }
                    }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            ExpressiveSearchField(
                query = filterQuery,
                onQueryChange = { filterQuery = it },
                placeholder = stringResource(R.string.search),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 1.dp,
            ) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(visibleLines) { line ->
                            val lineColor = when {
                                line.contains("ERROR[") || line.contains(" [Error]") -> Color(0xFFEF5350)
                                line.contains("WARN[") || line.contains(" [Warning]") -> Color(0xFFFFA726)
                                line.contains("INFO[") || line.contains(" [Info]") -> Color(0xFF66BB6A)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = lineColor,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
