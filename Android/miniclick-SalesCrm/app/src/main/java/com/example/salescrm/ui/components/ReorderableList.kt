package com.example.salescrm.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A simplified reorderable list state for LazyColumn
 */
@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (Int, Int) -> Unit
): ReorderableLazyListState {
    val coroutineScope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    return remember(lazyListState, haptic) {
        ReorderableLazyListState(lazyListState, onMove, coroutineScope, haptic)
    }
}

class ReorderableLazyListState(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit,
    private val scope: CoroutineScope,
    private val haptic: HapticFeedback
) {
    private var draggedItemKey by mutableStateOf<Any?>(null)
    private var dragOffset by mutableStateOf(0f)
    
    val isDragging get() = draggedItemKey != null

    fun onDragStart(key: Any) {
        draggedItemKey = key
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun onDragInterrupted() {
        draggedItemKey = null
        dragOffset = 0f
    }

    fun onDrag(delta: Offset) {
        dragOffset += delta.y
        
        val draggedItem = draggedItemKey?.let { key ->
            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        } ?: return

        val startOffset = draggedItem.offset + dragOffset
        val middleOffset = startOffset + draggedItem.size / 2f

        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            middleOffset.toInt() in item.offset..(item.offset + item.size) &&
                    draggedItem.index != item.index
        }

        if (targetItem != null) {
            val oldIndex = draggedItem.index
            val newIndex = targetItem.index
            onMove(oldIndex, newIndex)
            dragOffset -= targetItem.offset - draggedItem.offset
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun Modifier.reorderableItem(key: Any): Modifier = graphicsLayer {
        val isDragged = key == draggedItemKey
        translationY = if (isDragged) dragOffset else 0f
        alpha = if (isDragged) 0.8f else 1f
        scaleX = if (isDragged) 1.05f else 1f
        scaleY = if (isDragged) 1.05f else 1f
    }.zIndex(if (key == draggedItemKey) 1f else 0f)

    fun Modifier.dragHandle(key: Any): Modifier = pointerInput(key) {
        detectDragGesturesAfterLongPress(
            onDragStart = { _ -> onDragStart(key) },
            onDragEnd = { onDragInterrupted() },
            onDragCancel = { onDragInterrupted() },
            onDrag = { change, dragAmount ->
                change.consume()
                onDrag(dragAmount)
            }
        )
    }
}
