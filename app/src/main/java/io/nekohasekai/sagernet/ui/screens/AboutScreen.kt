@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)

package io.nekohasekai.sagernet.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.ktx.launchCustomTab
import io.nekohasekai.sagernet.plugin.PluginManager.loadString
import io.nekohasekai.sagernet.ui.components.AppMotion
import io.nekohasekai.sagernet.ui.components.FloatingTopBar
import io.nekohasekai.sagernet.ui.components.LocalBottomInset
import io.nekohasekai.sagernet.ui.components.blurBackdropSource
import io.nekohasekai.sagernet.ui.components.floatingTopBarSpace
import io.nekohasekai.sagernet.ui.components.hapticClick
import io.nekohasekai.sagernet.ui.components.rememberBlurBackdrop
import io.nekohasekai.sagernet.utils.PackageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libcore.Libcore
import moe.matsuri.nb4a.plugin.Plugins

private const val REPO_URL = "https://github.com/HatsuneMikuUwU/Re-NekoBox"
private const val UPSTREAM_URL = "https://github.com/MatsuriDayo/NekoBoxForAndroid"

private data class AboutHighlight(
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
)

private data class AboutLink(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val showChevron: Boolean = true,
    val onClick: (() -> Unit)? = null,
)

private val HIGHLIGHTS =
    listOf(
        AboutHighlight(Icons.Outlined.Router, R.string.about_hl_core_title, R.string.about_hl_core_desc),
        AboutHighlight(Icons.Outlined.Hub, R.string.about_hl_protocols_title, R.string.about_hl_protocols_desc),
        AboutHighlight(Icons.Outlined.AccountTree, R.string.about_hl_routing_title, R.string.about_hl_routing_desc),
        AboutHighlight(Icons.Outlined.Palette, R.string.about_hl_material_title, R.string.about_hl_material_desc),
    )

