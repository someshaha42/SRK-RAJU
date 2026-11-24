package com.framecut.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.framecut.ui.components.icons.FrameCutIcons
import com.framecut.ui.core.NormalizedRegion
import com.framecut.ui.core.UiConstants
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.FrameCutShapes
import com.framecut.ui.theme.LocalStudioColors
import kotlin.math.roundToInt

@Composable
fun CropEditorCanvas(
    aspectRatio: Float,
    cropRect: NormalizedRegion,
    enabled: Boolean,
    onCropRectChange: (NormalizedRegion) -> Unit,
    modifier: Modifier = Modifier,
    previewContent: @Composable () -> Unit = {},
) {
    val studioColors = LocalStudioColors.current
    val density = LocalDensity.current
    val minSpan = UiConstants.MIN_CROP_SPAN_FRACTION
    var boxWidthPx by remember { mutableFloatStateOf(1f) }
    var boxHeightPx by remember { mutableFloatStateOf(1f) }
    val halfTouchPx = with(density) { FrameCutDimens.ResizeHandleTouchSize.toPx() / 2f }
    val currentCropRect by rememberUpdatedState(cropRect)
    val currentOnCropRectChange by rememberUpdatedState(onCropRectChange)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 380.dp)
            .clip(FrameCutShapes.TileShape)
            .background(Color(0xFF0A0C10)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(aspectRatio.coerceIn(0.35f, 2.5f))
                .fillMaxSize()
                .onSizeChanged {
                    if (it.width > 0 && it.height > 0) {
                        boxWidthPx = it.width.toFloat()
                        boxHeightPx = it.height.toFloat()
                    }
                },
        ) {
            previewContent()

            RegionBorderOverlayCanvas(
                leftFraction = cropRect.left,
                topFraction = cropRect.top,
                rightFraction = cropRect.right,
                bottomFraction = cropRect.bottom,
                animate = false,
                showBorder = true,
                scrimColor = studioColors.regionEditorScrim,
                modifier = Modifier.fillMaxSize(),
            )

            val lPx = cropRect.left * boxWidthPx
            val tPx = cropRect.top * boxHeightPx
            val rPx = cropRect.right * boxWidthPx
            val bPx = cropRect.bottom * boxHeightPx

            Box(
                modifier = Modifier
                    .offset { IntOffset(((lPx + rPx) / 2f - halfTouchPx).roundToInt(), ((tPx + bPx) / 2f - halfTouchPx).roundToInt()) }
                    .size(FrameCutDimens.ResizeHandleTouchSize)
                    .pointerInput(enabled, boxWidthPx, boxHeightPx) {
                        if (!enabled) return@pointerInput
                        detectDragGestures { change, drag ->
                            change.consume()
                            val rect = currentCropRect
                            val dx = drag.x / boxWidthPx
                            val dy = drag.y / boxHeightPx
                            val wf = rect.right - rect.left
                            val hf = rect.bottom - rect.top
                            val nl = (rect.left + dx).coerceIn(0f, 1f - wf)
                            val nt = (rect.top + dy).coerceIn(0f, 1f - hf)
                            currentOnCropRectChange(NormalizedRegion(nl, nt, nl + wf, nt + hf))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = CircleShape, color = studioColors.dragBadgeBackdrop, contentColor = Color.White) {
                    Icon(
                        imageVector = FrameCutIcons.RegionDrag,
                        contentDescription = "Move crop area",
                        modifier = Modifier.padding(6.dp).size(16.dp),
                    )
                }
            }

            CropHandleDot(
                xPx = lPx - halfTouchPx,
                yPx = tPx - halfTouchPx,
                enabled = enabled,
                onDrag = { dxPx, dyPx ->
                    val rect = currentCropRect
                    val nl = (rect.left + dxPx / boxWidthPx).coerceIn(0f, rect.right - minSpan)
                    val nt = (rect.top + dyPx / boxHeightPx).coerceIn(0f, rect.bottom - minSpan)
                    currentOnCropRectChange(rect.copy(left = nl, top = nt))
                },
            )
            CropHandleDot(
                xPx = rPx - halfTouchPx,
                yPx = tPx - halfTouchPx,
                enabled = enabled,
                onDrag = { dxPx, dyPx ->
                    val rect = currentCropRect
                    val nr = (rect.right + dxPx / boxWidthPx).coerceIn(rect.left + minSpan, 1f)
                    val nt = (rect.top + dyPx / boxHeightPx).coerceIn(0f, rect.bottom - minSpan)
                    currentOnCropRectChange(rect.copy(right = nr, top = nt))
                },
            )
            CropHandleDot(
                xPx = lPx - halfTouchPx,
                yPx = bPx - halfTouchPx,
                enabled = enabled,
                onDrag = { dxPx, dyPx ->
                    val rect = currentCropRect
                    val nl = (rect.left + dxPx / boxWidthPx).coerceIn(0f, rect.right - minSpan)
                    val nb = (rect.bottom + dyPx / boxHeightPx).coerceIn(rect.top + minSpan, 1f)
                    currentOnCropRectChange(rect.copy(left = nl, bottom = nb))
                },
            )
            CropHandleDot(
                xPx = rPx - halfTouchPx,
                yPx = bPx - halfTouchPx,
                enabled = enabled,
                onDrag = { dxPx, dyPx ->
                    val rect = currentCropRect
                    val nr = (rect.right + dxPx / boxWidthPx).coerceIn(rect.left + minSpan, 1f)
                    val nb = (rect.bottom + dyPx / boxHeightPx).coerceIn(rect.top + minSpan, 1f)
                    currentOnCropRectChange(rect.copy(right = nr, bottom = nb))
                },
            )
        }
    }
}

@Composable
private fun CropHandleDot(
    xPx: Float,
    yPx: Float,
    enabled: Boolean,
    onDrag: (Float, Float) -> Unit,
) {
    val studioColors = LocalStudioColors.current
    val currentOnDrag by rememberUpdatedState(onDrag)
    Box(
        modifier = Modifier
            .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
            .size(FrameCutDimens.ResizeHandleTouchSize)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, drag ->
                    change.consume()
                    currentOnDrag(drag.x, drag.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(FrameCutDimens.ResizeHandleDotSize)
                .clip(CircleShape)
                .background(studioColors.recordAccent)
                .border(FrameCutDimens.BorderHandle, Color.White, CircleShape),
        )
    }
}
