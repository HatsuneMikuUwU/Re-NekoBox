package io.nekohasekai.sagernet.ui.components

import android.os.Build

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

val FloatingBarHeight = 56.dp
val FloatingBarHorizontalPadding = 16.dp
val FloatingBarTopPadding = 8.dp
val FloatingTabBarHeight = 64.dp
val FloatingTabBarMargin = 12.dp

val LocalBottomInset = compositionLocalOf { 0.dp }
val LocalBlurBackdrop = compositionLocalOf<LayerBackdrop?> { null }

@Composable
fun floatingTopBarSpace(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + FloatingBarTopPadding + FloatingBarHeight + 8.dp

@Composable
fun floatingTabBarSpace(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + FloatingTabBarMargin + FloatingTabBarHeight

@Composable
fun rememberBlurBackdrop(
    enabled: Boolean = true,
    background: Color = MaterialTheme.colorScheme.background,
): LayerBackdrop? {
    if (
        !enabled ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        !isRenderEffectSupported()
    ) {
        return null
    }
    return rememberLayerBackdrop {
        drawRect(background)
        drawContent()
    }
}

fun Modifier.blurBackdropSource(backdrop: LayerBackdrop?): Modifier =
    if (backdrop != null) this.layerBackdrop(backdrop) else this

@Composable
fun Modifier.blurEffect(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape,
    blendColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    blendAlpha: Float = 0.8f,
): Modifier {
    if (!enabled || backdrop == null) return this
    val blend = blendColor.copy(alpha = blendAlpha)
    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(BlendColorEntry(color = blend)),
            ),
        ),
    )
}

@Composable
fun Modifier.barGlass(
    backdrop: LayerBackdrop? = LocalBlurBackdrop.current,
    shape: Shape = CircleShape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    drawFlatBackground: Boolean = true,
    strokeCornerRadius: Dp = Dp.Infinity,
): Modifier =
    if (backdrop != null) {
        blurEffect(backdrop, shape = shape, blendColor = containerColor)
            .glassStroke(cornerRadius = strokeCornerRadius)
    } else {
        (if (drawFlatBackground) background(containerColor, shape) else this)
            .glassStroke(cornerRadius = strokeCornerRadius)
    }

private const val FADE_STEPS = 16
const val DEFAULT_SCRIM_ALPHA = 0.85f

private fun smoothStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

fun topScrim(color: Color, maxAlpha: Float = DEFAULT_SCRIM_ALPHA): Brush =
    Brush.verticalGradient(
        colorStops = Array(FADE_STEPS + 1) { i ->
            val t = i / FADE_STEPS.toFloat()
            t to color.copy(alpha = maxAlpha * (1f - smoothStep(t)))
        },
    )

fun bottomScrim(color: Color, maxAlpha: Float = DEFAULT_SCRIM_ALPHA): Brush =
    Brush.verticalGradient(
        colorStops = Array(FADE_STEPS + 1) { i ->
            val t = i / FADE_STEPS.toFloat()
            t to color.copy(alpha = maxAlpha * smoothStep(t))
        },
    )

@Composable
fun Modifier.glassStroke(
    cornerRadius: Dp = Dp.Infinity,
    strokeWidth: Dp = 1.dp,
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val top = if (isDark) Color.White.copy(alpha = 0x28 / 255f) else Color.White
    val bottom = if (isDark) Color.White.copy(alpha = 0x14 / 255f) else Color.White
    return drawWithContent {
        drawContent()
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@drawWithContent
        val strokePx = strokeWidth.toPx().coerceAtLeast(0f)
        if (strokePx <= 0f) return@drawWithContent
        val maxRadius = min(width, height) / 2f
        val radius = if (cornerRadius == Dp.Infinity) maxRadius else min(cornerRadius.toPx(), maxRadius)
        drawGlassStroke(top, bottom, radius, strokePx)
    }
}

