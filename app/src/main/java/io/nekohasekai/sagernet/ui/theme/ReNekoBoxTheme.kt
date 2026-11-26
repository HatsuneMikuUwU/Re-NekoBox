package io.nekohasekai.sagernet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.utils.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive theme state holder so theme & night-mode updates animate seamlessly
 * across all Compose screens without requiring harsh Activity restarts.
 */
object AppThemeState {
    private val _appTheme = MutableStateFlow(DataStore.appTheme)
    val appTheme = _appTheme.asStateFlow()

    private val _nightTheme = MutableStateFlow(DataStore.nightTheme)
    val nightTheme = _nightTheme.asStateFlow()

    fun updateAppTheme(themeId: Int) {
        DataStore.appTheme = themeId
        _appTheme.value = themeId
    }

    fun updateNightTheme(mode: Int) {
        DataStore.nightTheme = mode
        Theme.currentNightMode = mode
        Theme.applyNightTheme()
        _nightTheme.value = mode
    }

    fun syncFromDataStore() {
        _appTheme.value = DataStore.appTheme
        _nightTheme.value = DataStore.nightTheme
    }
}

data class ThemeSeed(
    val id: Int,
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val isOledBlack: Boolean = false,
)

val ReNekoBoxThemeSeeds: List<ThemeSeed> = listOf(
    ThemeSeed(0, "Dynamic (Material You)", Color(0xFFFB7299), Color(0xFF7B5260), Color(0xFF7E5700)),
    ThemeSeed(Theme.RED, "Red", Color(0xFFF44336), Color(0xFF775652), Color(0xFF715B2E)),
    ThemeSeed(Theme.PINK_SSR, "Pink SSR", Color(0xFFFB7299), Color(0xFF74565F), Color(0xFF7C5635)),
    ThemeSeed(Theme.PINK, "Pink", Color(0xFFE91E63), Color(0xFF74565E), Color(0xFF7D5635)),
    ThemeSeed(Theme.PURPLE, "Purple", Color(0xFF9C27B0), Color(0xFF6B586B), Color(0xFF82524A)),
    ThemeSeed(Theme.DEEP_PURPLE, "Deep Purple", Color(0xFF673AB7), Color(0xFF625B71), Color(0xFF7E5260)),
    ThemeSeed(Theme.INDIGO, "Indigo", Color(0xFF3F51B5), Color(0xFF5B5D72), Color(0xFF77536D)),
    ThemeSeed(Theme.BLUE, "Blue", Color(0xFF2196F3), Color(0xFF535F70), Color(0xFF6B5778)),
    ThemeSeed(Theme.LIGHT_BLUE, "Light Blue", Color(0xFF03A9F4), Color(0xFF4F616E), Color(0xFF625A7C)),
    ThemeSeed(Theme.CYAN, "Cyan", Color(0xFF00BCD4), Color(0xFF4A6267), Color(0xFF525E7D)),
    ThemeSeed(Theme.TEAL, "Teal", Color(0xFF009688), Color(0xFF4A635E), Color(0xFF456179)),
    ThemeSeed(Theme.GREEN, "Green", Color(0xFF4CAF50), Color(0xFF52634F), Color(0xFF39656B)),
    ThemeSeed(Theme.LIGHT_GREEN, "Light Green", Color(0xFF8BC34A), Color(0xFF57624A), Color(0xFF386663)),
    ThemeSeed(Theme.LIME, "Lime", Color(0xFFAFB42B), Color(0xFF5F6144), Color(0xFF3C665A)),
    ThemeSeed(Theme.YELLOW, "Yellow", Color(0xFFFBC02D), Color(0xFF675E40), Color(0xFF45664C)),
    ThemeSeed(Theme.AMBER, "Amber", Color(0xFFFFC107), Color(0xFF6B5D3F), Color(0xFF4B6546)),
    ThemeSeed(Theme.ORANGE, "Orange", Color(0xFFFF9800), Color(0xFF735A42), Color(0xFF58633A)),
    ThemeSeed(Theme.DEEP_ORANGE, "Deep Orange", Color(0xFFFF5722), Color(0xFF77574D), Color(0xFF6B5E2F)),
    ThemeSeed(Theme.BROWN, "Brown", Color(0xFF795548), Color(0xFF6E5A52), Color(0xFF635F35)),
    ThemeSeed(Theme.GREY, "Grey", Color(0xFF757575), Color(0xFF5E5E5E), Color(0xFF605D64)),
    ThemeSeed(Theme.BLUE_GREY, "Blue Grey", Color(0xFF607D8B), Color(0xFF526067), Color(0xFF635A70)),
    ThemeSeed(Theme.BLACK, "OLED Black", Color(0xFF212121), Color(0xFF5C5F66), Color(0xFF705860), isOledBlack = true),
    ThemeSeed(Theme.VERDANT_MINT, "Verdant Mint", Color(0xFF00B96B), Color(0xFF4B6356), Color(0xFF3C6472)),
)

