package com.framecut.ui.definitions

import androidx.compose.runtime.Immutable

@Immutable
data class UiStateAlphas(
    val idleContentAlpha: Float = 1f,
    val lockedContentAlpha: Float = 0.52f,
    val subRowContainerAlpha: Float = 0.6f,
    val cardHoveredAlpha: Float = 0.55f,
    val overlayHudSurfaceAlpha: Float = 0.94f,
    val statusHeaderGradientAlpha: Float = 0.35f,
)

@Immutable
data class UiRegionDragSpec(
    val minSpanFraction: Float = 0.12f,
    val aspectFitWidthCap: Float = 0.90f,
    val aspectFitHeightCap: Float = 0.78f,
    val hudTopDockBottomThreshold: Float = 0.72f,
    val hudTopDockTopThreshold: Float = 0.25f,
)

object UiInteractionDefinitions {
    val States = UiStateAlphas()
    val RegionDrag = UiRegionDragSpec()
}
