package io.nekohasekai.sagernet.ui.theme

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ui.background.BackgroundType
import io.nekohasekai.sagernet.ui.background.RandomInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.abs

enum class ThemeMode(
    @StringRes val labelRes: Int,
) {
    SYSTEM(R.string.ui_theme_system),
    LIGHT(R.string.ui_theme_light),
    DARK(R.string.ui_theme_dark),
}

enum class AccentPalette(
    @StringRes val labelRes: Int,
    val seed: Color,
) {
    DEFAULT(R.string.ui_accent_default, Color(0xFF415F91)),
    OLIVE(R.string.ui_accent_olive, Color(0xFF4A672D)),
    PINK(R.string.ui_accent_pink, Color(0xFFB94073)),
    RED(R.string.ui_accent_red, Color(0xFFBA1A1A)),
    ORANGE(R.string.ui_accent_orange, Color(0xFF944A00)),
    AMBER(R.string.ui_accent_amber, Color(0xFF8C5300)),
    YELLOW(R.string.ui_accent_yellow, Color(0xFF795900)),
    LIME(R.string.ui_accent_lime, Color(0xFF5E6400)),
    GREEN(R.string.ui_accent_green, Color(0xFF006D39)),
    CYAN(R.string.ui_accent_cyan, Color(0xFF006A64)),
    TEAL(R.string.ui_accent_teal, Color(0xFF006874)),
    LIGHT_BLUE(R.string.ui_accent_light_blue, Color(0xFF00639B)),
    BLUE(R.string.ui_accent_blue, Color(0xFF335BBC)),
    INDIGO(R.string.ui_accent_indigo, Color(0xFF5355A9)),
    PURPLE(R.string.ui_accent_purple, Color(0xFF6750A4)),
    DEEP_PURPLE(R.string.ui_accent_deep_purple, Color(0xFF7E42A4)),
    BLUE_GREY(R.string.ui_accent_blue_grey, Color(0xFF575D7E)),
    BROWN(R.string.ui_accent_brown, Color(0xFF7D524A)),
    GREY(R.string.ui_accent_grey, Color(0xFF5F6162)),
}

enum class PaletteStyle(
    @StringRes val labelRes: Int,
) {
    TonalSpot(R.string.ui_palette_tonal_spot),
    Neutral(R.string.ui_palette_neutral),
    Vibrant(R.string.ui_palette_vibrant),
    Expressive(R.string.ui_palette_expressive),
    Rainbow(R.string.ui_palette_rainbow),
    FruitSalad(R.string.ui_palette_fruit_salad),
    Monochrome(R.string.ui_palette_monochrome),
    Fidelity(R.string.ui_palette_fidelity),
    Content(R.string.ui_palette_content),
    Cmf(R.string.ui_palette_cmf),
    ;

    val supportsSpec2025: Boolean
        get() = this == TonalSpot || this == Neutral || this == Vibrant || this == Expressive

    val forcesSpec2026: Boolean
        get() = this == Cmf

    val offeredSpecs: List<ColorSpec>
        get() =
            when {
                forcesSpec2026 -> listOf(ColorSpec.SPEC_2026)
                supportsSpec2025 -> listOf(ColorSpec.SPEC_2021, ColorSpec.SPEC_2025, ColorSpec.SPEC_2026)
                else -> listOf(ColorSpec.SPEC_2021, ColorSpec.SPEC_2026)
            }
}

enum class BarEffect(
    @StringRes val labelRes: Int,
) {
    BLUR(R.string.ui_bar_effect_blur),
    LIQUID_GLASS(R.string.ui_bar_effect_liquid_glass),
}

enum class NavigationBarStyle(
    @StringRes val labelRes: Int,
) {
    FLOATING(R.string.ui_nav_bar_floating),
    MATERIAL(R.string.ui_nav_bar_material),
}

enum class ColorSpec(
    @StringRes val labelRes: Int,
) {
    SPEC_2021(R.string.ui_color_spec_2021),
    SPEC_2025(R.string.ui_color_spec_2025),
    SPEC_2026(R.string.ui_color_spec_2026),
}