private fun buildExpressiveLightScheme(seed: ThemeSeed): ColorScheme {
    val primary = if (seed.isOledBlack) Color(0xFF1F1F1F) else seed.primary
    val onPrimary = if (primary.luminance() > 0.55f) Color(0xFF1A1A1A) else Color.White
    val primaryContainer = lerp(primary, Color.White, 0.80f)
    val onPrimaryContainer = lerp(primary, Color.Black, 0.65f)
    val secondaryContainer = lerp(seed.secondary, Color.White, 0.82f)
    val onSecondaryContainer = lerp(seed.secondary, Color.Black, 0.65f)
    val tertiaryContainer = lerp(seed.tertiary, Color.White, 0.82f)
    val onTertiaryContainer = lerp(seed.tertiary, Color.Black, 0.65f)
    val surfaceTintBase = lerp(Color(0xFFFCF8F9), primary, 0.03f)

    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = seed.secondary,
        onSecondary = Color.White,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = seed.tertiary,
        onTertiary = Color.White,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = surfaceTintBase,
        onBackground = Color(0xFF1C1B1F),
        surface = surfaceTintBase,
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = lerp(Color(0xFFE7E0EC), primary, 0.06f),
        onSurfaceVariant = Color(0xFF49454F),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = lerp(Color(0xFFF7F2F5), primary, 0.04f),
        surfaceContainer = lerp(Color(0xFFF3EDF2), primary, 0.06f),
        surfaceContainerHigh = lerp(Color(0xFFECE6EC), primary, 0.08f),
        surfaceContainerHighest = lerp(Color(0xFFE6E0E6), primary, 0.11f),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )
}

private fun buildExpressiveDarkScheme(seed: ThemeSeed): ColorScheme {
    val primary = if (seed.isOledBlack) Color(0xFFE2E2E6) else lerp(seed.primary, Color.White, 0.35f)
    val onPrimary = if (seed.isOledBlack) Color(0xFF121212) else lerp(seed.primary, Color.Black, 0.72f)
    val primaryContainer = if (seed.isOledBlack) Color(0xFF2B2B2F) else lerp(seed.primary, Color.Black, 0.58f)
    val onPrimaryContainer = lerp(primary, Color.White, 0.75f)
    val secondary = lerp(seed.secondary, Color.White, 0.42f)
    val secondaryContainer = lerp(seed.secondary, Color.Black, 0.55f)
    val tertiary = lerp(seed.tertiary, Color.White, 0.42f)
    val tertiaryContainer = lerp(seed.tertiary, Color.Black, 0.55f)

    val bg = if (seed.isOledBlack) Color(0xFF000000) else lerp(Color(0xFF121215), seed.primary, 0.03f)
    val surfaceLow = if (seed.isOledBlack) Color(0xFF0E0E10) else lerp(Color(0xFF1A1A1E), seed.primary, 0.05f)
    val surfaceMid = if (seed.isOledBlack) Color(0xFF16161A) else lerp(Color(0xFF202026), seed.primary, 0.07f)
    val surfaceHigh = if (seed.isOledBlack) Color(0xFF1F1F24) else lerp(Color(0xFF2A2930), seed.primary, 0.09f)
    val surfaceHighest = if (seed.isOledBlack) Color(0xFF28282E) else lerp(Color(0xFF35343C), seed.primary, 0.11f)

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = Color(0xFF1E1E22),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = Color(0xFFE6E1E5),
        tertiary = tertiary,
        onTertiary = Color(0xFF1E1E22),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = Color(0xFFE6E1E5),
        background = bg,
        onBackground = Color(0xFFE6E1E5),
        surface = bg,
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceContainerLowest = if (seed.isOledBlack) Color.Black else Color(0xFF0D0D10),
        surfaceContainerLow = surfaceLow,
        surfaceContainer = surfaceMid,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHighest,
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
}

/**
 * Material 3 Expressive Shape Scale: bolder, friendlier radii for cards,
 * hero containers, pills, and grouped preference surfaces.
 */
val ReNekoBoxExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Material 3 Expressive Typography Scale: emphasized display/headline/title
 * weights with tight tracking for scannable proxy cards and dashboards.
 */
val ReNekoBoxExpressiveTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun ReNekoBoxTheme(
    content: @Composable () -> Unit,
) {
    val themeId by AppThemeState.appTheme.collectAsState()
    val nightMode by AppThemeState.nightTheme.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val darkTheme = when (nightMode) {
        1 -> true
        2 -> false
        else -> systemDark
    }

    val context = LocalContext.current
    val colorScheme = if (themeId == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val seed = ReNekoBoxThemeSeeds.firstOrNull { it.id == themeId }
            ?: ReNekoBoxThemeSeeds.first { it.id == Theme.PINK_SSR }
        if (darkTheme) buildExpressiveDarkScheme(seed) else buildExpressiveLightScheme(seed)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ReNekoBoxExpressiveShapes,
        typography = ReNekoBoxExpressiveTypography,
        content = content,
    )
}
