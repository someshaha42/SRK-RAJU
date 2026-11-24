package com.framecut.ui.core

object UiConstants {
    const val APP_NAME = "FrameCut"
    const val SETTINGS_SUBTITLE = "H.265/HEVC Hardware Surface Pipeline"
    const val CROP_TOOL_SUBTITLE = "Universal Photo & Video Crop"

    const val MIN_REGION_SPAN_FRACTION = 0.12f
    const val MIN_CROP_SPAN_FRACTION = 0.08f
    const val FULL_FRAME_TOLERANCE = 0.008f

    const val DEFAULT_CANVAS_PORTRAIT_WIDTH = 1080
    const val DEFAULT_CANVAS_PORTRAIT_HEIGHT = 1920
    const val DEFAULT_CANVAS_LANDSCAPE_WIDTH = 1920
    const val DEFAULT_CANVAS_LANDSCAPE_HEIGHT = 1080

    const val AUDIO_SAMPLE_RATE_DEFAULT_HZ = 48_000
    const val AUDIO_BITRATE_DEFAULT_BPS = 128_000

    object Routes {
        const val MAIN = "main"
        const val SETTINGS = "settings"
        const val RECORDINGS = "recordings"
        const val CROP = "crop"
        const val REGION_OVERLAY = "region_selector_overlay"
    }

    object TestTags {
        const val THEME_TOGGLE = "btn_theme_toggle"
        const val NAV_RECORDINGS = "btn_nav_recordings"
        const val NAV_SETTINGS = "btn_nav_settings"
        const val NAV_CROP = "btn_nav_crop"
        const val RECORDING_STATUS_BADGE = "recording_status_badge"
        const val RECORDING_TIMER = "recording_timer"
        const val BTN_RECORD = "btn_record"
        const val BTN_PAUSE_RESUME = "btn_pause_resume"
        const val BTN_STOP = "btn_stop"
        const val BTN_OPEN_RECORDINGS = "btn_open_recordings"
        const val SETTINGS_LOCKED_BANNER = "settings_locked_banner"
    }
}
