package io.nekohasekai.sagernet.ui.background

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import io.nekohasekai.sagernet.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

enum class BackgroundType(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
) {
    NONE(R.string.ui_bg_none, R.string.ui_bg_none_desc),
    CIRCLES(R.string.ui_bg_circles, R.string.ui_bg_circles_desc),
    RINGS(R.string.ui_bg_rings, R.string.ui_bg_rings_desc),
    MESH(R.string.ui_bg_mesh, R.string.ui_bg_mesh_desc),
    SPACE(R.string.ui_bg_space, R.string.ui_bg_space_desc),
    SHAPES(R.string.ui_bg_shapes, R.string.ui_bg_shapes_desc),
    SNOW(R.string.ui_bg_snow, R.string.ui_bg_snow_desc),
    GRID(R.string.ui_bg_grid, R.string.ui_bg_grid_desc),
    PARTICLES(R.string.ui_bg_particles, R.string.ui_bg_particles_desc),
    LAVA(R.string.ui_bg_lava, R.string.ui_bg_lava_desc),
    HALLOWEEN(R.string.ui_bg_halloween, R.string.ui_bg_halloween_desc),
    FIREWORKS(R.string.ui_bg_fireworks, R.string.ui_bg_fireworks_desc),
    RANDOM(R.string.ui_bg_random, R.string.ui_bg_random_desc),
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
