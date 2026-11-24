package com.framecut.ui.definitions

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Immutable

@Immutable
data class UiColorTweenSpec(
    val durationMillis: Int = 220,
    val label: String,
)

@Immutable
data class UiPulseSpec(
    val transitionLabel: String,
    val valueLabel: String,
    val initialValue: Float,
    val targetValue: Float,
    val durationMillis: Int,
    val easing: Easing = FastOutSlowInEasing,
    val repeatMode: RepeatMode = RepeatMode.Reverse,
)

@Immutable
data class UiVisibilityTransitionSpec(
    val label: String,
    val enterDurationMillis: Int = 180,
    val exitDurationMillis: Int = 160,
) {
    fun enterTransition(): EnterTransition =
        fadeIn(tween(enterDurationMillis)) + expandVertically(tween(enterDurationMillis))

    fun exitTransition(): ExitTransition =
        fadeOut(tween(exitDurationMillis)) + shrinkVertically(tween(exitDurationMillis))
}

@Immutable
data class UiDashedBorderAnimSpec(
    val transitionLabel: String,
    val valueLabel: String,
    val durationMillis: Int = 900,
    val phaseSteps: Int = 12,
    val easing: Easing = LinearEasing,
    val shadowAlpha: Float = 0.45f,
    val shadowExtraStrokePx: Float = 2f,
)

@Immutable
data class UiPathEntry(
    val canonicalPath: String,
    val moduleName: String,
    val assetPath: String,
)