data class RoleOverrides(
    val primary: Int = 0,
    val secondary: Int = 0,
    val tertiary: Int = 0,
    val neutral: Int = 0,
    val neutralVariant: Int = 0,
    val error: Int = 0,
) {
    val isEmpty: Boolean
        get() = primary == 0 && secondary == 0 && tertiary == 0 && neutral == 0 && neutralVariant == 0 && error == 0

    val count: Int
        get() = listOf(primary, secondary, tertiary, neutral, neutralVariant, error).count { it != 0 }
}

enum class PaletteRole(
    @StringRes val labelRes: Int,
) {
    PRIMARY(R.string.ui_role_primary),
    SECONDARY(R.string.ui_role_secondary),
    TERTIARY(R.string.ui_role_tertiary),
    NEUTRAL(R.string.ui_role_neutral),
    NEUTRAL_VARIANT(R.string.ui_role_neutral_variant),
    ERROR(R.string.ui_role_error),
}

fun RoleOverrides.get(role: PaletteRole): Int =
    when (role) {
        PaletteRole.PRIMARY -> primary
        PaletteRole.SECONDARY -> secondary
        PaletteRole.TERTIARY -> tertiary
        PaletteRole.NEUTRAL -> neutral
        PaletteRole.NEUTRAL_VARIANT -> neutralVariant
        PaletteRole.ERROR -> error
    }

fun RoleOverrides.withRole(
    role: PaletteRole,
    argb: Int,
): RoleOverrides =
    when (role) {
        PaletteRole.PRIMARY -> copy(primary = argb)
        PaletteRole.SECONDARY -> copy(secondary = argb)
        PaletteRole.TERTIARY -> copy(tertiary = argb)
        PaletteRole.NEUTRAL -> copy(neutral = argb)
        PaletteRole.NEUTRAL_VARIANT -> copy(neutralVariant = argb)
        PaletteRole.ERROR -> copy(error = argb)
    }

enum class ContrastPreset(
    @StringRes val labelRes: Int,
    val value: Float,
) {
    REDUCED(R.string.ui_contrast_reduced, -1f),
    STANDARD(R.string.ui_contrast_standard, 0f),
    MEDIUM(R.string.ui_contrast_medium, 0.5f),
    HIGH(R.string.ui_contrast_high, 1f),
}

const val CONTRAST_MIN = -1f
const val CONTRAST_MAX = 1f
const val CONTRAST_STEP = 0.1f

fun Float.coerceToContrast(): Float =
    (Math.round(coerceIn(CONTRAST_MIN, CONTRAST_MAX) / CONTRAST_STEP) * CONTRAST_STEP).let {
        if (abs(it) < 0.01f) 0f else it
    }

fun Float.contrastLabel(context: Context): String {
    ContrastPreset.entries.firstOrNull { abs(it.value - this) < 0.01f }?.let { return context.getString(it.labelRes) }
    return String.format(Locale.US, "%+.1f", this)
}

data class AppearanceSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accent: AccentPalette = AccentPalette.DEFAULT,
    val amoled: Boolean = false,
    val blur: Boolean = true,
    val barEffect: BarEffect = BarEffect.BLUR,
    val navigationBarStyle: NavigationBarStyle = NavigationBarStyle.FLOATING,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec = ColorSpec.SPEC_2025,
    val contrast: Float = 0f,
    val customSeed: Int = 0,
    val cmfTertiarySeed: Int = 0,
    val roleOverrides: RoleOverrides = RoleOverrides(),
    val animateColorChange: Boolean = false,
    val colorAnimationMs: Int = COLOR_ANIM_DEFAULT_MS,
    val uiScale: Float = UI_SCALE_DEFAULT,
    val fontScale: Float = FONT_SCALE_DEFAULT,
    val autoScaleUi: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val backgroundType: BackgroundType = BackgroundType.NONE,
    val backgroundParallax: Boolean = true,
    val randomInterval: RandomInterval = RandomInterval.ON_LAUNCH,
    val seasonalThemes: Boolean = true,
)