private fun DrawScope.drawGlassStroke(
    topColor: Color,
    bottomColor: Color,
    radius: Float,
    strokePx: Float,
) {
    val outline = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius))) }
    clipPath(outline) {
        if (topColor.alpha > 0f) drawEdgeStroke(topColor, radius, strokePx, isTop = true)
        if (bottomColor.alpha > 0f) drawEdgeStroke(bottomColor, radius, strokePx * 2f / 3f, isTop = false)
    }
}

private fun DrawScope.drawEdgeStroke(
    color: Color,
    radius: Float,
    strokePx: Float,
    isTop: Boolean,
) {
    val half = strokePx / 2f
    val clipTop = if (isTop) 0f else max(size.height - radius * 2f, 0f)
    val clipBottom = if (isTop) min(radius * 2f, size.height) else size.height
    val offsetY = if (isTop) half else -half
    clipRect(left = -half, top = clipTop, right = size.width + half, bottom = clipBottom) {
        drawRoundRect(
            color = color,
            topLeft = Offset(-half, offsetY),
            size = Size(size.width + strokePx, size.height),
            cornerRadius = CornerRadius(radius),
            style = Stroke(width = strokePx),
        )
    }
}

@Composable
fun FloatingGlassButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = LocalBlurBackdrop.current,
    size: Dp = FloatingBarHeight,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(size)
            .barGlass(backdrop = backdrop, shape = CircleShape, containerColor = MaterialTheme.colorScheme.surfaceContainer)
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}

@Composable
fun FloatingTopBar(
    title: String,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = LocalBlurBackdrop.current,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val brush = remember(cs.background) { topScrim(cs.background) }
    val fadeExtra = 16.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(brush, size = Size(size.width, size.height + fadeExtra.toPx()))
            }
            .statusBarsPadding()
            .padding(horizontal = FloatingBarHorizontalPadding)
            .padding(top = FloatingBarTopPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                FloatingGlassButton(
                    contentDescription = "Back",
                    onClick = onBack,
                    backdrop = backdrop,
                    size = FloatingBarHeight,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = cs.onSurface)
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(FloatingBarHeight)
                    .barGlass(backdrop = backdrop, shape = CircleShape, containerColor = cs.surfaceContainer)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
                    ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (actions != null) {
                Row(
                    modifier = Modifier
                        .height(FloatingBarHeight)
                        .barGlass(backdrop = backdrop, shape = CircleShape, containerColor = cs.surfaceContainer)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
                        actions()
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = LocalBlurBackdrop.current,
    autoFocus: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val brush = remember(cs.background) { topScrim(cs.background) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val fadeExtra = 16.dp

    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(brush, size = Size(size.width, size.height + fadeExtra.toPx()))
            }
            .statusBarsPadding()
            .padding(horizontal = FloatingBarHorizontalPadding)
            .padding(top = FloatingBarTopPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                FloatingGlassButton(
                    contentDescription = "Back",
                    onClick = onBack,
                    backdrop = backdrop,
                    size = FloatingBarHeight,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = cs.onSurface)
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(FloatingBarHeight)
                    .barGlass(backdrop = backdrop, shape = CircleShape, containerColor = cs.surfaceContainer)
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = cs.onSurfaceVariant,
                        )
                    }
                }
            }

            if (actions != null) {
                Row(
                    modifier = Modifier
                        .height(FloatingBarHeight)
                        .barGlass(backdrop = backdrop, shape = CircleShape, containerColor = cs.surfaceContainer)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
                        actions()
                    }
                }
            }
        }
    }
}