private val STACK =
    listOf(
        "Kotlin",
        "Coroutines",
        "Jetpack Compose",
        "Material 3 Expressive",
        "sing-box",
        "Go",
        "AndroidX",
        "Room",
        "OkHttp",
        "CameraX",
        "ZXing",
        "MaterialKolor",
        "Miuix",
    )

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
        } else {
            true
        }
    }

    LaunchedEffect(Unit) {
        licenseText = withContext(Dispatchers.IO) {
            runCatching { context.assets.open("LICENSE").bufferedReader().use { it.readText() } }
                .getOrDefault("")
        }
    }

    val plugins = remember {
        runCatching {
            PackageCache.awaitLoadSync()
            PackageCache.installedPluginPackages.values.mapNotNull { pkg ->
                val pluginId = pkg.providers?.firstOrNull()?.loadString(Plugins.METADATA_KEY_ID)
                if (pluginId.isNullOrBlank()) null
                else Triple(pluginId, Plugins.displayExeProvider(pkg.packageName), "v${pkg.versionName}")
            }
        }.getOrDefault(emptyList())
    }

    val links = buildList {
        add(
            AboutLink(
                icon = Icons.Outlined.Code,
                title = stringResource(R.string.github),
                subtitle = "HatsuneMikuUwU/Re-NekoBox",
                onClick = { context.launchCustomTab(REPO_URL) },
            ),
        )
        add(
            AboutLink(
                icon = Icons.Outlined.NewReleases,
                title = stringResource(R.string.about_releases),
                subtitle = stringResource(R.string.about_releases_desc),
                onClick = { context.launchCustomTab("$REPO_URL/releases") },
            ),
        )
        add(
            AboutLink(
                icon = Icons.Outlined.BugReport,
                title = stringResource(R.string.about_report_issue),
                subtitle = stringResource(R.string.about_report_issue_desc),
                onClick = { context.launchCustomTab("$REPO_URL/issues") },
            ),
        )
        if (!ignoringBattery && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            add(
                AboutLink(
                    icon = Icons.Outlined.BatteryChargingFull,
                    title = stringResource(R.string.ignore_battery_optimizations),
                    subtitle = stringResource(R.string.ignore_battery_optimizations_sum),
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    },
                ),
            )
        }
        add(
            AboutLink(
                icon = Icons.Outlined.Layers,
                title = stringResource(R.string.about_upstream),
                subtitle = "MatsuriDayo/NekoBoxForAndroid",
                onClick = { context.launchCustomTab(UPSTREAM_URL) },
            ),
        )
    }

    val topInset = floatingTopBarSpace()
    val bottomInset = LocalBottomInset.current
    val topBackdrop = rememberBlurBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blurBackdropSource(topBackdrop),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(8.dp + topInset))
                AboutHero()
                Spacer(Modifier.height(28.dp))
                HighlightGrid()
                Spacer(Modifier.height(28.dp))
                AboutSectionTitle(stringResource(R.string.about_section_links))
                AboutLinkGroup(links)
                if (plugins.isNotEmpty()) {
                    Spacer(Modifier.height(28.dp))
                    AboutSectionTitle(stringResource(R.string.about_section_plugins))
                    AboutLinkGroup(
                        plugins.map { (id, provider, ver) ->
                            AboutLink(
                                icon = Icons.Outlined.Extension,
                                title = "$id ($provider)",
                                subtitle = ver,
                                showChevron = false,
                                onClick = null,
                            )
                        },
                    )
                }
                Spacer(Modifier.height(28.dp))
                AboutSectionTitle(stringResource(R.string.about_section_built_with))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    STACK.forEach { StackPill(it) }
                }
                if (licenseText.isNotBlank()) {
                    Spacer(Modifier.height(28.dp))
                    LicenseCard(
                        licenseText = licenseText,
                        expanded = showLicense,
                        onToggle = { showLicense = !showLicense },
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.about_footer),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(24.dp + bottomInset))
            }
        }

        FloatingTopBar(
            backdrop = topBackdrop,
            title = stringResource(R.string.menu_about),
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun RotatingCookieFrame(
    modifier: Modifier = Modifier,
    frameSize: Dp = 176.dp,
    innerSize: Dp = 116.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val spin = rememberInfiniteTransition(label = "cookie-spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "cookie-angle",
    )
    Box(modifier.size(frameSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = angle }
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(cs.primaryContainer),
        )
        Box(
            Modifier
                .size(innerSize)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(cs.primary),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
private fun AboutHero() {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RotatingCookieFrame {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.ic_launcher_background)),
            )
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = cs.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.app_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(shape = CircleShape, color = cs.secondaryContainer, contentColor = cs.onSecondaryContainer) {
                Text(
                    "v${SagerNet.appVersionNameForDisplay}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            val singBoxVersion = remember {
                val firstLine = Libcore.versionBox().lineSequence().firstOrNull()?.trim().orEmpty()
                if (firstLine.startsWith("sing-box", ignoreCase = true)) firstLine else "sing-box: $firstLine"
            }
            Surface(shape = CircleShape, color = cs.tertiaryContainer, contentColor = cs.onTertiaryContainer) {
                Text(
                    singBoxVersion,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HighlightGrid() {
    val cs = MaterialTheme.colorScheme
    val leaning = RoundedCornerShape(topStart = 36.dp, topEnd = 12.dp, bottomEnd = 36.dp, bottomStart = 12.dp)
    val mirrored = RoundedCornerShape(topStart = 12.dp, topEnd = 36.dp, bottomEnd = 12.dp, bottomStart = 36.dp)
    val palette =
        listOf(
            cs.primaryContainer to cs.onPrimaryContainer,
            cs.secondaryContainer to cs.onSecondaryContainer,
            cs.tertiaryContainer to cs.onTertiaryContainer,
            cs.surfaceContainer to cs.onSurface,
        )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HIGHLIGHTS.chunked(2).forEachIndexed { row, pair ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEachIndexed { col, item ->
                    val i = row * 2 + col
                    HighlightTile(
                        item = item,
                        container = palette[i].first,
                        content = palette[i].second,
                        shape = if (i == 0 || i == 3) leaning else mirrored,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightTile(
    item: AboutHighlight,
    container: Color,
    content: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.clip(shape).background(container).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(48.dp).clip(MaterialShapes.Clover4Leaf.toShape()).background(content),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = null, tint = container)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(item.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = content,
            )
            Text(
                stringResource(item.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun AboutSectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

private fun aboutGroupedShape(
    index: Int,
    count: Int,
): Shape {
    val outer = 28.dp
    val inner = 4.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner,
    )
}

@Composable
private fun AboutLinkGroup(links: List<AboutLink>) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        links.forEachIndexed { i, link ->
            val rowContent: @Composable () -> Unit = {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(48.dp).clip(MaterialShapes.Clover4Leaf.toShape()).background(cs.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(link.icon, contentDescription = null, tint = cs.onPrimary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            link.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            link.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (link.showChevron) {
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(MaterialShapes.Cookie9Sided.toShape())
                                .background(cs.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = cs.onSurface,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            if (link.onClick != null) {
                Surface(
                    onClick = hapticClick(block = link.onClick),
                    shape = aboutGroupedShape(i, links.size),
                    color = cs.surfaceContainer,
                    content = rowContent,
                )
            } else {
                Surface(
                    shape = aboutGroupedShape(i, links.size),
                    color = cs.surfaceContainer,
                    content = rowContent,
                )
            }
        }
    }
}

@Composable
private fun StackPill(label: String) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = CircleShape, color = cs.surfaceContainer, contentColor = cs.onSurface) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun LicenseCard(
    licenseText: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.tertiaryContainer)
            .clickable(onClick = hapticClick(block = onToggle))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = cs.onTertiaryContainer)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.about_license_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = cs.onTertiaryContainer,
                )
                Text(
                    stringResource(if (expanded) R.string.about_license_tap_collapse else R.string.about_license_tap_expand),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onTertiaryContainer,
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = AppMotion.expandEnter(),
            exit = AppMotion.expandExit(),
        ) {
            Text(
                text = licenseText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = cs.onTertiaryContainer.copy(alpha = 0.9f),
            )
        }
    }
}
