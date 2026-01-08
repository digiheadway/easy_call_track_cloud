package com.miniclick.calltrackmanage.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun VerticalScrollbar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    itemCount: Int,
    minItems: Int = 100
) {
    if (itemCount < minItems) return

    val coroutineScope = rememberCoroutineScope()
    var containerHeight by remember { mutableFloatStateOf(0f) }
    
    // Track if user is currently dragging the scrollbar
    var isDragging by remember { mutableStateOf(false) }
    
    // Only show scrollbar when scrolling or dragging
    val isScrolling = lazyListState.isScrollInProgress
    val showScrollbar = isScrolling || isDragging
    
    val alpha by animateFloatAsState(
        targetValue = if (showScrollbar) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "scrollbar_alpha"
    )

    if (alpha == 0f && !showScrollbar) return

    // Calculate normalized scroll position (0f to 1f)
    val scrollOffset = remember {
        derivedStateOf {
            val firstVisibleItemIndex = lazyListState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = lazyListState.firstVisibleItemScrollOffset
            
            if (itemCount == 0) 0f
            else {
                val totalItems = itemCount.toFloat()
                val progress = firstVisibleItemIndex.toFloat() / totalItems
                val offsetInItem = if (totalItems > 0) (firstVisibleItemScrollOffset.toFloat() / 500f) / totalItems else 0f
                min(1f, progress + offsetInItem)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(20.dp)
            .alpha(alpha)
            .onGloballyPositioned { containerHeight = it.size.height.toFloat() }
    ) {
        val scrollbarHeight = 60.dp
        val maxScroll = constraints.maxHeight.toFloat() - 100f // Approximate handle height
        
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp)
                .offset(y = (scrollOffset.value * maxScroll).dp)
                .width(6.dp)
                .height(scrollbarHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                .pointerInput(itemCount) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val delta = dragAmount.y / maxScroll
                            val newScrollIndex = ((scrollOffset.value + delta) * itemCount).toInt()
                            coroutineScope.launch {
                                lazyListState.scrollToItem(
                                    max(0, min(itemCount - 1, newScrollIndex))
                                )
                            }
                        }
                    )
                }
        )
    }
}