@Composable
fun <T> FloatingTabBar(
    items: List<T>,
    selectedIndex: () -> Int,
    onSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = LocalBlurBackdrop.current,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    activeContentColor: Color = indicatorColor,
    tabMinWidth: Dp = 76.dp,
    iconContent: @Composable (item: T, index: Int) -> Unit,
    labelContent: @Composable (item: T, index: Int) -> Unit,
) {
    val pillShape = CircleShape
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val tabsCount = items.size

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).coerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex().coerceIn(0, (tabsCount - 1).coerceAtLeast(0))) }
    val selectedIndexUpdated by rememberUpdatedState(selectedIndex)
    val onSelectedUpdated by rememberUpdatedState(onSelected)

    val valueAnimation = remember { Animatable(currentIndex.toFloat()) }
    val valueSpec = remember { spring<Float>(1f, 1000f, 0.001f) }
    val settleSpec = remember { spring<Float>(1f, 300f, 0.5f) }

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx).toInt().coerceIn(0, tabsCount - 1)
    }

    fun settleOffset() {
        scope.launch { offsetAnimation.animateTo(0f, settleSpec) }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { selectedIndexUpdated() }.collectLatest { index ->
            val safe = index.coerceIn(0, (tabsCount - 1).coerceAtLeast(0))
            if (currentIndex != safe) {
                currentIndex = safe
                valueAnimation.animateTo(safe.toFloat(), valueSpec)
            }
        }
    }

    fun activateTab(index: Int) {
        if (currentIndex != index) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            currentIndex = index
            onSelectedUpdated(index)
        }
        scope.launch { valueAnimation.animateTo(index.toFloat(), valueSpec) }
    }

    val dragModifier = Modifier.pointerInput(tabsCount, isLtr) {
        inspectDragGestures(
            onDragStart = { down ->
                scope.launch { valueAnimation.animateTo(indexAt(down.position.x).toFloat(), valueSpec) }
            },
            onDragEnd = {
                val target = valueAnimation.targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != target) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentIndex = target
                    onSelectedUpdated(target)
                }
                scope.launch { valueAnimation.animateTo(target.toFloat(), valueSpec) }
                settleOffset()
            },
            onDragCancel = {
                scope.launch { valueAnimation.animateTo(currentIndex.toFloat(), valueSpec) }
                settleOffset()
            },
        ) { change, dragAmount ->
            val inside = change.position.x in 0f..totalWidthPx && change.previousPosition.x in 0f..totalWidthPx
            if (inside && tabWidthPx > 0f && dragAmount.x != 0f) {
                val next = (valueAnimation.targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                    .coerceIn(0f, (tabsCount - 1).toFloat())
                scope.launch { valueAnimation.animateTo(next, valueSpec) }
                scope.launch { offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x) }
            }
        }
    }

    val tabsContent: @Composable RowScope.(Color) -> Unit = { color ->
        CompositionLocalProvider(LocalContentColor provides color) {
            items.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier
                        .defaultMinSize(minWidth = tabMinWidth)
                        .semantics(mergeDescendants = true) {
                            selected = index == currentIndex
                            role = Role.Tab
                            onClick {
                                activateTab(index)
                                true
                            }
                        }
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    iconContent(item, index)
                    labelContent(item, index)
                }
            }
        }
    }

    Box(
        modifier = modifier.width(IntrinsicSize.Min),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                }
                .graphicsLayer { translationX = panelOffset }
                .barGlass(backdrop = backdrop, shape = pillShape, containerColor = containerColor)
                .then(dragModifier)
                .height(FloatingTabBarHeight)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabsContent(contentColor)
        }

        if (tabWidthPx > 0f) {
            val tabWidthDp = with(density) { tabWidthPx.toDp() }
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        val progressOffset = valueAnimation.value * tabWidthPx
                        translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                    }
                    .clip(pillShape)
                    .background(indicatorColor.copy(alpha = 0.15f), pillShape)
                    .height(FloatingTabBarHeight - 8.dp)
                    .width(tabWidthDp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier
                        .clearAndSetSemantics {}
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .requiredWidth(with(density) { (totalWidthPx - 8.dp.toPx()).toDp() })
                        .height(FloatingTabBarHeight - 8.dp)
                        .graphicsLayer {
                            val progressOffset = valueAnimation.value * tabWidthPx
                            translationX = if (isLtr) -progressOffset else progressOffset
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabsContent(activeContentColor)
                }
            }
        }
    }
}

private suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(false, PointerEventPass.Initial)
        val down = awaitFirstDown(false)
        onDragStart(down)
        onDrag(initialDown, Offset.Zero)
        val upEvent = drag(
            pointerId = initialDown.id,
            onDrag = { onDrag(it, it.positionChange()) },
        )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) return null
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) return null
        if (change.changedToUpIgnoreConsumed()) return change
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) return dragEvent
        }
    }
}
