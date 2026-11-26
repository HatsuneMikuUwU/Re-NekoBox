package io.nekohasekai.sagernet.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Update
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.ktx.launchCustomTab
import io.nekohasekai.sagernet.plugin.PluginManager.loadString
import io.nekohasekai.sagernet.ui.components.ExpressiveGroupPosition
import io.nekohasekai.sagernet.ui.components.ExpressiveIconBadge
import io.nekohasekai.sagernet.ui.components.PrefActionRow
import io.nekohasekai.sagernet.ui.components.PrefCategory
import io.nekohasekai.sagernet.utils.PackageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libcore.Libcore
import moe.matsuri.nb4a.plugin.Plugins

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var licenseText by remember { mutableStateOf("") }
    var showLicense by remember { mutableStateOf(false) }

    val ignoringBattery = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(PowerManager::class.java)
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else true
    }

    LaunchedEffect(Unit) {
        licenseText = withContext(Dispatchers.IO) {
            runCatching { context.assets.open("LICENSE").bufferedReader().use { it.readText() } }
                .getOrDefault("")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.menu_about),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ExpressiveIconBadge(
                            icon = Icons.Rounded.Info,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(54.dp),
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = SagerNet.appVersionNameForDisplay,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            )
                            Text(
                                text = "sing-box ${Libcore.versionBox()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                            )
                        }
                    }
                }
            }

            item {
                PrefCategory(titleRes = R.string.menu_about) {
                    PrefActionRow(
                        title = stringResource(R.string.app_version),
                        summary = SagerNet.appVersionNameForDisplay,
                        icon = Icons.Rounded.Update,
                        position = ExpressiveGroupPosition.TOP,
                        onClick = {
                            context.launchCustomTab("https://github.com/MatsuriDayo/NekoBoxForAndroid/releases")
                        },
                    )
                    PrefActionRow(
                        title = stringResource(R.string.version_x, "sing-box"),
                        summary = Libcore.versionBox(),
                        icon = Icons.Rounded.Layers,
                        showChevron = false,
                        onClick = {},
                    )
                    if (!ignoringBattery && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PrefActionRow(
                            title = stringResource(R.string.ignore_battery_optimizations),
                            summary = stringResource(R.string.ignore_battery_optimizations_sum),
                            icon = Icons.Rounded.BatteryChargingFull,
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                            Uri.parse("package:${context.packageName}"),
                                        )
                                    )
                                }
                            },
                        )
                    }
                    PrefActionRow(
                        title = stringResource(R.string.donate),
                        summary = stringResource(R.string.donate_info),
                        icon = Icons.Rounded.CardGiftcard,
                        onClick = {
                            context.launchCustomTab("https://matsuridayo.github.io/index_docs/#donate")
                        },
                    )
                    PrefActionRow(
                        title = stringResource(R.string.github),
                        summary = "https://github.com/MatsuriDayo/NekoBoxForAndroid",
                        icon = Icons.Rounded.Code,
                        position = ExpressiveGroupPosition.BOTTOM,
                        onClick = {
                            context.launchCustomTab("https://github.com/MatsuriDayo/NekoBoxForAndroid")
                        },
                    )
                }
            }

            val plugins = runCatching {
                PackageCache.awaitLoadSync()
                PackageCache.installedPluginPackages.values.mapNotNull { pkg ->
                    val pluginId = pkg.providers?.firstOrNull()?.loadString(Plugins.METADATA_KEY_ID)
                    if (pluginId.isNullOrBlank()) null
                    else Triple(pluginId, Plugins.displayExeProvider(pkg.packageName), "v${pkg.versionName}")
                }
            }.getOrDefault(emptyList())

            if (plugins.isNotEmpty()) {
                item {
                    PrefCategory(title = "Plugins") {
                        plugins.forEachIndexed { idx, (id, provider, ver) ->
                            val pos = when {
                                plugins.size == 1 -> ExpressiveGroupPosition.SINGLE
                                idx == 0 -> ExpressiveGroupPosition.TOP
                                idx == plugins.lastIndex -> ExpressiveGroupPosition.BOTTOM
                                else -> ExpressiveGroupPosition.MIDDLE
                            }
                            PrefActionRow(
                                title = "$id ($provider)",
                                summary = ver,
                                icon = Icons.Rounded.Extension,
                                showChevron = false,
                                position = pos,
                                onClick = {},
                            )
                        }
                    }
                }
            }

            if (licenseText.isNotBlank()) {
                item {
                    PrefCategory(title = "License") {
                        PrefActionRow(
                            title = "GNU General Public License",
                            summary = if (showLicense) "Tap to collapse" else "Tap to expand",
                            position = ExpressiveGroupPosition.SINGLE,
                            onClick = { showLicense = !showLicense },
                        )
                    }
                    if (showLicense) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Text(
                                text = licenseText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
