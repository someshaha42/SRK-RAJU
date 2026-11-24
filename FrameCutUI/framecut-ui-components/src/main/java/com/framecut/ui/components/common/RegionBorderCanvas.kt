package com.framecut.ui.components.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.framecut.ui.definitions.UiAnimationDefinitions
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.LocalStudioColors

@Composable
fun RegionBorderOverlayCanvas(
    leftFraction: Float,
    topFraction: Float,
    rightFraction: Float,
    bottomFraction: Float,
    animate: Boolean,
    showBorder: Boolean,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = FrameCutDimens.BorderFocus,
    scrimColor: Color = Color.Transparent,
    neutralGuideColor: Color = Color.Transparent,
) {
    val studioColors = LocalStudioColors.current
    val density = LocalDensity.current
    val dashPx = with(density) { FrameCutDimens.RegionDashLength.toPx() }
    val gapPx = with(density) { FrameCutDimens.RegionDashGap.toPx() }
    val strokePx = with(density) { strokeWidth.toPx() }
    val neutralStrokePx = with(density) { FrameCutDimens.BorderRegionNeutralStroke.toPx() }

    val animSpec = UiAnimationDefinitions.RegionBorderCanvasDash
    val shadowColor = remember(animSpec.shadowAlpha) { Color.Black.copy(alpha = animSpec.shadowAlpha) }
    val shadowStroke = remember(strokePx, animSpec.shadowExtraStrokePx) {
        Stroke(width = strokePx + animSpec.shadowExtraStrokePx)
    }
    val neutralStroke = remember(neutralStrokePx) { Stroke(width = neutralStrokePx) }
    val animatedStrokes = remember(showBorder, dashPx, gapPx, strokePx, animSpec.phaseSteps) {
        if (!showBorder) {
            emptyArray()
        } else {
            val intervals = floatArrayOf(dashPx, gapPx)
            val period = dashPx + gapPx
            Array(animSpec.phaseSteps) { step ->
                val stepPhase = -(step.toFloat() / animSpec.phaseSteps) * period
                Stroke(width = strokePx, pathEffect = PathEffect.dashPathEffect(intervals, stepPhase))
            }
        }
    }

    val stepState: State<Float>? = if (showBorder && animate) {
        val transition = rememberInfiniteTransition(label = animSpec.transitionLabel)
        transition.animateFloat(
            initialValue = 0f,
            targetValue = animSpec.phaseSteps.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = animSpec.durationMillis, easing = animSpec.easing),
                repeatMode = RepeatMode.Restart,
            ),
            label = animSpec.valueLabel,
        )
    } else {
        null
    }

    val stepIndexState: State<Int>? = remember(stepState, animSpec.phaseSteps) {
        stepState?.let { animState ->
            derivedStateOf { animState.value.toInt().mod(animSpec.phaseSteps) }
        }
    }

    Canvas(modifier = modifier) {
        val l = (leftFraction * size.width).coerceIn(0f, size.width)
        val t = (topFraction * size.height).coerceIn(0f, size.height)
        val r = (rightFraction * size.width).coerceIn(l, size.width)
        val b = (bottomFraction * size.height).coerceIn(t, size.height)

        if (scrimColor != Color.Transparent) {
            drawRect(color = scrimColor, topLeft = Offset.Zero, size = Size(size.width, t))
            drawRect(color = scrimColor, topLeft = Offset(0f, b), size = Size(size.width, (size.height - b).coerceAtLeast(0f)))
            drawRect(color = scrimColor, topLeft = Offset(0f, t), size = Size(l, (b - t).coerceAtLeast(0f)))
            drawRect(color = scrimColor, topLeft = Offset(r, t), size = Size((size.width - r).coerceAtLeast(0f), (b - t).coerceAtLeast(0f)))
        }

        val rectTopLeft = Offset(l, t)
        val rectSize = Size((r - l).coerceAtLeast(1f), (b - t).coerceAtLeast(1f))

        if (showBorder && animatedStrokes.isNotEmpty()) {
            val stepIndex = stepIndexState?.value ?: 0
            drawRect(color = shadowColor, topLeft = rectTopLeft, size = rectSize, style = shadowStroke)
            drawRect(color = studioColors.recordAccent, topLeft = rectTopLeft, size = rectSize, style = animatedStrokes[stepIndex])
        } else if (neutralGuideColor != Color.Transparent) {
            drawRect(color = neutralGuideColor, topLeft = rectTopLeft, size = rectSize, style = neutralStroke)
        }
    }
}
