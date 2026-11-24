package com.framecut.ui.core

import androidx.compose.runtime.Immutable

@Immutable
data class ComponentDescriptor(
    val id: String,
    val parentId: String?,
    val positionIndex: Int,
    val testTag: String,
    val title: String? = null,
    val subtitle: String? = null,
)

object ComponentRegistry {
    object Screens {
        val Main = ComponentDescriptor("screen_main", null, 0, "screen_main", "FrameCut")
        val Settings = ComponentDescriptor("screen_settings", null, 1, "screen_settings", "Encoding Settings")
        val Recordings = ComponentDescriptor("screen_recordings", null, 2, "screen_recordings", "Recorded Videos")
        val CropTool = ComponentDescriptor("screen_crop", null, 3, "screen_crop", "Media Crop Tool")
        val RegionOverlay = ComponentDescriptor("overlay_region_selector", null, 4, "overlay_region_selector")
    }

    object Panels {
        val FeedbackBanner = ComponentDescriptor("panel_feedback_banner", "screen_main", 1, "panel_feedback_banner")
        val RecordingConsole = ComponentDescriptor("panel_recording_console", "screen_main", 2, "panel_recording_console")
        val SavedVideoBanner = ComponentDescriptor("panel_saved_video_banner", "screen_main", 3, "last_saved_banner")
        val RecordingMode = ComponentDescriptor("panel_recording_mode", "panel_settings_grid", 0, "panel_recording_mode", "Recording Mode")
        val RegionPreview = ComponentDescriptor("panel_region_preview", "panel_settings_grid", 1, "panel_region_preview", "Selected Region")
        val OrientationModePanel = ComponentDescriptor("panel_orientation_mode", "panel_settings_grid", 2, "panel_orientation_mode", "Orientation")
        val AudioSourcePanel = ComponentDescriptor("panel_audio_source", "panel_settings_grid", 3, "panel_audio_source", "Audio Source")
        val VideoBitratePanel = ComponentDescriptor("panel_video_bitrate", "panel_settings_grid", 4, "panel_video_bitrate", "Video Bitrate")
        val RegionAnimationPanel = ComponentDescriptor("panel_region_animation", "panel_settings_grid", 5, "panel_region_animation", "Region Border Animation")
        val BitrateMode = ComponentDescriptor("panel_bitrate_mode", "screen_settings", 2, "panel_bitrate_mode", "Bitrate Mode", "Controls bitrate allocation behavior")
        val ColorDepth = ComponentDescriptor("panel_color_depth", "screen_settings", 3, "panel_color_depth", "Color Depth", "Selects hardware HEVC bit precision")
        val VideoFps = ComponentDescriptor("panel_video_fps", "screen_settings", 4, "panel_video_fps", "Frame Rate (FPS)", "Sets capture smoothness per second")
        val ColorStandard = ComponentDescriptor("panel_color_standard", "screen_settings", 5, "panel_color_standard", "Color Standard", "Fixed hardware display color gamut")
        val OutputContainer = ComponentDescriptor("panel_output_container", "screen_settings", 6, "panel_output_container", "Output Container", "Active MP4 muxing container format")
        val RenderingMode = ComponentDescriptor("panel_rendering_mode", "screen_settings", 7, "panel_rendering_mode", "Rendering Pipeline", "Fixed zero-copy surface callback pipeline")
        val GopInterval = ComponentDescriptor("panel_gop_interval", "screen_settings", 8, "panel_gop_interval", "Keyframe Interval (GOP)", "Controls seeking & edit precision")
        val ColorRange = ComponentDescriptor("panel_color_range", "screen_settings", 9, "panel_color_range", "Color Range", "Sets luminance & contrast span")
        val AudioFormat = ComponentDescriptor("panel_audio_format", "screen_settings", 10, "panel_audio_format", "Audio Format", "Selects mono 48 kHz audio encoder")
        val ChromaSubsampling = ComponentDescriptor("panel_chroma_subsampling", "screen_settings", 11, "panel_chroma_subsampling", "Chroma Subsampling", "Active hardware surface color sampling")
        val RegionHud = ComponentDescriptor("panel_region_hud", "overlay_region_selector", 10, "panel_region_hud")
    }

    object Buttons {
        val ThemeToggle = ComponentDescriptor("btn_theme_toggle", "toolbar_main", 1, UiConstants.TestTags.THEME_TOGGLE)
        val NavCrop = ComponentDescriptor("btn_nav_crop", "toolbar_main", 2, UiConstants.TestTags.NAV_CROP)
        val NavRecordings = ComponentDescriptor("btn_nav_recordings", "toolbar_main", 3, UiConstants.TestTags.NAV_RECORDINGS)
        val NavSettings = ComponentDescriptor("btn_nav_settings", "toolbar_main", 4, UiConstants.TestTags.NAV_SETTINGS)
        val Record = ComponentDescriptor("btn_record", "panel_recording_console", 2, UiConstants.TestTags.BTN_RECORD, "Record")
        val PauseResume = ComponentDescriptor("btn_pause_resume", "panel_recording_console", 3, UiConstants.TestTags.BTN_PAUSE_RESUME)
        val Stop = ComponentDescriptor("btn_stop", "panel_recording_console", 4, UiConstants.TestTags.BTN_STOP, "Stop")
        val OpenRecordings = ComponentDescriptor("btn_open_recordings", "screen_main", 5, UiConstants.TestTags.BTN_OPEN_RECORDINGS)
        val AdjustRegion = ComponentDescriptor("btn_adjust_region", "panel_region_preview", 1, "btn_adjust_region", "Adjust Region")
        val ConfirmRegion = ComponentDescriptor("btn_confirm_region", "panel_region_hud", 7, "btn_confirm_region", "Confirm Region")
    }

    val allDescriptors: List<ComponentDescriptor> = listOf(
        Screens.Main,
        Screens.Settings,
        Screens.Recordings,
        Screens.CropTool,
        Screens.RegionOverlay,
        Panels.FeedbackBanner,
        Panels.RecordingConsole,
        Panels.SavedVideoBanner,
        Panels.RecordingMode,
        Panels.RegionPreview,
        Panels.OrientationModePanel,
        Panels.AudioSourcePanel,
        Panels.VideoBitratePanel,
        Panels.RegionAnimationPanel,
        Panels.BitrateMode,
        Panels.ColorDepth,
        Panels.VideoFps,
        Panels.ColorStandard,
        Panels.OutputContainer,
        Panels.RenderingMode,
        Panels.GopInterval,
        Panels.ColorRange,
        Panels.AudioFormat,
        Panels.ChromaSubsampling,
        Panels.RegionHud,
        Buttons.ThemeToggle,
        Buttons.NavCrop,
        Buttons.NavRecordings,
        Buttons.NavSettings,
        Buttons.Record,
        Buttons.PauseResume,
        Buttons.Stop,
        Buttons.OpenRecordings,
        Buttons.AdjustRegion,
        Buttons.ConfirmRegion,
    )
}
