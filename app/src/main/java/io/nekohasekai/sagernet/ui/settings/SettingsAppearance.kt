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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.R
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
        title = stringResource(R.string.ui_scale_group),
        initiallyExpanded = false,
    ) {
        SwitchItem(
            icon = Icons.Outlined.AspectRatio,
            title = stringResource(R.string.ui_auto_scale),
            subtitle = stringResource(R.string.ui_auto_scale_desc),
            shape = itemShape(0, 3),
            checked = settings.autoScaleUi,
            enabled = true,
            onChange = Appearance::setAutoScaleUi,
        )
        PrefItem(
            icon = Icons.Outlined.ZoomIn,
            title = stringResource(R.string.ui_display_scale),
            subtitle =
                if (settings.autoScaleUi) {
                    stringResource(R.string.ui_scale_disabled_auto)
                } else {
                    stringResource(R.string.ui_display_scale_desc, settings.uiScale.toUiScalePercent())
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
            title = stringResource(R.string.ui_font_size),
            subtitle =
                if (settings.autoScaleUi) {
                    stringResource(R.string.ui_scale_disabled_auto)
                } else {
                    stringResource(R.string.ui_font_size_desc, settings.fontScale.toUiScalePercent())
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
            title = stringResource(R.string.ui_display_scale),
            description =
                stringResource(R.string.ui_display_scale_dialog_desc),
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
            title = stringResource(R.string.ui_font_size),
            description =
                stringResource(R.string.ui_font_size_dialog_desc),
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
        title = stringResource(R.string.ui_customization),
        initiallyExpanded = initiallyExpanded,
    ) {
        PrefItem(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.ui_color_scheme),
            subtitle = stringResource(R.string.ui_color_scheme_desc),
            shape = itemShape(0, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            enabled = !dynamicActive,
            onDisabledClick = {
                Toast.makeText(ctx, R.string.ui_color_scheme_disable_dynamic_toast, Toast.LENGTH_SHORT).show()
            },
            onClick = { showAccentSheet = true },
            badge = cs.primary,
            badgeTint = cs.onPrimary,
            end = { ColorSchemePreview() },
        )
        PrefItem(
            icon = Icons.Outlined.Colorize,
            title = stringResource(R.string.ui_custom_color),
            subtitle =
                if (settings.customSeed != 0) {
                    stringResource(R.string.ui_custom_color_active, settings.seedColor.toHexString())
                } else {
                    stringResource(R.string.ui_custom_color_desc)
                },
            shape = itemShape(1, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            enabled = !dynamicActive,
            onDisabledClick = {
                Toast.makeText(ctx, R.string.ui_custom_color_disable_dynamic_toast, Toast.LENGTH_SHORT).show()
            },
            onClick = { showCustomColorSheet = true },
            badge = cs.primary,
            badgeTint = cs.onPrimary,
        )
        PrefItem(
            icon = Icons.Outlined.Style,
            title = stringResource(R.string.ui_palette_style),
            subtitle = stringResource(settings.paletteStyle.labelRes),
            shape = itemShape(2, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            badge = cs.primary,
            badgeTint = cs.onPrimary,
            onClick = { showStyleDialog = true },
        )
        PrefItem(
            icon = Icons.Outlined.Tune,
            title = stringResource(R.string.ui_color_spec),
            subtitle =
                if (settings.paletteStyle.forcesSpec2026) {
                    stringResource(R.string.ui_color_spec_cmf_auto)
                } else if (runningSpec != settings.colorSpec) {
                    stringResource(R.string.ui_color_spec_fallback, stringResource(stringResource(settings.colorSpec.labelRes)Res), stringResource(runningSpec.labelRes), stringResource(settings.paletteStyle.labelRes))
                } else {
                    stringResource(settings.colorSpec.labelRes)
                },
            shape = itemShape(3, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            enabled = !settings.paletteStyle.forcesSpec2026,
            onDisabledClick = {
                Toast.makeText(ctx, R.string.ui_color_spec_cmf_toast, Toast.LENGTH_SHORT).show()
            },
            onClick = { showSpecDialog = true },
            badge = cs.primary,
            badgeTint = cs.onPrimary,
        )
        PrefItem(
            icon = Icons.Outlined.Contrast,
            title = stringResource(R.string.ui_contrast),
            subtitle = settings.contrast.contrastLabel(ctx),
            shape = itemShape(4, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            badge = cs.primary,
            badgeTint = cs.onPrimary,
            onClick = { showContrastDialog = true },
        )
        PrefItem(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.ui_per_role_colors),
            subtitle =
                if (settings.roleOverrides.isEmpty) {
                    stringResource(R.string.ui_per_role_colors_desc)
                } else {
                    stringResource(R.string.ui_per_role_colors_count, settings.roleOverrides.count)
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
            title = stringResource(R.string.ui_animate_color),
            subtitle = stringResource(R.string.ui_animate_color_desc),
            shape = itemShape(6, itemCount),
            checked = settings.animateColorChange,
            enabled = true,
            onChange = Appearance::setAnimateColorChange,
        )
        if (settings.animateColorChange) {
            PrefItem(
                icon = Icons.Outlined.Timer,
                title = stringResource(R.string.ui_animation_duration),
                subtitle = stringResource(R.string.ui_animation_duration_ms, settings.colorAnimationMs),
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
            title = stringResource(R.string.ui_dynamic_colors),
            subtitle =
                if (DynamicColorSupported) {
                    stringResource(R.string.ui_dynamic_colors_desc)
                } else {
                    stringResource(R.string.ui_requires_android_12)
                },
            shape = itemShape(7 + animExtra, itemCount),
            checked = dynamicActive,
            enabled = DynamicColorSupported,
            onChange = Appearance::setDynamicColor,
        )
        SwitchItem(
            icon = Icons.Outlined.Contrast,
            title = stringResource(R.string.ui_amoled_mode),
            subtitle = stringResource(R.string.ui_amoled_mode_desc),
            shape = itemShape(8 + animExtra, itemCount),
            checked = settings.amoled,
            enabled = true,
            onChange = Appearance::setAmoled,
        )
        SwitchItem(
            icon = Icons.Outlined.BlurOn,
            title = stringResource(R.string.ui_blur_effect),
            subtitle =
                if (BlurSupported) {
                    stringResource(R.string.ui_blur_effect_desc)
                } else {
                    stringResource(R.string.ui_requires_android_13)
                },
            shape = itemShape(9 + animExtra, itemCount),
            checked = settings.blur && BlurSupported,
            enabled = BlurSupported,
            onChange = Appearance::setBlur,
        )
        PrefItem(
            icon = Icons.Outlined.Tab,
            title = stringResource(R.string.ui_bar_effect),
            subtitle =
                if (barEffectEnabled) {
                    stringResource(R.string.ui_bar_effect_desc, stringResource(settings.barEffect.labelRes))
                } else {
                    stringResource(R.string.ui_bar_effect_enable_blur)
                },
            shape = itemShape(10 + animExtra, itemCount),
            container = itemContainer(),
            content = cs.onSurface,
            enabled = barEffectEnabled,
            onDisabledClick = {
                Toast.makeText(ctx, R.string.ui_bar_effect_enable_blur_toast, Toast.LENGTH_SHORT).show()
            },
            onClick = { showBarEffectDialog = true },
            badge = cs.primary,
            badgeTint = cs.onPrimary,
        )
        SwitchItem(
            icon = Icons.Outlined.TouchApp,
            title = stringResource(R.string.ui_haptics),
            subtitle = stringResource(R.string.ui_haptics_desc),
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
            title = stringResource(R.string.ui_palette_style),
            options = PaletteStyle.entries,
            selected = settings.paletteStyle,
            label = { ctx.getString(it.labelRes) },
            onSelect = Appearance::setPaletteStyle,
            onDismiss = { showStyleDialog = false },
        )
    }

    if (showBarEffectDialog) {
        OptionDialog(
            icon = Icons.Outlined.Tab,
            title = stringResource(R.string.ui_bar_effect),
            options = BarEffect.entries,
            selected = settings.barEffect,
            label = { ctx.getString(it.labelRes) },
            onSelect = Appearance::setBarEffect,
            onDismiss = { showBarEffectDialog = false },
        )
    }

    if (showSpecDialog) {
        OptionDialog(
            icon = Icons.Outlined.Tune,
            title = stringResource(R.string.ui_color_spec),
            options = offeredSpecs,
            selected = if (settings.colorSpec in offeredSpecs) settings.colorSpec else runningSpec,
            label = { ctx.getString(it.labelRes) },
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
            DialogConfirmButton(stringResource(if (live) R.string.ui_save else R.string.ui_apply)) {
                confirmed = true
                if (!live && selected != original) onApply(selected)
                onDismiss()
                if (selected != original) onSaved()
            }
        },
        dismissButton = { DialogCancelButton(label = stringResource(R.string.ui_cancel), onClick = cancel) },
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
    val ctx = LocalContext.current
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
        title = stringResource(R.string.ui_animated_background),
        initiallyExpanded = false,
    ) {
        PrefItem(
            icon = Icons.Outlined.Animation,
            title = stringResource(R.string.ui_choose_background),
            subtitle = "${stringResource(settings.backgroundType.labelRes)}: ${stringResource(settings.backgroundType.descriptionRes)}",
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
                title = stringResource(R.string.ui_random_interval),
                subtitle = stringResource(settings.randomInterval.labelRes),
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
            title = stringResource(R.string.ui_background_parallax),
            subtitle =
                if (none) {
                    stringResource(R.string.ui_background_pick_first)
                } else {
                    stringResource(R.string.ui_background_parallax_desc)
                },
            shape = itemShape(count - 2, count),
            checked = settings.backgroundParallax,
            enabled = !none,
            onChange = Appearance::setBackgroundParallax,
        )
        SwitchItem(
            icon = Icons.Outlined.Celebration,
            title = stringResource(R.string.ui_seasonal_themes),
            subtitle =
                when {
                    none -> stringResource(R.string.ui_background_pick_first)
                    event != null -> stringResource(R.string.ui_seasonal_themes_active, stringResource(event.labelRes), event.endLabel)
                    else -> stringResource(R.string.ui_seasonal_themes_desc)
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
            title = stringResource(R.string.ui_animated_background),
            options = types,
            selected = settings.backgroundType,
            label = { ctx.getString(it.labelRes) },
            onSelect = Appearance::setBackgroundType,
            onDismiss = { showPicker = false },
        )
    }
    if (showIntervalDialog) {
        OptionDialog(
            icon = Icons.Outlined.Shuffle,
            title = stringResource(R.string.ui_random_interval),
            options = RandomInterval.entries,
            selected = settings.randomInterval,
            label = { ctx.getString(it.labelRes) },
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
        title = stringResource(R.string.ui_night_mode),
        initiallyExpanded = false,
    ) {
        val options =
            listOf(
                Triple(R.string.ui_theme_dark, Icons.Outlined.DarkMode, ThemeMode.DARK),
                Triple(R.string.ui_theme_light, Icons.Outlined.LightMode, ThemeMode.LIGHT),
                Triple(R.string.ui_theme_system, Icons.Outlined.SettingsSuggest, ThemeMode.SYSTEM),
            )
        options.forEachIndexed { index, (labelRes, icon, mode) ->
            NightModeItem(
                title = stringResource(labelRes),
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
