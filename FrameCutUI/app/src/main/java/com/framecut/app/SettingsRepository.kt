package com.framecut.app

import android.content.Context
import com.framecut.ui.core.AudioFormatOption
import com.framecut.ui.core.AudioSourceOption
import com.framecut.ui.core.BitrateModeOption
import com.framecut.ui.core.ColorDepthOption
import com.framecut.ui.core.ColorRangeOption
import com.framecut.ui.core.GopIntervalOption
import com.framecut.ui.core.NormalizedRegion
import com.framecut.ui.core.OrientationMode
import com.framecut.ui.core.RecordingMode
import com.framecut.ui.core.RecordingSettings
import com.framecut.ui.core.RegionAnimationOption
import com.framecut.ui.core.VideoBitrateOption
import com.framecut.ui.core.VideoFpsOption

class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): RecordingSettings {
        val defaults = RecordingSettings()
        val darkPref = prefs.getInt(KEY_DARK_OVERRIDE, -1)
        val darkOverride = when (darkPref) {
            1 -> true
            0 -> false
            else -> null
        }

        val region = NormalizedRegion(
            left = prefs.getFloat(KEY_REGION_LEFT, defaults.selectedRegion.left),
            top = prefs.getFloat(KEY_REGION_TOP, defaults.selectedRegion.top),
            right = prefs.getFloat(KEY_REGION_RIGHT, defaults.selectedRegion.right),
            bottom = prefs.getFloat(KEY_REGION_BOTTOM, defaults.selectedRegion.bottom),
        ).normalized()

        return RecordingSettings(
            recordingMode = enumOrDefault(prefs.getString(KEY_RECORDING_MODE, null), defaults.recordingMode),
            orientationMode = enumOrDefault(prefs.getString(KEY_ORIENTATION_MODE, null), defaults.orientationMode),
            audioSource = enumOrDefault(prefs.getString(KEY_AUDIO_SOURCE, null), defaults.audioSource),
            videoBitrate = enumOrDefault(prefs.getString(KEY_VIDEO_BITRATE, null), defaults.videoBitrate),
            regionAnimation = enumOrDefault(prefs.getString(KEY_REGION_ANIM, null), defaults.regionAnimation),
            selectedRegion = region,
            bitrateMode = enumOrDefault(prefs.getString(KEY_BITRATE_MODE, null), defaults.bitrateMode),
            colorDepth = enumOrDefault(prefs.getString(KEY_COLOR_DEPTH, null), defaults.colorDepth),
            videoFps = enumOrDefault(prefs.getString(KEY_VIDEO_FPS, null), defaults.videoFps),
            gopInterval = enumOrDefault(prefs.getString(KEY_GOP_INTERVAL, null), defaults.gopInterval),
            colorRange = enumOrDefault(prefs.getString(KEY_COLOR_RANGE, null), defaults.colorRange),
            audioFormat = enumOrDefault(prefs.getString(KEY_AUDIO_FORMAT, null), defaults.audioFormat),
            darkThemeOverride = darkOverride,
            customVideoBitrateMbps = prefs.getLong(KEY_CUSTOM_BITRATE_MBPS, -1L).takeIf { it > 0L },
            customVideoFps = prefs.getInt(KEY_CUSTOM_FPS, -1).takeIf { it > 0 },
            customGopSeconds = prefs.getFloat(KEY_CUSTOM_GOP, -1f).takeIf { it > 0f },
            customAudioSampleRateHz = prefs.getInt(KEY_CUSTOM_AUDIO_HZ, -1).takeIf { it > 0 },
            customAudioBitrateBps = prefs.getInt(KEY_CUSTOM_AUDIO_BPS, -1).takeIf { it > 0 },
        )
    }

    fun save(settings: RecordingSettings) {
        prefs.edit()
            .putString(KEY_RECORDING_MODE, settings.recordingMode.name)
            .putString(KEY_ORIENTATION_MODE, settings.orientationMode.name)
            .putString(KEY_AUDIO_SOURCE, settings.audioSource.name)
            .putString(KEY_VIDEO_BITRATE, settings.videoBitrate.name)
            .putString(KEY_REGION_ANIM, settings.regionAnimation.name)
            .putFloat(KEY_REGION_LEFT, settings.selectedRegion.left)
            .putFloat(KEY_REGION_TOP, settings.selectedRegion.top)
            .putFloat(KEY_REGION_RIGHT, settings.selectedRegion.right)
            .putFloat(KEY_REGION_BOTTOM, settings.selectedRegion.bottom)
            .putString(KEY_BITRATE_MODE, settings.bitrateMode.name)
            .putString(KEY_COLOR_DEPTH, settings.colorDepth.name)
            .putString(KEY_VIDEO_FPS, settings.videoFps.name)
            .putString(KEY_GOP_INTERVAL, settings.gopInterval.name)
            .putString(KEY_COLOR_RANGE, settings.colorRange.name)
            .putString(KEY_AUDIO_FORMAT, settings.audioFormat.name)
            .putInt(
                KEY_DARK_OVERRIDE,
                when (settings.darkThemeOverride) {
                    true -> 1
                    false -> 0
                    null -> -1
                },
            )
            .putLong(KEY_CUSTOM_BITRATE_MBPS, settings.customVideoBitrateMbps ?: -1L)
            .putInt(KEY_CUSTOM_FPS, settings.customVideoFps ?: -1)
            .putFloat(KEY_CUSTOM_GOP, settings.customGopSeconds ?: -1f)
            .putInt(KEY_CUSTOM_AUDIO_HZ, settings.customAudioSampleRateHz ?: -1)
            .putInt(KEY_CUSTOM_AUDIO_BPS, settings.customAudioBitrateBps ?: -1)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }

    companion object {
        private const val PREFS_NAME = "framecut_settings"
        private const val KEY_RECORDING_MODE = "recording_mode"
        private const val KEY_ORIENTATION_MODE = "orientation_mode"
        private const val KEY_AUDIO_SOURCE = "audio_source"
        private const val KEY_VIDEO_BITRATE = "video_bitrate"
        private const val KEY_REGION_ANIM = "region_anim"
        private const val KEY_REGION_LEFT = "region_left"
        private const val KEY_REGION_TOP = "region_top"
        private const val KEY_REGION_RIGHT = "region_right"
        private const val KEY_REGION_BOTTOM = "region_bottom"
        private const val KEY_BITRATE_MODE = "bitrate_mode"
        private const val KEY_COLOR_DEPTH = "color_depth"
        private const val KEY_VIDEO_FPS = "video_fps"
        private const val KEY_GOP_INTERVAL = "gop_interval"
        private const val KEY_COLOR_RANGE = "color_range"
        private const val KEY_AUDIO_FORMAT = "audio_format"
        private const val KEY_DARK_OVERRIDE = "dark_override"
        private const val KEY_CUSTOM_BITRATE_MBPS = "custom_bitrate_mbps"
        private const val KEY_CUSTOM_FPS = "custom_fps"
        private const val KEY_CUSTOM_GOP = "custom_gop"
        private const val KEY_CUSTOM_AUDIO_HZ = "custom_audio_hz"
        private const val KEY_CUSTOM_AUDIO_BPS = "custom_audio_bps"
    }
}
