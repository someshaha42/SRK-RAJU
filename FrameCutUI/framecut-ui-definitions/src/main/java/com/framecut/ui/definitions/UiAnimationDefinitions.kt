package com.framecut.ui.definitions

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode

object UiAnimationDefinitions {
    val StatusBadgeColor = UiColorTweenSpec(
        durationMillis = 220,
        label = "status_badge_color",
    )

    val StatusBadgeContainer = UiColorTweenSpec(
        durationMillis = 220,
        label = "status_badge_container",
    )

    val RecordingDotPulse = UiPulseSpec(
        transitionLabel = "recording_dot_pulse",
        valueLabel = "recording_dot_alpha",
        initialValue = 1f,
        targetValue = 0.25f,
        durationMillis = 750,
        easing = FastOutSlowInEasing,
        repeatMode = RepeatMode.Reverse,
    )

    val FeedbackBannerVisibility = UiVisibilityTransitionSpec(
        label = "feedback_banner_visibility",
    )

    val SavedBannerVisibility = UiVisibilityTransitionSpec(
        label = "saved_video_banner_visibility",
    )

    val RegionPreviewVisibility = UiVisibilityTransitionSpec(
        label = "region_preview_visibility",
    )

    val RegionBorderCanvasDash = UiDashedBorderAnimSpec(
        transitionLabel = "region_border_dash",
        valueLabel = "region_border_phase_step",
        durationMillis = 900,
        phaseSteps = 12,
        easing = LinearEasing,
    )
}
