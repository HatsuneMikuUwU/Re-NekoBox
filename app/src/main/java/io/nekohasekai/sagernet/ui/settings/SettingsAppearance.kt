@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)

package io.nekohasekai.sagernet.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DesignServices
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.nekohasekai.sagernet.ui.background.BackgroundType
import io.nekohasekai.sagernet.ui.background.RandomInterval
import io.nekohasekai.sagernet.ui.background.SeasonalEvent
import io.nekohasekai.sagernet.ui.components.AppDialog
import io.nekohasekai.sagernet.ui.components.DialogCancelButton
import io.nekohasekai.sagernet.ui.components.DialogConfirmButton
import io.nekohasekai.sagernet.ui.components.rememberStableHapticAction
import io.nekohasekai.sagernet.ui.theme.Appearance
import io.nekohasekai.sagernet.ui.theme.BarEffect
import io.nekohasekai.sagernet.ui.theme.DynamicColorSupported
import io.nekohasekai.sagernet.ui.theme.FONT_SCALE_MAX
import io.nekohasekai.sagernet.ui.theme.FONT_SCALE_MIN
import io.nekohasekai.sagernet.ui.theme.FONT_SCALE_STEP
import io.nekohasekai.sagernet.ui.theme.PaletteStyle
import io.nekohasekai.sagernet.ui.theme.ThemeMode
import io.nekohasekai.sagernet.ui.theme.UI_SCALE_MAX
import io.nekohasekai.sagernet.ui.theme.UI_SCALE_MIN
import io.nekohasekai.sagernet.ui.theme.UI_SCALE_STEP
import io.nekohasekai.sagernet.ui.theme.coerceToFontScale
import io.nekohasekai.sagernet.ui.theme.coerceToUiScale
import io.nekohasekai.sagernet.ui.theme.contrastLabel
import io.nekohasekai.sagernet.ui.theme.effectiveSpec
import io.nekohasekai.sagernet.ui.theme.seedColor
import io.nekohasekai.sagernet.ui.theme.toHexString
import io.nekohasekai.sagernet.ui.theme.toUiScalePercent

@Composable
internal fun UiScaleGroup() {
    val cs = MaterialTheme.colorScheme
    val settings by Appearance.settings.collectAsStateWithLifecycle()
    var showDensityDialog by rememberSaveable { mutableStateOf(false) }
    var showFontDialog by rememberSaveable { mutableStateOf(false) }

    SettingGroup(
        icon = Icons.Outlined.ZoomIn,
        title = "UI scale",
        initiallyExpanded = false,
    ) {
        SwitchItem(
            icon = Icons.Outlined.AspectRatio,
            title = "Auto scale",
            subtitle = "Automatically match UI scale to your device screen width",
            shape = itemShape(0, 3),
            checked = settings.autoScaleUi,
            enabled = true,
            onChange = Appearance::setAutoScaleUi,
        )
        PrefItem(
            icon = Icons.Outlined.ZoomIn,
            title = "Display scale",
            subtitle =
                if (settings.autoScaleUi) {
                    "Disabled while auto scale is active"
                } else {
                    "${settings.uiScale.toUiScalePercent()}% of system default"
                },
            shape = itemShape(1, 3),
            container = itemContainer(),
            content = cs.onSurface,
            enabled = !settings.autoScaleUi,
            badge = cs.primary,
            badgeTint = cs.onPrimary,
            onClick = { showDensityDialog = true },
        )
        PrefItem(
            icon = Icons.Outlined.FormatSize,
            title = "Font size",
            subtitle =
                if (settings.autoScaleUi) {
                    "Disabled while auto scale is active"
                } else {
                    "${settings.fontScale.toUiScalePercent()}% of default"
                },
            shape = itemShape(2, 3),
            container = itemContainer(),
            content = cs.onSurface,
            enabled = !settings.autoScaleUi,
            badge = cs.primary,
            badgeTint = cs.onPrimary,
            onClick = { showFontDialog = true },
        )
    }

    if (showDensityDialog) {
        LiveScaleDialog(
            icon = Icons.Outlined.ZoomIn,
            title = "Display scale",
            description =
                "Scales the entire app interface (elements and text) on top of system display zoom. " +
                    "Applied when you tap Apply.",
            currentScale = settings.uiScale,
            min = UI_SCALE_MIN,
            max = UI_SCALE_MAX,
            step = UI_SCALE_STEP,
            coerce = { it.coerceToUiScale() },
            onApply = Appearance::setUiScale,
            live = false,
            onDismiss = { showDensityDialog = false },
        )
    }

    if (showFontDialog) {
        LiveScaleDialog(
            icon = Icons.Outlined.FormatSize,
            title = "Font size",
            description =
                "Adjusts text size only, on top of display scale and system font size. " +
                    "Applied when you tap Apply.",
            currentScale = settings.fontScale,
            min = FONT_SCALE_MIN,
            max = FONT_SCALE_MAX,
            step = FONT_SCALE_STEP,
            coerce = { it.coerceToFontScale() },
            onApply = Appearance::setFontScale,
            live = false,
            onDismiss = { showFontDialog = false },
        )
    }
}

