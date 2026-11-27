package io.nekohasekai.sagernet.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle as KolorPaletteStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class AppPaletteStyle(val label: String) {
    TonalSpot("Tonal Spot"),
    Vibrant("Vibrant"),
    Expressive("Expressive"),
    Neutral("Neutral"),
    Fidelity("Fidelity"),
    Content("Content"),
    Rainbow("Rainbow"),
    FruitSalad("Fruit Salad"),
    Monochrome("Monochrome");

    fun toKolor(): KolorPaletteStyle = when (this) {
        TonalSpot -> KolorPaletteStyle.TonalSpot
        Vibrant -> KolorPaletteStyle.Vibrant
        Expressive -> KolorPaletteStyle.Expressive
        Neutral -> KolorPaletteStyle.Neutral
        Fidelity -> KolorPaletteStyle.Fidelity
        Content -> KolorPaletteStyle.Content
        Rainbow -> KolorPaletteStyle.Rainbow
        FruitSalad -> KolorPaletteStyle.FruitSalad
        Monochrome -> KolorPaletteStyle.Monochrome
    }
}

enum class ContrastPreset(val label: String, val value: Float) {
    REDUCED("Reduced", -0.5f),
    DEFAULT("Default", 0f),
    MEDIUM("Medium", 0.5f),
    HIGH("High", 1.0f),
}

data class AppearanceSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    val seedColorArgb: Int = 0xFF6750A4.toInt(),
    val paletteStyle: AppPaletteStyle = AppPaletteStyle.TonalSpot,
    val contrast: Float = 0f,
    val amoled: Boolean = false,
    val blurEnabled: Boolean = true,
    val animateColorChange: Boolean = true,
    val colorAnimationMs: Int = 350,
    val uiScale: Float = 1.0f,
    val fontScale: Float = 1.0f,
) {
    val seedColor: Color get() = Color(seedColorArgb)
}

val PresetSeedColors: List<Pair<String, Int>> = listOf(
    "Violet" to 0xFF6750A4.toInt(),
    "Sakura" to 0xFFE91E63.toInt(),
    "Crimson" to 0xFFD32F2F.toInt(),
    "Tangerine" to 0xFFFF6D00.toInt(),
    "Amber" to 0xFFFFB300.toInt(),
    "Matcha" to 0xFF43A047.toInt(),
    "Teal" to 0xFF00897B.toInt(),
    "Miku Cyan" to 0xFF39C5BB.toInt(),
    "Sky" to 0xFF0288D1.toInt(),
    "Indigo" to 0xFF3949AB.toInt(),
    "Lavender" to 0xFF8E24AA.toInt(),
    "Slate" to 0xFF546E7A.toInt(),
)

object AppearanceStore {
    private const val PREF_NAME = "re_nekobox_appearance"
    private var prefs: SharedPreferences? = null

    private val _settings = MutableStateFlow(AppearanceSettings())
    val settings: StateFlow<AppearanceSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs = p
        _settings.value = AppearanceSettings(
            mode = ThemeMode.entries.getOrElse(p.getInt("mode", 0)) { ThemeMode.SYSTEM },
            dynamicColor = p.getBoolean("dynamicColor", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
            seedColorArgb = p.getInt("seedColorArgb", 0xFF6750A4.toInt()),
            paletteStyle = AppPaletteStyle.entries.getOrElse(p.getInt("paletteStyle", 0)) { AppPaletteStyle.TonalSpot },
            contrast = p.getFloat("contrast", 0f).coerceIn(-0.5f, 1.0f),
            amoled = p.getBoolean("amoled", false),
            blurEnabled = p.getBoolean("blurEnabled", true),
            animateColorChange = p.getBoolean("animateColorChange", true),
            colorAnimationMs = p.getInt("colorAnimationMs", 350).coerceIn(100, 1000),
            uiScale = p.getFloat("uiScale", 1.0f).coerceIn(0.85f, 1.15f),
            fontScale = p.getFloat("fontScale", 1.0f).coerceIn(0.85f, 1.20f),
        )
    }

    private fun update(transform: AppearanceSettings.() -> AppearanceSettings) {
        _settings.update { cur ->
            val next = cur.transform()
            prefs?.edit()?.apply {
                putInt("mode", next.mode.ordinal)
                putBoolean("dynamicColor", next.dynamicColor)
                putInt("seedColorArgb", next.seedColorArgb)
                putInt("paletteStyle", next.paletteStyle.ordinal)
                putFloat("contrast", next.contrast)
                putBoolean("amoled", next.amoled)
                putBoolean("blurEnabled", next.blurEnabled)
                putBoolean("animateColorChange", next.animateColorChange)
                putInt("colorAnimationMs", next.colorAnimationMs)
                putFloat("uiScale", next.uiScale)
                putFloat("fontScale", next.fontScale)
                apply()
            }
            next
        }
    }

    fun setMode(mode: ThemeMode) = update { copy(mode = mode) }
    fun setDynamicColor(enabled: Boolean) = update { copy(dynamicColor = enabled) }
    fun setSeedColor(argb: Int) = update { copy(seedColorArgb = argb, dynamicColor = false) }
    fun setPaletteStyle(style: AppPaletteStyle) = update { copy(paletteStyle = style) }
    fun setContrast(value: Float) = update { copy(contrast = value.coerceIn(-0.5f, 1.0f)) }
    fun setAmoled(enabled: Boolean) = update { copy(amoled = enabled) }
    fun setBlurEnabled(enabled: Boolean) = update { copy(blurEnabled = enabled) }
    fun setAnimateColorChange(enabled: Boolean) = update { copy(animateColorChange = enabled) }
    fun setColorAnimationMs(ms: Int) = update { copy(colorAnimationMs = ms.coerceIn(100, 1000)) }
    fun setUiScale(scale: Float) = update { copy(uiScale = scale.coerceIn(0.85f, 1.15f)) }
    fun setFontScale(scale: Float) = update { copy(fontScale = scale.coerceIn(0.85f, 1.20f)) }
    fun reset() = update { AppearanceSettings() }
}
