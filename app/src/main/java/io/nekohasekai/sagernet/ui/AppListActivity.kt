package io.nekohasekai.sagernet.ui

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressiveSearchField
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppListActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            val context = LocalContext.current
            var loading by remember { mutableStateOf(true) }
            var apps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
            var selectedPackages by remember {
                mutableStateOf(
                    DataStore.routePackages.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                )
            }
            var showSystem by remember { mutableStateOf(false) }
            var query by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                apps = withContext(Dispatchers.IO) {
                    val pm = context.packageManager
                    pm.getInstalledApplications(0)
                        .map { info ->
                            InstalledAppItem(
                                packageName = info.packageName,
                                label = pm.getApplicationLabel(info).toString(),
                                isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                                iconBitmap = runCatching { pm.getApplicationIcon(info).toBitmapOrNull() }.getOrNull(),
                            )
                        }
                        .sortedWith(
                            compareByDescending<InstalledAppItem> { it.packageName in selectedPackages }
                                .thenBy { it.label.lowercase() }
                        )
                }
                loading = false
            }

            val filtered = remember(apps, query, showSystem, selectedPackages) {
                apps.filter { app ->
                    (showSystem || !app.isSystem || app.packageName in selectedPackages) &&
                        (query.isBlank() ||
                            app.label.contains(query, ignoreCase = true) ||
                            app.packageName.contains(query, ignoreCase = true))
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.apps), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                DataStore.routePackages = selectedPackages.joinToString("\n")
                                setResult(RESULT_OK)
                                finish()
                            }) {
                                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.apply))
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExpressiveSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            placeholder = stringResource(R.string.search),
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = showSystem,
                            onClick = { showSystem = !showSystem },
                            label = { Text("System") },
                            shape = CircleShape,
                        )
                    }

                    if (loading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsIndexed(filtered, key = { _, item -> item.packageName }) { idx, item ->
                                val checked = item.packageName in selectedPackages
                                val pos = when {
                                    filtered.size == 1 -> ExpressiveGroupPosition.SINGLE
                                    idx == 0 -> ExpressiveGroupPosition.TOP
                                    idx == filtered.lastIndex -> ExpressiveGroupPosition.BOTTOM
                                    else -> ExpressiveGroupPosition.MIDDLE
                                }
                                ExpressiveSurfaceItem(
                                    position = pos,
                                    selected = checked,
                                    onClick = {
                                        selectedPackages = if (checked) {
                                            selectedPackages - item.packageName
                                        } else {
                                            selectedPackages + item.packageName
                                        }
                                        DataStore.routePackages = selectedPackages.joinToString("\n")
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (item.iconBitmap != null) {
                                            Image(
                                                bitmap = item.iconBitmap.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.size(38.dp),
                                            )
                                            Spacer(Modifier.width(12.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = item.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Checkbox(checked = checked, onCheckedChange = null)
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
