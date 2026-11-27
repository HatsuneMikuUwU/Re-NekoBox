package io.nekohasekai.sagernet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import io.nekohasekai.sagernet.ui.theme.Appearance
import kotlinx.coroutines.channels.Channel

fun <T> List<T>.moveItem(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) return this
    return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}

@Composable
fun rememberDragDropState(
    listState: LazyListState,
    headerCount: Int = 0,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragEnd: () -> Unit = {},
): DragDropState {
    val haptic = LocalHapticFeedback.current
    val currentHeaderCount by rememberUpdatedState(headerCount)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val state = remember(listState) {
        DragDropState(
            listState = listState,
            headerCountProvider = { currentHeaderCount },
            onMove = { from, to -> currentOnMove(from, to) },
            onDragEnd = { currentOnDragEnd() },
            onHaptic = { type ->
                if (Appearance.settings.value.hapticsEnabled) {
                    haptic.performHapticFeedback(type)
                }
            },
        )
    }

    LaunchedEffect(state) {
        while (true) {
            val delta = state.scrollChannel.receive()
            listState.scrollBy(delta)
        }
    }

    return state
}

class DragDropState internal constructor(
    val listState: LazyListState,
    private val headerCountProvider: () -> Int,
    private val onMove: (Int, Int) -> Unit,
    private val onDragEnd: () -> Unit,
    private val onHaptic: (HapticFeedbackType) -> Unit,
) {
    var draggedIndex by mutableStateOf<Int?>(null)
        private set

    private var draggedDistance by mutableFloatStateOf(0f)
    private var initialDraggedItemOffset by mutableIntStateOf(0)
    private var initialDraggedItemSize by mutableIntStateOf(0)
    private var hasMoved = false

    internal val scrollChannel = Channel<Float>(Channel.CONFLATED)

    private val currentLazyItemInfo: LazyListItemInfo?
        get() {
            val idx = draggedIndex ?: return null
            val targetLazyIndex = idx + headerCountProvider()
            return listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetLazyIndex }
        }

    val draggedItemOffset: Float
        get() {
            val currentInfo = currentLazyItemInfo ?: return 0f
            return (initialDraggedItemOffset + draggedDistance) - currentInfo.offset
        }

    internal fun onDragStart(offset: Offset) {
        val headerCount = headerCountProvider()
        val adjustedY = offset.y.toInt() - listState.layoutInfo.beforeContentPadding
        val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index >= headerCount && adjustedY in item.offset..(item.offset + item.size)
        } ?: return

        draggedIndex = hit.index - headerCount
        initialDraggedItemOffset = hit.offset
        initialDraggedItemSize = hit.size
        draggedDistance = 0f
        hasMoved = false
        onHaptic(HapticFeedbackType.LongPress)
    }

    internal fun onDrag(dragAmount: Offset) {
        val currentDataIndex = draggedIndex ?: return
        draggedDistance += dragAmount.y

        val headerCount = headerCountProvider()
        val currentLazyIndex = currentDataIndex + headerCount
        val startOffset = initialDraggedItemOffset + draggedDistance
        val endOffset = startOffset + initialDraggedItemSize
        val middleOffset = (startOffset + endOffset) / 2f

        val targetItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index >= headerCount &&
                item.index != currentLazyIndex &&
                middleOffset >= item.offset &&
                middleOffset <= item.offset + item.size
        }

        if (targetItem != null) {
            val targetDataIndex = targetItem.index - headerCount
            if (targetDataIndex >= 0 && targetDataIndex != currentDataIndex) {
                onMove(currentDataIndex, targetDataIndex)
                draggedIndex = targetDataIndex
                hasMoved = true
                onHaptic(HapticFeedbackType.SegmentTick)
            }
        } else {
            val overscroll = computeOverscroll(startOffset, endOffset)
            if (overscroll != 0f) {
                scrollChannel.trySend(overscroll)
            }
        }
    }

    internal fun onDragInterrupted() {
        val shouldCommit = draggedIndex != null && hasMoved
        draggedIndex = null
        draggedDistance = 0f
        initialDraggedItemOffset = 0
        initialDraggedItemSize = 0
        hasMoved = false
        if (shouldCommit) {
            onDragEnd()
        }
    }

    private fun computeOverscroll(startOffset: Float, endOffset: Float): Float {
        val layoutInfo = listState.layoutInfo
        val margin = 96f
        val topThreshold = layoutInfo.viewportStartOffset + margin
        val bottomThreshold = layoutInfo.viewportEndOffset - margin
        return when {
            endOffset > bottomThreshold && listState.canScrollForward ->
                ((endOffset - bottomThreshold) * 0.25f).coerceIn(4f, 28f)
            startOffset < topThreshold && listState.canScrollBackward ->
                ((startOffset - topThreshold) * 0.25f).coerceIn(-28f, -4f)
            else -> 0f
        }
    }
}

fun Modifier.dragContainer(
    dragDropState: DragDropState,
    enabled: Boolean = true,
): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(dragDropState) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> dragDropState.onDragStart(offset) },
                onDrag = { change, offset ->
                    change.consume()
                    dragDropState.onDrag(offset)
                },
                onDragEnd = { dragDropState.onDragInterrupted() },
                onDragCancel = { dragDropState.onDragInterrupted() },
            )
        }
    }

@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit,
) {
    val isDragging = index == dragDropState.draggedIndex
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        label = "dragItemScale",
    )
    val itemModifier = if (isDragging) {
        modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = dragDropState.draggedItemOffset
                scaleX = scale
                scaleY = scale
                alpha = 0.96f
            }
    } else {
        modifier
            .zIndex(0f)
            .animateItem()
    }
    Box(modifier = itemModifier) {
        content(isDragging)
    }
}
