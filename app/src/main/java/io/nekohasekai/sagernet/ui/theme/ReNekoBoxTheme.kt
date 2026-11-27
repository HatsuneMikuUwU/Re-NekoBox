package io.nekohasekai.sagernet.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.PaletteStyle as KolorPaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec as KolorColorSpec
import com.materialkolor.material3.DynamicMaterialExpressiveTheme
import com.materialkolor.material3.dynamicColorScheme as kolorScheme
import com.materialkolor.material3.rememberDynamicMaterialThemeState

val ReNekoBoxExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

val ReNekoBoxExpressiveTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 62.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 50.sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
    ),
)

val DynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun PaletteStyle.toKolor(cmfTertiarySeed: Int = 0): KolorPaletteStyle =
    when (this) {
        PaletteStyle.TonalSpot -> KolorPaletteStyle.TonalSpot
        PaletteStyle.Neutral -> KolorPaletteStyle.Neutral
        PaletteStyle.Vibrant -> KolorPaletteStyle.Vibrant
        PaletteStyle.Expressive -> KolorPaletteStyle.Expressive
        PaletteStyle.Rainbow -> KolorPaletteStyle.Rainbow
        PaletteStyle.FruitSalad -> KolorPaletteStyle.FruitSalad
        PaletteStyle.Monochrome -> KolorPaletteStyle.Monochrome
        PaletteStyle.Fidelity -> KolorPaletteStyle.Fidelity
        PaletteStyle.Content -> KolorPaletteStyle.Content
        PaletteStyle.Cmf -> KolorPaletteStyle.Cmf(
            tertiarySeedColor = cmfTertiarySeed.takeIf { it != 0 }?.let { Color(it) },
        )
    }

fun resolveSpecVersion(
    style: PaletteStyle,
    spec: ColorSpec,
): KolorColorSpec.SpecVersion =
    when {
        spec == ColorSpec.SPEC_2026 -> KolorColorSpec.SpecVersion.SPEC_2026
        spec == ColorSpec.SPEC_2025 && style.supportsSpec2025 -> KolorColorSpec.SpecVersion.SPEC_2025
        else -> KolorColorSpec.SpecVersion.SPEC_2021
    }

fun effectiveSpec(
    style: PaletteStyle,
    spec: ColorSpec,
): ColorSpec =
    when {
        style.forcesSpec2026 -> ColorSpec.SPEC_2026
        !style.supportsSpec2025 -> ColorSpec.SPEC_2021
        spec == ColorSpec.SPEC_2026 -> ColorSpec.SPEC_2025
        else -> spec
    }

fun paletteColorScheme(
    seed: Color,
    dark: Boolean,
    style: PaletteStyle,
    spec: ColorSpec,
    contrast: Float = 0f,
    overrides: RoleOverrides = RoleOverrides(),
    cmfTertiarySeed: Int = 0,
    amoled: Boolean = false,
): ColorScheme =
    kolorScheme(
        seedColor = seed,
        isDark = dark,
        isAmoled = dark && amoled,
        primary = overrides.primary.toColorOrNull(),
        secondary = overrides.secondary.toColorOrNull(),
        tertiary = overrides.tertiary.toColorOrNull(),
        neutral = overrides.neutral.toColorOrNull(),
        neutralVariant = overrides.neutralVariant.toColorOrNull(),
        error = overrides.error.toColorOrNull(),
        style = style.toKolor(cmfTertiarySeed),
        contrastLevel = contrast.toDouble().coerceIn(-1.0, 1.0),
        specVersion = resolveSpecVersion(style, spec),
    )

private fun Int.toColorOrNull(): Color? = if (this == 0) null else Color(this)

@Composable
fun rememberAppDarkTheme(mode: ThemeMode): Boolean =
    when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

@Composable
fun ReNekoBoxTheme(
    settings: AppearanceSettings? = null,
    content: @Composable () -> Unit,
) {
    val observed by Appearance.settings.collectAsStateWithLifecycle()
    val active = settings ?: observed
    val darkTheme = rememberAppDarkTheme(active.mode)

    val systemSeed = if (DynamicColorSupported) colorResource(android.R.color.system_accent1_500) else null
    val seed = systemSeed.takeIf { active.dynamicColor } ?: active.seedColor
    val overrides = active.roleOverrides
    val state =
        rememberDynamicMaterialThemeState(
            seedColor = seed,
            isDark = darkTheme,
            isAmoled = darkTheme && active.amoled,
            primary = overrides.primary.toColorOrNull(),
            secondary = overrides.secondary.toColorOrNull(),
            tertiary = overrides.tertiary.toColorOrNull(),
            neutral = overrides.neutral.toColorOrNull(),
            neutralVariant = overrides.neutralVariant.toColorOrNull(),
            error = overrides.error.toColorOrNull(),
            style = active.paletteStyle.toKolor(active.cmfTertiarySeed),
            contrastLevel = active.contrast.toDouble().coerceIn(-1.0, 1.0),
            specVersion = resolveSpecVersion(active.paletteStyle, active.colorSpec),
        )

    val colorSpec =
        remember(active.colorAnimationMs) {
            tween<Color>(durationMillis = active.colorAnimationMs)
        }

    DynamicMaterialExpressiveTheme(
        state = state,
        motionScheme = MotionScheme.expressive(),
        shapes = ReNekoBoxExpressiveShapes,
        typography = ReNekoBoxExpressiveTypography,
        animate = active.animateColorChange,
        animationSpec = colorSpec,
    ) {
        val view = LocalView.current
        val windowBg = MaterialTheme.colorScheme.background
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = Color.Transparent.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = Color.Transparent.toArgb()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                    window.setBackgroundDrawable(ColorDrawable(windowBg.toArgb()))
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            content()
        }
    }
}
