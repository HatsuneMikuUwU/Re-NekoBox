@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)

package io.nekohasekai.sagernet.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.ui.theme.AppPaletteStyle
import io.nekohasekai.sagernet.ui.theme.AppearanceStore
import io.nekohasekai.sagernet.ui.theme.ContrastPreset
import io.nekohasekai.sagernet.ui.theme.PresetSeedColors
import io.nekohasekai.sagernet.ui.theme.ThemeMode
import kotlin.math.abs
import kotlin.math.roundToInt

private val SheetGroupCorner = 28.dp

@Composable
fun UiCustomizationSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val settings by AppearanceStore.settings.collectAsState()
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var showAccentSheet by rememberSaveable { mutableStateOf(false) }
    var showPaletteDialog by rememberSaveable { mutableStateOf(false) }
    var showContrastDialog by rememberSaveable { mutableStateOf(false) }
    var showAnimSpeedDialog by rememberSaveable { mutableStateOf(false) }
    var showUiScaleDialog by rememberSaveable { mutableStateOf(false) }
    var showFontScaleDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = visible, onBack = onDismiss)

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.85f)
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(topStart = SheetGroupCorner, bottomStart = SheetGroupCorner),
                color = cs.background,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 10.dp,
                            end = 10.dp,
                            bottom = 12.dp,
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(MaterialShapes.Clover4Leaf.toShape())
                                .background(cs.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "UI Customization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Re:NekoBox • Material 3 Expressive",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }

                    // 1. Theme & Color Group
                    SheetSettingGroup(
                        icon = Icons.Rounded.ColorLens,
                        title = "Theme & Colors",
                        initiallyExpanded = true,
                    ) {
                        val count = if (dynamicSupported) 7 else 6
                        var idx = 0
                        if (dynamicSupported) {
                            SheetSwitchItem(
                                icon = Icons.Rounded.Colorize,
                                title = "Dynamic Color",
                                subtitle = "Follow Android 12+ wallpaper colors",
                                shape = groupedSheetItemShape(idx++, count),
                                checked = settings.dynamicColor,
                                onChange = AppearanceStore::setDynamicColor,
                            )
                        }
                        val hexSeed = String.format("#%06X", 0xFFFFFF and settings.seedColorArgb)
                        SheetPrefItem(
                            icon = Icons.Rounded.Palette,
                            title = "Accent Seed Color",
                            subtitle = if (settings.dynamicColor && dynamicSupported) "Using system wallpaper ($hexSeed fallback)" else hexSeed,
                            shape = groupedSheetItemShape(idx++, count),
                            onClick = { showAccentSheet = true },
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(MaterialShapes.Cookie9Sided.toShape())
                                        .background(if (settings.dynamicColor && dynamicSupported) cs.primary else settings.seedColor),
                                )
                            },
                        )
                        SheetPrefItem(
                            icon = Icons.Rounded.Style,
                            title = "Palette Style",
                            subtitle = settings.paletteStyle.label,
                            shape = groupedSheetItemShape(idx++, count),
                            onClick = { showPaletteDialog = true },
                        )
                        val contrastLabel = ContrastPreset.entries.minByOrNull { abs(it.value - settings.contrast) }?.label ?: "Default"
                        SheetPrefItem(
                            icon = Icons.Rounded.Contrast,
                            title = "Contrast",
                            subtitle = contrastLabel,
                            shape = groupedSheetItemShape(idx++, count),
                            onClick = { showContrastDialog = true },
                        )
                        SheetSwitchItem(
                            icon = Icons.Rounded.FormatColorFill,
                            title = "AMOLED Pure Black",
                            subtitle = "Pure #000000 background in dark mode",
                            shape = groupedSheetItemShape(idx++, count),
                            checked = settings.amoled,
                            onChange = AppearanceStore::setAmoled,
                        )
                        SheetSwitchItem(
                            icon = Icons.Rounded.BlurOn,
                            title = "Floating Blur Effect",
                            subtitle = "Frosted glass blur on floating bars & dialogs",
                            shape = groupedSheetItemShape(idx++, count),
                            checked = settings.blurEnabled,
                            onChange = AppearanceStore::setBlurEnabled,
                        )
                        SheetPrefItem(
                            icon = Icons.Rounded.Animation,
                            title = "Color Transition",
                            subtitle = if (settings.animateColorChange) "Smooth (${settings.colorAnimationMs} ms)" else "Instant",
                            shape = groupedSheetItemShape(idx, count),
                            onClick = { showAnimSpeedDialog = true },
                        )
                    }

                    // 2. Night Mode Group
                    SheetSettingGroup(
                        icon = Icons.Rounded.DarkMode,
                        title = "Night Mode",
                        initiallyExpanded = true,
                    ) {
                        val modes = listOf(
                            Triple(ThemeMode.SYSTEM, "Follow system", Icons.Rounded.BrightnessMedium),
                            Triple(ThemeMode.LIGHT, "Light mode", Icons.Rounded.LightMode),
                            Triple(ThemeMode.DARK, "Dark mode", Icons.Rounded.DarkMode),
                        )
                        modes.forEachIndexed { index, (mode, label, icon) ->
                            SheetRadioItem(
                                icon = icon,
                                title = label,
                                selected = settings.mode == mode,
                                shape = groupedSheetItemShape(index, modes.size),
                                onClick = { AppearanceStore.setMode(mode) },
                            )
                        }
                    }

                    // 3. UI & Font Scale Group
                    SheetSettingGroup(
                        icon = Icons.Rounded.ZoomIn,
                        title = "UI & Font Scale",
                        initiallyExpanded = false,
                    ) {
                        SheetPrefItem(
                            icon = Icons.Rounded.ZoomIn,
                            title = "Display Scale",
                            subtitle = "${(settings.uiScale * 100).roundToInt()}% of system default",
                            shape = groupedSheetItemShape(0, 2),
                            onClick = { showUiScaleDialog = true },
                        )
                        SheetPrefItem(
                            icon = Icons.Rounded.FormatSize,
                            title = "Font Scale",
                            subtitle = "${(settings.fontScale * 100).roundToInt()}% of system default",
                            shape = groupedSheetItemShape(1, 2),
                            onClick = { showFontScaleDialog = true },
                        )
                    }

                    // 4. Reset Group
                    SheetSettingGroup(
                        icon = Icons.Rounded.Restore,
                        title = "Reset",
                        initiallyExpanded = false,
                    ) {
                        SheetPrefItem(
                            icon = Icons.Rounded.Restore,
                            title = "Reset UI Customization",
                            subtitle = "Restore default theme, palette, blur, and scale",
                            shape = groupedSheetItemShape(0, 1),
                            onClick = { showResetDialog = true },
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }

    // Accent Color ModalBottomSheet (AnimeX style with WindowBlurEffect + SheetItemSurface + Cookie swatches + Hex input)
    if (showAccentSheet) {
        var hexInput by remember(settings.seedColorArgb) {
            mutableStateOf(String.format("#%06X", 0xFFFFFF and settings.seedColorArgb))
        }
        val parsedHex = remember(hexInput) {
            val clean = hexInput.trim().removePrefix("#")
            if (clean.length == 6) clean.toIntOrNull(16)?.let { 0xFF000000.toInt() or it } else null
        }

        ModalBottomSheet(
            onDismissRequest = { showAccentSheet = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            WindowBlurEffect()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Accent Seed Color",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val cookie = MaterialShapes.Cookie9Sided.toShape()
                    PresetSeedColors.forEach { (_, argb) ->
                        val color = Color(argb)
                        val isSelected = !settings.dynamicColor && settings.seedColorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(cookie)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, cs.onSurface, cookie) else Modifier
                                )
                                .clickable {
                                    AppearanceStore.setSeedColor(argb)
                                    hexInput = String.format("#%06X", 0xFFFFFF and argb)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }

                SheetItemSurface(shape = groupedSheetItemShape(0, 1)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { hexInput = it },
                            label = { Text("Custom Hex (#RRGGBB)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                        )
                        DialogConfirmButton("Apply") {
                            parsedHex?.let {
                                AppearanceStore.setSeedColor(it)
                                showAccentSheet = false
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaletteDialog) {
        val original = rememberSaveable { settings.paletteStyle }
        var confirmed by remember { mutableStateOf(false) }
        val cancel = {
            if (!confirmed) AppearanceStore.setPaletteStyle(original)
            showPaletteDialog = false
        }
        AppDialog(
            icon = Icons.Rounded.Style,
            title = "Palette Style",
            onDismiss = cancel,
            confirmButton = {
                DialogConfirmButton("Save") {
                    confirmed = true
                    showPaletteDialog = false
                }
            },
            dismissButton = { DialogCancelButton("Cancel", cancel) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    AppPaletteStyle.entries.forEach { style ->
                        DialogOptionRow(
                            label = style.label,
                            selected = settings.paletteStyle == style,
                            onClick = { AppearanceStore.setPaletteStyle(style) },
                        )
                    }
                }
            },
        )
    }

    if (showContrastDialog) {
        val original = rememberSaveable { settings.contrast }
        var confirmed by remember { mutableStateOf(false) }
        val cancel = {
            if (!confirmed) AppearanceStore.setContrast(original)
            showContrastDialog = false
        }
        AppDialog(
            icon = Icons.Rounded.Contrast,
            title = "Contrast",
            onDismiss = cancel,
            confirmButton = {
                DialogConfirmButton("Save") {
                    confirmed = true
                    showContrastDialog = false
                }
            },
            dismissButton = { DialogCancelButton("Cancel", cancel) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ContrastPreset.entries.forEach { preset ->
                        DialogOptionRow(
                            label = preset.label,
                            selected = abs(settings.contrast - preset.value) < 0.01f,
                            onClick = { AppearanceStore.setContrast(preset.value) },
                        )
                    }
                }
            },
        )
    }

    if (showAnimSpeedDialog) {
        val options = listOf(0 to "Instant (off)", 200 to "Fast (200 ms)", 350 to "Balanced (350 ms)", 500 to "Smooth (500 ms)", 750 to "Slow (750 ms)")
        val currentMs = if (settings.animateColorChange) settings.colorAnimationMs else 0
        AppDialog(
            icon = Icons.Rounded.Timer,
            title = "Color Transition",
            onDismiss = { showAnimSpeedDialog = false },
            confirmButton = { DialogCancelButton("Close") { showAnimSpeedDialog = false } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    options.forEach { (ms, label) ->
                        DialogOptionRow(
                            label = label,
                            selected = currentMs == ms,
                            onClick = {
                                if (ms == 0) {
                                    AppearanceStore.setAnimateColorChange(false)
                                } else {
                                    AppearanceStore.setAnimateColorChange(true)
                                    AppearanceStore.setColorAnimationMs(ms)
                                }
                                showAnimSpeedDialog = false
                            },
                        )
                    }
                }
            },
        )
    }

    if (showUiScaleDialog) {
        var draft by remember { mutableFloatStateOf(settings.uiScale) }
        AppDialog(
            icon = Icons.Rounded.ZoomIn,
            title = "Display Scale",
            onDismiss = { showUiScaleDialog = false },
            confirmButton = {
                DialogConfirmButton("Save") {
                    AppearanceStore.setUiScale(draft)
                    showUiScaleDialog = false
                }
            },
            dismissButton = { DialogCancelButton("Cancel") { showUiScaleDialog = false } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${(draft * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Slider(
                        value = draft,
                        onValueChange = { draft = (it * 20).roundToInt() / 20f },
                        valueRange = 0.85f..1.15f,
                        steps = 5,
                    )
                }
            },
        )
    }

    if (showFontScaleDialog) {
        var draft by remember { mutableFloatStateOf(settings.fontScale) }
        AppDialog(
            icon = Icons.Rounded.FormatSize,
            title = "Font Scale",
            onDismiss = { showFontScaleDialog = false },
            confirmButton = {
                DialogConfirmButton("Save") {
                    AppearanceStore.setFontScale(draft)
                    showFontScaleDialog = false
                }
            },
            dismissButton = { DialogCancelButton("Cancel") { showFontScaleDialog = false } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${(draft * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Slider(
                        value = draft,
                        onValueChange = { draft = (it * 20).roundToInt() / 20f },
                        valueRange = 0.85f..1.20f,
                        steps = 6,
                    )
                }
            },
        )
    }

    if (showResetDialog) {
        AppDialog(
            icon = Icons.Rounded.Restore,
            title = "Reset UI Customization?",
            onDismiss = { showResetDialog = false },
            text = { Text("Theme colors, palette style, night mode, blur, and scale will be restored to defaults.") },
            confirmButton = {
                DialogDestructiveButton("Reset") {
                    AppearanceStore.reset()
                    showResetDialog = false
                }
            },
            dismissButton = { DialogCancelButton("Cancel") { showResetDialog = false } },
        )
    }
}

@Composable
private fun SheetSettingGroup(
    icon: ImageVector,
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val arrowRot by animateFloatAsState(if (expanded) 180f else 0f, label = "groupArrow")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SheetGroupCorner),
        color = cs.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(arrowRot),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun SheetPrefItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    shape: Shape,
    enabled: Boolean = true,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    SheetItemSurface(
        shape = shape,
        onClick = if (enabled) {
            {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        } else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.5f)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialShapes.Clover4Leaf.toShape())
                    .background(cs.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = cs.onSecondaryContainer, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun SheetSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    shape: Shape,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    SheetItemSurface(
        shape = shape,
        onClick = if (enabled) {
            {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onChange(!checked)
            }
        } else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.5f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialShapes.Clover4Leaf.toShape())
                    .background(cs.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = cs.onSecondaryContainer, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onChange(it)
                    }
                } else null,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun SheetRadioItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = shape,
        color = if (selected) cs.primaryContainer else cs.surfaceContainer,
        contentColor = if (selected) cs.onPrimaryContainer else cs.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialShapes.Clover4Leaf.toShape())
                    .background(if (selected) cs.primary else cs.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) cs.onPrimary else cs.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) cs.primary else cs.onSurfaceVariant,
            )
        }
    }
}