@Composable
internal fun CustomizationGroup(initiallyExpanded: Boolean = true) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val settings by Appearance.settings.collectAsStateWithLifecycle()
    val dynamicActive = settings.dynamicColor && DynamicColorSupported
    val runningSpec = effectiveSpec(settings.paletteStyle, settings.colorSpec)
    val offeredSpecs = settings.paletteStyle.offeredSpecs
    var showAccentSheet by rememberSaveable { mutableStateOf(false) }
    var showStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showSpecDialog by rememberSaveable { mutableStateOf(false) }
    var showBarEffectDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomColorSheet by rememberSaveable { mutableStateOf(false) }
    var showContrastDialog by rememberSaveable { mutableStateOf(false) }
    var showRoleSheet by rememberSaveable { mutableStateOf(false) }
    var showColorAnimDialog by rememberSaveable { mutableStateOf(false) }
    val animExtra = if (settings.animateColorChange) 1 else 0
    val itemCount = 12 + animExtra
    val barEffectEnabled = settings.blur && BlurSupported

    SettingGroup(
        icon = Icons.Outlined.DesignServices,
        title = "Customization",
        initiallyExpanded = initiallyExpanded,
    ) {
        PrefItem(
            icon = Icons.Outlined.Palette,
            title = "Color scheme",
            subtitle = "App theme will be generated from your selected color",
            shape = itemShape(0, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            enabled = !dynamicActive,
            onDisabledClick = {
                Toast.makeText(ctx, "Turn off dynamic colors first to change color scheme", Toast.LENGTH_SHORT).show()
            },
            onClick = { showAccentSheet = true },
            badge = cs.primary,
            badgeTint = cs.onPrimary,
            end = { ColorSchemePreview() },
        )
        PrefItem(
            icon = Icons.Outlined.Colorize,
            title = "Custom color",
            subtitle =
                if (settings.customSeed != 0) {
                    "Active, ${settings.seedColor.toHexString()}"
                } else {
                    "Pick any color or extract from an image"
                },
            shape = itemShape(1, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            enabled = !dynamicActive,
            onDisabledClick = {
                Toast.makeText(ctx, "Turn off dynamic colors first to use a custom color", Toast.LENGTH_SHORT).show()
            },
            onClick = { showCustomColorSheet = true },
            badge = cs.primary,
            badgeTint = cs.onPrimary,
        )
        PrefItem(
            icon = Icons.Outlined.Style,
            title = "Palette style",
            subtitle = settings.paletteStyle.label,
            shape = itemShape(2, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            badge = cs.primary,
            badgeTint = cs.onPrimary,
            onClick = { showStyleDialog = true },
        )
        PrefItem(
            icon = Icons.Outlined.Tune,
            title = "Color spec",
            subtitle =
                if (settings.paletteStyle.forcesSpec2026) {
                    "Expressive (2026), automatic for CMF style"
                } else if (runningSpec != settings.colorSpec) {
                    "${settings.colorSpec.label}, running as ${runningSpec.label} in ${settings.paletteStyle.label} style"
                } else {
                    settings.colorSpec.label
                },
            shape = itemShape(3, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            enabled = !settings.paletteStyle.forcesSpec2026,
            onDisabledClick = {
                Toast.makeText(ctx, "CMF always uses Expressive (2026) spec", Toast.LENGTH_SHORT).show()
            },
            onClick = { showSpecDialog = true },
            badge = cs.primary,
            badgeTint = cs.onPrimary,
        )
        PrefItem(
            icon = Icons.Outlined.Contrast,
            title = "Contrast",
            subtitle = settings.contrast.contrastLabel(),
            shape = itemShape(4, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            badge = cs.primary,
            badgeTint = cs.onPrimary,
            onClick = { showContrastDialog = true },
        )
        PrefItem(
            icon = Icons.Outlined.Palette,
            title = "Per-role colors",
            subtitle =
                if (settings.roleOverrides.isEmpty) {
                    "Lock primary, secondary, tertiary, and other palette roles"
                } else {
                    "${settings.roleOverrides.count} roles locked"
                },
            shape = itemShape(5, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            badge = cs.primary,
            badgeTint = cs.onPrimary,
            onClick = { showRoleSheet = true },
        )
        SwitchItem(
            icon = Icons.Outlined.Animation,
            title = "Animate color changes",
            subtitle = "Smoothly transition app colors when switching scheme, mode, or palette style",
            shape = itemShape(6, itemCount),
            checked = settings.animateColorChange,
            enabled = true,
            onChange = Appearance::setAnimateColorChange,
        )
        if (settings.animateColorChange) {
            PrefItem(
                icon = Icons.Outlined.Timer,
                title = "Animation duration",
                subtitle = "${settings.colorAnimationMs} ms",
                shape = itemShape(7, itemCount),
                container = itemContainer(),
                content = cs.onSurface,
                badge = cs.primary,
                badgeTint = cs.onPrimary,
                onClick = { showColorAnimDialog = true },
            )
        }
        SwitchItem(
            icon = Icons.Outlined.FormatColorFill,
            title = "Dynamic colors",
            subtitle =
                if (DynamicColorSupported) {
                    "Follow your wallpaper colors automatically (Material You)"
                } else {
                    "Requires Android 12 or higher"
                },
            shape = itemShape(7 + animExtra, itemCount),
            checked = dynamicActive,
            enabled = DynamicColorSupported,
            onChange = Appearance::setDynamicColor,
        )
        SwitchItem(
            icon = Icons.Outlined.Contrast,
            title = "AMOLED mode",
            subtitle = "Pure black background in dark mode to save battery on OLED screens",
            shape = itemShape(8 + animExtra, itemCount),
            checked = settings.amoled,
            enabled = true,
            onChange = Appearance::setAmoled,
        )
        SwitchItem(
            icon = Icons.Outlined.BlurOn,
            title = "Blur effect",
            subtitle =
                if (BlurSupported) {
                    "Enable real-time blur effects across bars and dialogs"
                } else {
                    "Requires Android 13 or higher"
                },
            shape = itemShape(9 + animExtra, itemCount),
            checked = settings.blur && BlurSupported,
            enabled = BlurSupported,
            onChange = Appearance::setBlur,
        )
        PrefItem(
            icon = Icons.Outlined.Tab,
            title = "Bar effect",
            subtitle =
                if (barEffectEnabled) {
                    "${settings.barEffect.label} for tab bar, toolbar, and search bar"
                } else {
                    "Enable blur effect first"
                },
            shape = itemShape(10 + animExtra, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            enabled = barEffectEnabled,
            onDisabledClick = {
                Toast.makeText(ctx, "Enable blur effect first to configure this", Toast.LENGTH_SHORT).show()
            },
            onClick = { showBarEffectDialog = true },
            badge = cs.primary,
            badgeTint = cs.onPrimary,
        )
        SwitchItem(
            icon = Icons.Outlined.TouchApp,
            title = "Haptics",
            subtitle = "Subtle tactile feedback when tapping buttons, cards, switches, and sliders",
            shape = itemShape(11 + animExtra, itemCount),
            checked = settings.hapticsEnabled,
            enabled = true,
            onChange = Appearance::setHapticsEnabled,
        )
    }

    if (showCustomColorSheet) {
        CustomColorSheet(onDismiss = { showCustomColorSheet = false })
    }

    if (showContrastDialog) {
        ContrastDialog(onDismiss = { showContrastDialog = false })
    }

    if (showColorAnimDialog && settings.animateColorChange) {
        ColorAnimationDialog(onDismiss = { showColorAnimDialog = false })
    }

    if (showRoleSheet) {
        RoleOverrideSheet(onDismiss = { showRoleSheet = false })
    }

    if (showAccentSheet) {
        AccentSheet(
            selected = settings.accent,
            onSelect = {
                Appearance.setCustomSeed(0)
                Appearance.setAccent(it)
            },
            onDismiss = { showAccentSheet = false },
        )
    }

    if (showStyleDialog) {
        OptionDialog(
            icon = Icons.Outlined.Style,
            title = "Palette style",
            options = PaletteStyle.entries,
            selected = settings.paletteStyle,
            label = { it.label },
            onSelect = Appearance::setPaletteStyle,
            onDismiss = { showStyleDialog = false },
        )
    }

    if (showBarEffectDialog) {
        OptionDialog(
            icon = Icons.Outlined.Tab,
            title = "Bar effect",
            options = BarEffect.entries,
            selected = settings.barEffect,
            label = { it.label },
            onSelect = Appearance::setBarEffect,
            onDismiss = { showBarEffectDialog = false },
        )
    }

    if (showSpecDialog) {
        OptionDialog(
            icon = Icons.Outlined.Tune,
            title = "Color spec",
            options = offeredSpecs,
            selected = if (settings.colorSpec in offeredSpecs) settings.colorSpec else runningSpec,
            label = { it.label },
            onSelect = Appearance::setColorSpec,
            onDismiss = { showSpecDialog = false },
        )
    }
}

@Composable
private fun LiveScaleDialog(
    icon: ImageVector,
    title: String,
    description: String,
    currentScale: Float,
    min: Float,
    max: Float,
    step: Float,
    coerce: (Float) -> Float,
    onApply: (Float) -> Unit,
    onDismiss: () -> Unit,
    live: Boolean = true,
    onSaved: () -> Unit = {},
) {
    val original = rememberSaveable { coerce(currentScale) }
    var selected by rememberSaveable { mutableFloatStateOf(original) }
    var confirmed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (confirmed) onDismiss() }
    val cancel = {
        if (live && !confirmed) onApply(original)
        onDismiss()
    }
    val span = max - min
    val sliderTick = rememberStableHapticAction(HapticFeedbackType.SegmentTick)
    val sliderState =
        rememberSliderState(
            value = (selected - min) / span,
            steps = Math.round(span / step) - 1,
        )

    AppDialog(
        icon = icon,
        title = title,
        onDismiss = cancel,
        confirmButton = {
            DialogConfirmButton(if (live) "Save" else "Apply") {
                confirmed = true
                if (!live && selected != original) onApply(selected)
                onDismiss()
                if (selected != original) onSaved()
            }
        },
        dismissButton = { DialogCancelButton(label = "Cancel", onClick = cancel) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "${selected.toUiScalePercent()}%",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Slider(
                    state = sliderState,
                    onValueChange = {
                        val snapped = coerce(min + it * span)
                        if (snapped != selected) sliderTick()
                        if (live && snapped != selected) onApply(snapped)
                        selected = snapped
                        sliderState.value = (snapped - min) / span
                    },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${min.toUiScalePercent()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${max.toUiScalePercent()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
internal fun BackgroundGroup() {
    val cs = MaterialTheme.colorScheme
    val settings by Appearance.settings.collectAsStateWithLifecycle()
    val event = remember { SeasonalEvent.on() }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showIntervalDialog by rememberSaveable { mutableStateOf(false) }
    val none = settings.backgroundType == BackgroundType.NONE
    val random = settings.backgroundType == BackgroundType.RANDOM
    val count = if (random) 4 else 3
    val types = remember { BackgroundType.entries.filter { it !in BackgroundType.SEASONAL } }

    SettingGroup(
        icon = Icons.Outlined.Animation,
        title = "Animated background",
        initiallyExpanded = false,
    ) {
        PrefItem(
            icon = Icons.Outlined.Animation,
            title = "Choose background",
            subtitle = "${settings.backgroundType.label}: ${settings.backgroundType.description}",
            shape = itemShape(0, count),
            container = itemContainer(),
            content = cs.onSurface,
            badge = cs.primary,
            badgeTint = cs.onPrimary,
            onClick = { showPicker = true },
        )
        if (random) {
            PrefItem(
                icon = Icons.Outlined.Shuffle,
                title = "Random interval",
                subtitle = settings.randomInterval.label,
                shape = itemShape(1, count),
                container = itemContainer(),
                content = cs.onSurface,
                badge = cs.primary,
                badgeTint = cs.onPrimary,
                onClick = { showIntervalDialog = true },
            )
        }
        SwitchItem(
            icon = Icons.Outlined.Layers,
            title = "Background parallax",
            subtitle =
                if (none) {
                    "Choose an animated background first"
                } else {
                    "Gently shifts background as you tilt your phone"
                },
            shape = itemShape(count - 2, count),
            checked = settings.backgroundParallax,
            enabled = !none,
            onChange = Appearance::setBackgroundParallax,
        )
        SwitchItem(
            icon = Icons.Outlined.Celebration,
            title = "Seasonal themes",
            subtitle =
                when {
                    none -> "Choose an animated background first"
                    event != null -> "${event.label} active, special background shown until ${event.endLabel}"
                    else -> "Special backgrounds during Halloween, Winter Holidays, and New Year"
                },
            shape = itemShape(count - 1, count),
            checked = settings.seasonalThemes,
            enabled = !none,
            onChange = Appearance::setSeasonalThemes,
        )
    }

    if (showPicker) {
        OptionDialog(
            icon = Icons.Outlined.Animation,
            title = "Animated background",
            options = types,
            selected = settings.backgroundType,
            label = { it.label },
            onSelect = Appearance::setBackgroundType,
            onDismiss = { showPicker = false },
        )
    }
    if (showIntervalDialog) {
        OptionDialog(
            icon = Icons.Outlined.Shuffle,
            title = "Random interval",
            options = RandomInterval.entries,
            selected = settings.randomInterval,
            label = { it.label },
            onSelect = Appearance::setRandomInterval,
            onDismiss = { showIntervalDialog = false },
        )
    }
}

@Composable
internal fun NightModeGroup() {
    val settings by Appearance.settings.collectAsStateWithLifecycle()
    SettingGroup(
        icon = Icons.Outlined.BrightnessMedium,
        title = "Night mode",
        initiallyExpanded = false,
    ) {
        val options =
            listOf(
                Triple("Dark", Icons.Outlined.DarkMode, ThemeMode.DARK),
                Triple("Light", Icons.Outlined.LightMode, ThemeMode.LIGHT),
                Triple("System", Icons.Outlined.SettingsSuggest, ThemeMode.SYSTEM),
            )
        options.forEachIndexed { index, (label, icon, mode) ->
            NightModeItem(
                title = label,
                icon = icon,
                selected = settings.mode == mode,
                shape = itemShape(index, options.size),
                onClick = { Appearance.setMode(mode) },
            )
        }
    }
}

@Composable
internal fun NightModeItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    PrefItem(
        icon = icon,
        title = title,
        subtitle = null,
        shape = shape,
        container = if (selected) cs.primaryContainer else itemContainer(),
        content = if (selected) cs.onPrimaryContainer else cs.onSurface,
        onClick = onClick,
        badge = if (selected) cs.onSecondaryContainer else cs.primary,
        badgeTint = if (selected) cs.secondaryContainer else cs.onPrimary,
        end = {
            Icon(
                if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
        },
    )
}
