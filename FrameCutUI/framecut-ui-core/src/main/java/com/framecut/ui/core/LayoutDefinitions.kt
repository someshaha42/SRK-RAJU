package com.framecut.ui.core

import androidx.compose.runtime.Immutable

@Immutable
data class ScreenLayoutSpec(
    val id: String,
    val route: String,
    val compactMaxWidthDp: Int,
    val expandedBreakpointDp: Int,
    val expandedMaxWidthDp: Int,
    val leftPaneWeight: Float = 0.38f,
    val rightPaneWeight: Float = 0.62f,
)

object LayoutDefinitions {
    val MainScreenSpec = ScreenLayoutSpec(
        id = "screen_main",
        route = UiConstants.Routes.MAIN,
        compactMaxWidthDp = 820,
        expandedBreakpointDp = 840,
        expandedMaxWidthDp = 1280,
        leftPaneWeight = 0.38f,
        rightPaneWeight = 0.62f,
    )

    val SettingsScreenSpec = ScreenLayoutSpec(
        id = "screen_settings",
        route = UiConstants.Routes.SETTINGS,
        compactMaxWidthDp = 760,
        expandedBreakpointDp = 780,
        expandedMaxWidthDp = 1120,
    )

    val RecordingsScreenSpec = ScreenLayoutSpec(
        id = "screen_recordings",
        route = UiConstants.Routes.RECORDINGS,
        compactMaxWidthDp = 680,
        expandedBreakpointDp = 700,
        expandedMaxWidthDp = 1240,
    )

    val CropScreenSpec = ScreenLayoutSpec(
        id = "screen_crop",
        route = UiConstants.Routes.CROP,
        compactMaxWidthDp = 760,
        expandedBreakpointDp = 840,
        expandedMaxWidthDp = 960,
    )

    val RegionOverlaySpec = ScreenLayoutSpec(
        id = "overlay_region_selector",
        route = UiConstants.Routes.REGION_OVERLAY,
        compactMaxWidthDp = 440,
        expandedBreakpointDp = 720,
        expandedMaxWidthDp = 520,
    )
}
