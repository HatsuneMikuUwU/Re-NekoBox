package io.nekohasekai.sagernet.ui.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

enum class BackgroundType(
    val label: String,
    val description: String,
) {
    NONE("Off", "Plain background without animation"),
    CIRCLES("Circles", "Soft circles drifting gently"),
    RINGS("Ripples", "Concentric rings rippling like water"),
    MESH("Mesh", "Undulating 3D low-poly mesh"),
    SPACE("Space", "Stars rushing toward you"),
    SHAPES("3D Shapes", "Spinning floating geometric solids"),
    SNOW("Snow", "Snowflakes drifting in the breeze"),
    GRID("Grid", "Breathing dots moving in waves"),
    PARTICLES("Particles", "Drifting interconnected particles"),
    LAVA("Lava", "Rising, falling, and merging lava blobs"),
    HALLOWEEN("Halloween", "Flying bats and glowing pumpkins under the moon"),
    FIREWORKS("Fireworks", "Rockets launching and bursting into colorful sparks"),
    RANDOM("Random", "Switches automatically on your chosen schedule"),
    ;

    companion object {
        val SEASONAL: Set<BackgroundType> = setOf(HALLOWEEN, FIREWORKS)

        val RANDOMIZABLE: List<BackgroundType> = entries.filter { it != NONE && it != RANDOM && it !in SEASONAL }
    }
}

val LocalAnimatedBackgroundActive = staticCompositionLocalOf { false }

@Composable
fun screenContainerColor(): Color =
    if (LocalAnimatedBackgroundActive.current) Color.Transparent else MaterialTheme.colorScheme.background

@Composable
fun AnimatedBackground(
    type: BackgroundType,
    modifier: Modifier = Modifier,
    enableParallax: Boolean = true,
) {
    if (type == BackgroundType.NONE) return

    Box(modifier.fillMaxSize().clipToBounds()) {
        when (type) {
            BackgroundType.CIRCLES -> CirclesBackground(enableParallax = enableParallax)
            BackgroundType.RINGS -> RingsBackground(enableParallax = enableParallax)
            BackgroundType.PARTICLES -> ParticlesBackground(enableParallax = enableParallax)
            BackgroundType.MESH -> MeshBackground(enableParallax = enableParallax)
            BackgroundType.SPACE -> SpaceBackground(enableParallax = enableParallax)
            BackgroundType.SHAPES -> ShapesBackground(enableParallax = enableParallax)
            BackgroundType.SNOW -> SnowBackground(enableParallax = enableParallax)
            BackgroundType.GRID -> GridBackground(enableParallax = enableParallax)
            BackgroundType.LAVA -> LavaBackground(enableParallax = enableParallax)
            BackgroundType.HALLOWEEN -> HalloweenBackground(enableParallax = enableParallax)
            BackgroundType.FIREWORKS -> FireworksBackground(enableParallax = enableParallax)
            BackgroundType.NONE, BackgroundType.RANDOM -> Unit
        }
    }
}

@Composable
internal fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