val AppearanceSettings.seedColor: Color
    get() = if (customSeed != 0) Color(customSeed) else accent.seed

private object AppearancePrefKeys {
    const val FILE = "re_nekobox_appearance"
    const val MODE = "mode"
    const val DYNAMIC = "dynamic"
    const val ACCENT = "accent"
    const val AMOLED = "amoled"
    const val BLUR = "blur"
    const val BAR_EFFECT = "bar_effect"
    const val NAV_BAR_STYLE = "navigation_bar_style"
    const val PALETTE_STYLE = "palette_style"
    const val COLOR_SPEC = "color_spec"
    const val CONTRAST = "contrast"
    const val CUSTOM_SEED = "custom_seed"
    const val CMF_TERTIARY = "cmf_tertiary_seed"
    const val ROLE_PRIMARY = "role_primary"
    const val ROLE_SECONDARY = "role_secondary"
    const val ROLE_TERTIARY = "role_tertiary"
    const val ROLE_NEUTRAL = "role_neutral"
    const val ROLE_NEUTRAL_VARIANT = "role_neutral_variant"
    const val ROLE_ERROR = "role_error"
    const val ANIMATE_COLOR = "animate_color_change"
    const val COLOR_ANIM_MS = "color_animation_ms"
    const val UI_SCALE = "ui_scale"
    const val FONT_SCALE = "font_scale"
    const val AUTO_SCALE_UI = "auto_scale_ui"
    const val HAPTICS = "haptics_enabled"
    const val BACKGROUND_TYPE = "background_type"
    const val BACKGROUND_PARALLAX = "background_parallax"
    const val RANDOM_INTERVAL = "random_interval"
    const val SEASONAL_THEMES = "seasonal_themes"
    const val LAST_RANDOM = "last_random_background"
}

object Appearance {
    private lateinit var appContext: Context

