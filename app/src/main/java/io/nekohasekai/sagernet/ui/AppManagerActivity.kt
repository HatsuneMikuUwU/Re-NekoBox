package io.nekohasekai.sagernet.ui

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressiveSearchField
import io.nekohasekai.sagernet.ui.components.ExpressiveSurfaceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val iconBitmap: Bitmap?,
)

fun Drawable.toBitmapOrNull(size: Int = 96): Bitmap? {
    return try {
        if (this is BitmapDrawable && bitmap != null) return bitmap
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bmp
    } catch (_: Exception) {
        null
    }
}

class AppManagerActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            val context = LocalContext.current
            var loading by remember { mutableStateOf(true) }
            var apps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
            var selectedPackages by remember {
                mutableStateOf(
                    DataStore.individual.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                )
            }
            var proxyAppsEnabled by remember { mutableStateOf(DataStore.proxyApps) }
            var bypassMode by remember { mutableStateOf(DataStore.bypass) }
            var showSystemApps by remember { mutableStateOf(false) }
            var query by remember { mutableStateOf("") }
            var menuOpen by remember { mutableStateOf(false) }

            fun persist(pkgs: Set<String>) {
                selectedPackages = pkgs
                DataStore.individual = pkgs.joinToString("\n")
            }

            LaunchedEffect(Unit) {
                apps = withContext(Dispatchers.IO) {
                    val pm = context.packageManager
                    pm.getInstalledApplications(0)
                        .filter { it.packageName != context.packageName }
                        .map { info ->
                            val isSys = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                            InstalledAppItem(
                                packageName = info.packageName,
                                label = pm.getApplicationLabel(info).toString(),
                                isSystem = isSys,
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

            val filtered = remember(apps, query, showSystemApps, selectedPackages) {
                apps.filter { app ->
                    (showSystemApps || !app.isSystem || app.packageName in selectedPackages) &&
                        (query.isBlank() ||
                            app.label.contains(query, ignoreCase = true) ||
                            app.packageName.contains(query, ignoreCase = true))
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.proxied_apps), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            Switch(
                                checked = proxyAppsEnabled,
                                onCheckedChange = {
                                    proxyAppsEnabled = it
                                    DataStore.proxyApps = it
                                },
                                modifier = Modifier.padding(end = 6.dp),
                            )
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_invert_selections)) },
                                        onClick = {
                                            menuOpen = false
                                            val allVisible = filtered.map { it.packageName }.toSet()
                                            val next = (selectedPackages - allVisible) + (allVisible - selectedPackages)
                                            persist(next)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.clear_selections)) },
                                        onClick = {
                                            menuOpen = false
                                            persist(emptySet())
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_copy)) },
                                        onClick = {
                                            menuOpen = false
                                            SagerNet.trySetPrimaryClip(selectedPackages.joinToString("\n"))
                                            showComposeSnackbar(getString(R.string.copy_success))
                                        },
                                    )
                                }
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
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        SegmentedButton(
                            selected = !bypassMode,
                            onClick = {
                                bypassMode = false
                                DataStore.bypass = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text(stringResource(R.string.route_proxy)) }
                        SegmentedButton(
                            selected = bypassMode,
                            onClick = {
                                bypassMode = true
                                DataStore.bypass = true
                            },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text(stringResource(R.string.bypass_apps)) }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            selected = showSystemApps,
                            onClick = { showSystemApps = !showSystemApps },
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
                            verticalArrangement = Arrangement.spacedBy(2.dp),
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
                                        val next = if (checked) selectedPackages - item.packageName else selectedPackages + item.packageName
                                        persist(next)
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
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = null,
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
}
