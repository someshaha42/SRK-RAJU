package com.framecut.ui.screens.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.framecut.ui.components.common.RegionBorderOverlayCanvas
import com.framecut.ui.components.icons.FrameCutIcons
import com.framecut.ui.core.AspectRatioPreset
import com.framecut.ui.core.NormalizedRegion
import com.framecut.ui.core.OrientationMode
import com.framecut.ui.core.RegionAnimationOption
import com.framecut.ui.core.RegionGeometry
import com.framecut.ui.core.UiConstants
import com.framecut.ui.definitions.UiInteractionDefinitions
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.FrameCutShapes
import com.framecut.ui.theme.LocalStudioColors
import kotlin.math.roundToInt

private enum class RegionDragHandle {
    MOVE,
    TOP_LEFT,
    TOP,
    TOP_RIGHT,
    RIGHT,
    BOTTOM_RIGHT,
    BOTTOM,
    BOTTOM_LEFT,
    LEFT,
}

@Composable
fun RegionSelectorOverlay(
    initialRegion: NormalizedRegion,
    orientationMode: OrientationMode,
    regionAnimation: RegionAnimationOption,
    onConfirm: (NormalizedRegion) -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)

    val studioColors = LocalStudioColors.current
    val density = LocalDensity.current
    val dragSpec = UiInteractionDefinitions.RegionDrag
    val minSpan = dragSpec.minSpanFraction

    var region by remember(initialRegion) { mutableStateOf(initialRegion.normalized(minSpan)) }
    var activePreset by remember { mutableStateOf(AspectRatioPreset.FREE) }

    val canvasWidthPx = if (orientationMode == OrientationMode.PORTRAIT) {
        UiConstants.DEFAULT_CANVAS_PORTRAIT_WIDTH
    } else {
        UiConstants.DEFAULT_CANVAS_LANDSCAPE_WIDTH
    }
    val canvasHeightPx = if (orientationMode == OrientationMode.PORTRAIT) {
        UiConstants.DEFAULT_CANVAS_PORTRAIT_HEIGHT
    } else {
        UiConstants.DEFAULT_CANVAS_LANDSCAPE_HEIGHT
    }
    val pixelRegion = remember(region, canvasWidthPx, canvasHeightPx) {
        RegionGeometry.toPixelRegion(region, canvasWidthPx, canvasHeightPx)
    }

    val dockHudTop = region.bottom > dragSpec.hudTopDockBottomThreshold &&
        region.top > dragSpec.hudTopDockTopThreshold

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(studioColors.regionOverlayBackdrop),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val containerH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

            RegionBorderOverlayCanvas(
                leftFraction = region.left,
                topFraction = region.top,
                rightFraction = region.right,
                bottomFraction = region.bottom,
                animate = regionAnimation == RegionAnimationOption.SHOW,
                showBorder = regionAnimation == RegionAnimationOption.SHOW,
                strokeWidth = FrameCutDimens.BorderRegionEditorStroke,
                scrimColor = studioColors.regionEditorScrim,
                neutralGuideColor = studioColors.regionNeutralGuide,
                modifier = Modifier.fillMaxSize(),
            )

            val rectLeftPx = region.left * containerW
            val rectTopPx = region.top * containerH
            val rectRightPx = region.right * containerW
            val rectBottomPx = region.bottom * containerH
            val rectWidthPx = (rectRightPx - rectLeftPx).coerceAtLeast(1f)
            val rectHeightPx = (rectBottomPx - rectTopPx).coerceAtLeast(1f)

            // Interior drag region
            Box(
                modifier = Modifier
                    .offset { IntOffset(rectLeftPx.roundToInt(), rectTopPx.roundToInt()) }
                    .size(
                        width = with(density) { rectWidthPx.toDp() },
                        height = with(density) { rectHeightPx.toDp() },
                    )
                    .pointerInput(containerW, containerH) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            activePreset = AspectRatioPreset.FREE
                            val dx = dragAmount.x / containerW
                            val dy = dragAmount.y / containerH
                            region = applyHandleDrag(
                                current = region,
                                handle = RegionDragHandle.MOVE,
                                dx = dx,
                                dy = dy,
                                minSpan = minSpan,
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = FrameCutShapes.TileShape,
                    color = studioColors.dragBadgeBackdrop,
                    border = BorderStroke(FrameCutDimens.BorderThin, studioColors.regionCanvasFrameBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FrameCutDimens.SpaceMd, vertical = FrameCutDimens.SpaceXs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space6),
                    ) {
                        Icon(
                            imageVector = FrameCutIcons.RegionDrag,
                            contentDescription = "Drag region",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${pixelRegion.width}×${pixelRegion.height}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                }
            }

            // 8 Resize Handles
            val handlePoints = listOf(
                RegionDragHandle.TOP_LEFT to Pair(rectLeftPx, rectTopPx),
                RegionDragHandle.TOP to Pair((rectLeftPx + rectRightPx) / 2f, rectTopPx),
                RegionDragHandle.TOP_RIGHT to Pair(rectRightPx, rectTopPx),
                RegionDragHandle.RIGHT to Pair(rectRightPx, (rectTopPx + rectBottomPx) / 2f),
                RegionDragHandle.BOTTOM_RIGHT to Pair(rectRightPx, rectBottomPx),
                RegionDragHandle.BOTTOM to Pair((rectLeftPx + rectRightPx) / 2f, rectBottomPx),
                RegionDragHandle.BOTTOM_LEFT to Pair(rectLeftPx, rectBottomPx),
                RegionDragHandle.LEFT to Pair(rectLeftPx, (rectTopPx + rectBottomPx) / 2f),
            )
            val touchHalfPx = with(density) { (FrameCutDimens.ResizeHandleTouchSize / 2).toPx() }

            handlePoints.forEach { (handle, centerPx) ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (centerPx.first - touchHalfPx).roundToInt(),
                                (centerPx.second - touchHalfPx).roundToInt(),
                            )
                        }
                        .size(FrameCutDimens.ResizeHandleTouchSize)
                        .pointerInput(handle, containerW, containerH) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                activePreset = AspectRatioPreset.FREE
                                val dx = dragAmount.x / containerW
                                val dy = dragAmount.y / containerH
                                region = applyHandleDrag(
                                    current = region,
                                    handle = handle,
                                    dx = dx,
                                    dy = dy,
                                    minSpan = minSpan,
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(FrameCutDimens.ResizeHandleDotSize)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(FrameCutDimens.BorderHandle, studioColors.recordAccent, CircleShape),
                    )
                }
            }

            // Floating HUD Card (docks top or bottom depending on region bounds)
            val hudModifier = if (dockHudTop) {
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = FrameCutDimens.SpaceXl, vertical = FrameCutDimens.SpaceLg)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = FrameCutDimens.SpaceXl, vertical = FrameCutDimens.SpaceLg)
            }

            Surface(
                modifier = hudModifier.widthIn(max = FrameCutDimens.RegionOverlayMaxWidth).fillMaxWidth(),
                shape = FrameCutShapes.SectionCardShape,
                color = MaterialTheme.colorScheme.surface.copy(
                    alpha = UiInteractionDefinitions.States.overlayHudSurfaceAlpha,
                ),
                border = BorderStroke(FrameCutDimens.BorderThin, MaterialTheme.colorScheme.outline),
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(FrameCutDimens.SpaceLg),
                    verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceMd),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recording Region",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${pixelRegion.width} × ${pixelRegion.height} px • (${pixelRegion.left}, ${pixelRegion.top})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = {
                                activePreset = AspectRatioPreset.FREE
                                region = NormalizedRegion()
                            },
                        ) {
                            Icon(imageVector = FrameCutIcons.RegionReset, contentDescription = "Reset region")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceSm),
                    ) {
                        AspectRatioPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = activePreset == preset,
                                onClick = {
                                    activePreset = preset
                                    preset.ratio?.let { ratio ->
                                        region = RegionGeometry.applyAspectRatio(
                                            current = region,
                                            targetRatio = ratio,
                                            screenW = canvasWidthPx.toFloat(),
                                            screenH = canvasHeightPx.toFloat(),
                                            widthCap = dragSpec.aspectFitWidthCap,
                                            heightCap = dragSpec.aspectFitHeightCap,
                                        )
                                    }
                                },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceMd),
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(FrameCutDimens.LibraryButtonHeight),
                            shape = FrameCutShapes.ActionButtonShape,
                        ) {
                            Icon(imageVector = FrameCutIcons.RegionCancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                            Text("Cancel")
                        }
                        Button(
                            onClick = { onConfirm(region.normalized(minSpan)) },
                            modifier = Modifier.weight(1f).height(FrameCutDimens.LibraryButtonHeight),
                            shape = FrameCutShapes.ActionButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = studioColors.recordAccent,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(imageVector = FrameCutIcons.RegionConfirm, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

private fun applyHandleDrag(
    current: NormalizedRegion,
    handle: RegionDragHandle,
    dx: Float,
    dy: Float,
    minSpan: Float,
): NormalizedRegion {
    if (handle == RegionDragHandle.MOVE) {
        val w = current.right - current.left
        val h = current.bottom - current.top
        val newLeft = (current.left + dx).coerceIn(0f, 1f - w)
        val newTop = (current.top + dy).coerceIn(0f, 1f - h)
        return NormalizedRegion(
            left = newLeft,
            top = newTop,
            right = newLeft + w,
            bottom = newTop + h,
        )
    }

    var l = current.left
    var t = current.top
    var r = current.right
    var b = current.bottom

    if (handle == RegionDragHandle.LEFT || handle == RegionDragHandle.TOP_LEFT || handle == RegionDragHandle.BOTTOM_LEFT) {
        l = (l + dx).coerceIn(0f, r - minSpan)
    }
    if (handle == RegionDragHandle.RIGHT || handle == RegionDragHandle.TOP_RIGHT || handle == RegionDragHandle.BOTTOM_RIGHT) {
        r = (r + dx).coerceIn(l + minSpan, 1f)
    }
    if (handle == RegionDragHandle.TOP || handle == RegionDragHandle.TOP_LEFT || handle == RegionDragHandle.TOP_RIGHT) {
        t = (t + dy).coerceIn(0f, b - minSpan)
    }
    if (handle == RegionDragHandle.BOTTOM || handle == RegionDragHandle.BOTTOM_LEFT || handle == RegionDragHandle.BOTTOM_RIGHT) {
        b = (b + dy).coerceIn(t + minSpan, 1f)
    }

    return NormalizedRegion(l, t, r, b).normalized(minSpan)
}