    private val _settings = MutableStateFlow(AppearanceSettings())
    val settings: StateFlow<AppearanceSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        val p = appContext.getSharedPreferences(AppearancePrefKeys.FILE, Context.MODE_PRIVATE)
        val d = AppearanceSettings()
        _settings.value =
            AppearanceSettings(
                mode =
                    runCatching { ThemeMode.valueOf(p.getString(AppearancePrefKeys.MODE, null).orEmpty()) }
                        .getOrDefault(d.mode),
                dynamicColor = p.getBoolean(AppearancePrefKeys.DYNAMIC, d.dynamicColor),
                accent =
                    runCatching { AccentPalette.valueOf(p.getString(AppearancePrefKeys.ACCENT, null).orEmpty()) }
                        .getOrDefault(d.accent),
                amoled = p.getBoolean(AppearancePrefKeys.AMOLED, d.amoled),
                blur = p.getBoolean(AppearancePrefKeys.BLUR, d.blur),
                barEffect =
                    runCatching { BarEffect.valueOf(p.getString(AppearancePrefKeys.BAR_EFFECT, null).orEmpty()) }
                        .getOrDefault(d.barEffect),
                navigationBarStyle =
                    runCatching {
                        NavigationBarStyle.valueOf(p.getString(AppearancePrefKeys.NAV_BAR_STYLE, null).orEmpty())
                    }.getOrDefault(d.navigationBarStyle),
                paletteStyle =
                    runCatching { PaletteStyle.valueOf(p.getString(AppearancePrefKeys.PALETTE_STYLE, null).orEmpty()) }
                        .getOrDefault(d.paletteStyle),
                colorSpec =
                    runCatching { ColorSpec.valueOf(p.getString(AppearancePrefKeys.COLOR_SPEC, null).orEmpty()) }
                        .getOrDefault(d.colorSpec),
                contrast = p.getFloat(AppearancePrefKeys.CONTRAST, d.contrast).coerceToContrast(),
                customSeed = p.getInt(AppearancePrefKeys.CUSTOM_SEED, d.customSeed),
                cmfTertiarySeed = p.getInt(AppearancePrefKeys.CMF_TERTIARY, d.cmfTertiarySeed),
                roleOverrides =
                    RoleOverrides(
                        primary = p.getInt(AppearancePrefKeys.ROLE_PRIMARY, 0),
                        secondary = p.getInt(AppearancePrefKeys.ROLE_SECONDARY, 0),
                        tertiary = p.getInt(AppearancePrefKeys.ROLE_TERTIARY, 0),
                        neutral = p.getInt(AppearancePrefKeys.ROLE_NEUTRAL, 0),
                        neutralVariant = p.getInt(AppearancePrefKeys.ROLE_NEUTRAL_VARIANT, 0),
                        error = p.getInt(AppearancePrefKeys.ROLE_ERROR, 0),
                    ),
                animateColorChange = p.getBoolean(AppearancePrefKeys.ANIMATE_COLOR, d.animateColorChange),
                colorAnimationMs =
                    p.getInt(AppearancePrefKeys.COLOR_ANIM_MS, d.colorAnimationMs).coerceToColorAnimMs(),
                uiScale = p.getFloat(AppearancePrefKeys.UI_SCALE, d.uiScale).coerceToUiScale(),
                fontScale = p.getFloat(AppearancePrefKeys.FONT_SCALE, d.fontScale).coerceToFontScale(),
                autoScaleUi = p.getBoolean(AppearancePrefKeys.AUTO_SCALE_UI, d.autoScaleUi),
                hapticsEnabled = p.getBoolean(AppearancePrefKeys.HAPTICS, d.hapticsEnabled),
                backgroundType =
                    runCatching {
                        BackgroundType.valueOf(p.getString(AppearancePrefKeys.BACKGROUND_TYPE, null).orEmpty())
                    }.getOrDefault(d.backgroundType),
                backgroundParallax = p.getBoolean(AppearancePrefKeys.BACKGROUND_PARALLAX, d.backgroundParallax),
                randomInterval =
                    runCatching {
                        RandomInterval.valueOf(p.getString(AppearancePrefKeys.RANDOM_INTERVAL, null).orEmpty())
                    }.getOrDefault(d.randomInterval),
                seasonalThemes = p.getBoolean(AppearancePrefKeys.SEASONAL_THEMES, d.seasonalThemes),
            )
    }

    fun setMode(mode: ThemeMode) = update { it.copy(mode = mode) }

    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    fun setAccent(accent: AccentPalette) = update { it.copy(accent = accent) }

    fun setAmoled(enabled: Boolean) = update { it.copy(amoled = enabled) }

    fun setBlur(enabled: Boolean) = update { it.copy(blur = enabled) }

    fun setBarEffect(effect: BarEffect) = update { it.copy(barEffect = effect) }

    fun setNavigationBarStyle(style: NavigationBarStyle) = update { it.copy(navigationBarStyle = style) }

    fun setPaletteStyle(style: PaletteStyle) = update { it.copy(paletteStyle = style) }

    fun setColorSpec(spec: ColorSpec) = update { it.copy(colorSpec = spec) }

    fun setContrast(level: Float) = update { it.copy(contrast = level.coerceToContrast()) }

    fun setCustomSeed(argb: Int) = update { it.copy(customSeed = argb) }

    fun setCmfTertiarySeed(argb: Int) = update { it.copy(cmfTertiarySeed = argb) }

    fun setRoleOverride(
        role: PaletteRole,
        argb: Int,
    ) = update { it.copy(roleOverrides = it.roleOverrides.withRole(role, argb)) }

    fun clearRoleOverrides() = update { it.copy(roleOverrides = RoleOverrides()) }

    fun setAnimateColorChange(enabled: Boolean) = update { it.copy(animateColorChange = enabled) }

    fun setColorAnimationMs(ms: Int) = update { it.copy(colorAnimationMs = ms.coerceToColorAnimMs()) }

    fun setUiScale(scale: Float) = update { it.copy(uiScale = scale.coerceToUiScale()) }

    fun setFontScale(scale: Float) = update { it.copy(fontScale = scale.coerceToFontScale()) }

    fun setAutoScaleUi(enabled: Boolean) = update { it.copy(autoScaleUi = enabled) }

    fun setHapticsEnabled(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }

    fun setBackgroundType(type: BackgroundType) = update { it.copy(backgroundType = type) }

    fun setBackgroundParallax(enabled: Boolean) = update { it.copy(backgroundParallax = enabled) }

    fun setRandomInterval(interval: RandomInterval) = update { it.copy(randomInterval = interval) }

    fun setSeasonalThemes(enabled: Boolean) = update { it.copy(seasonalThemes = enabled) }

    fun lastRandomBackground(): String? =
        if (::appContext.isInitialized) {
            appContext
                .getSharedPreferences(AppearancePrefKeys.FILE, Context.MODE_PRIVATE)
                .getString(AppearancePrefKeys.LAST_RANDOM, null)
        } else {
            null
        }

    fun setLastRandomBackground(name: String) {
        if (!::appContext.isInitialized) return
        appContext
            .getSharedPreferences(AppearancePrefKeys.FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(AppearancePrefKeys.LAST_RANDOM, name)
            .apply()
    }

    private fun update(block: (AppearanceSettings) -> AppearanceSettings) {
        val next = block(_settings.value)
        _settings.value = next
        if (!::appContext.isInitialized) return
        appContext
            .getSharedPreferences(AppearancePrefKeys.FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(AppearancePrefKeys.MODE, next.mode.name)
            .putBoolean(AppearancePrefKeys.DYNAMIC, next.dynamicColor)
            .putString(AppearancePrefKeys.ACCENT, next.accent.name)
            .putBoolean(AppearancePrefKeys.AMOLED, next.amoled)
            .putBoolean(AppearancePrefKeys.BLUR, next.blur)
            .putString(AppearancePrefKeys.BAR_EFFECT, next.barEffect.name)
            .putString(AppearancePrefKeys.NAV_BAR_STYLE, next.navigationBarStyle.name)
            .putString(AppearancePrefKeys.PALETTE_STYLE, next.paletteStyle.name)
            .putString(AppearancePrefKeys.COLOR_SPEC, next.colorSpec.name)
            .putFloat(AppearancePrefKeys.CONTRAST, next.contrast)
            .putInt(AppearancePrefKeys.CUSTOM_SEED, next.customSeed)
            .putInt(AppearancePrefKeys.CMF_TERTIARY, next.cmfTertiarySeed)
            .putInt(AppearancePrefKeys.ROLE_PRIMARY, next.roleOverrides.primary)
            .putInt(AppearancePrefKeys.ROLE_SECONDARY, next.roleOverrides.secondary)
            .putInt(AppearancePrefKeys.ROLE_TERTIARY, next.roleOverrides.tertiary)
            .putInt(AppearancePrefKeys.ROLE_NEUTRAL, next.roleOverrides.neutral)
            .putInt(AppearancePrefKeys.ROLE_NEUTRAL_VARIANT, next.roleOverrides.neutralVariant)
            .putInt(AppearancePrefKeys.ROLE_ERROR, next.roleOverrides.error)
            .putBoolean(AppearancePrefKeys.ANIMATE_COLOR, next.animateColorChange)
            .putInt(AppearancePrefKeys.COLOR_ANIM_MS, next.colorAnimationMs)
            .putFloat(AppearancePrefKeys.UI_SCALE, next.uiScale)
            .putFloat(AppearancePrefKeys.FONT_SCALE, next.fontScale)
            .putBoolean(AppearancePrefKeys.AUTO_SCALE_UI, next.autoScaleUi)
            .putBoolean(AppearancePrefKeys.HAPTICS, next.hapticsEnabled)
            .putString(AppearancePrefKeys.BACKGROUND_TYPE, next.backgroundType.name)
            .putBoolean(AppearancePrefKeys.BACKGROUND_PARALLAX, next.backgroundParallax)
            .putString(AppearancePrefKeys.RANDOM_INTERVAL, next.randomInterval.name)
            .putBoolean(AppearancePrefKeys.SEASONAL_THEMES, next.seasonalThemes)
            .apply()
    }
}
