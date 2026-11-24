package com.framecut.ui.core

import androidx.compose.runtime.Immutable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class RecordingMode {
    FULL_SCREEN,
    REGION,
}

enum class OrientationMode {
    PORTRAIT,
    LANDSCAPE,
}

enum class AudioSourceOption {
    INTERNAL,
    MICROPHONE,
}

enum class VideoBitrateOption(val mbps: Int, val label: String) {
    MBPS_100(100, "100 Mbps"),
    MBPS_150(150, "150 Mbps"),
    MBPS_200(200, "200 Mbps"),
    MBPS_250(250, "250 Mbps"),
    MBPS_300(300, "300 Mbps"),
    MBPS_350(350, "350 Mbps"),
}

enum class RegionAnimationOption {
    SHOW,
    HIDE,
}

enum class BitrateModeOption(val label: String, val subtitle: String) {
    VBR("VBR", "Balanced adaptive quality"),
    CQ("CQ", "Constant target quality"),
}

enum class ColorDepthOption(val bitDepth: Int, val label: String, val subtitle: String) {
    BIT_8(8, "8-Bit", "Standard smooth playback"),
    BIT_10(10, "10-Bit", "Richer gradient depth"),
}

enum class VideoFpsOption(val fps: Int, val label: String, val subtitle: String) {
    FPS_30(30, "30 FPS", "Saves battery & heat"),
    FPS_60(60, "60 FPS", "Fluid motion capture"),
}

enum class GopIntervalOption(val secondsFloat: Float, val label: String, val subtitle: String) {
    SEC_1(1.0f, "1 Second", "Smaller file size"),
    SEC_0_5(0.5f, "0.5 Second", "Faster video scrubbing"),
}

enum class ColorRangeOption(val label: String, val subtitle: String) {
    LIMITED("Limited (16–235)", "Standard safe compatibility"),
    FULL("Full (0–255)", "Maximum tonal contrast"),
}

enum class AudioFormatOption(val label: String, val subtitle: String) {
    MONO_AAC("Mono AAC", "48 kHz · 128 kbps"),
    MONO_MP3("Mono MP3", "48 kHz · 128 kbps"),
}

enum class RecordingPhase {
    IDLE,
    RECORDING,
    PAUSED,
    FINALIZING,
}

enum class AspectRatioPreset(val id: String, val label: String, val ratio: Float?) {
    FREE("free", "Free", null),
    R16_9("16_9", "16:9", 16f / 9f),
    R9_16("9_16", "9:16", 9f / 16f),
    R4_3("4_3", "4:3", 4f / 3f),
    R1_1("1_1", "1:1", 1f),
}

enum class CropMediaCategory(val label: String) {
    ALL("ALL"),
    PHOTO("PHOTO"),
    VIDEO("VIDEO"),
}

enum class CropMediaKind {
    PHOTO,
    VIDEO,
}

enum class UiColorRole {
    BACKGROUND,
    SURFACE,
    SURFACE_VARIANT,
    OUTLINE,
    OUTLINE_VARIANT,
    ON_SURFACE,
    ON_SURFACE_VARIANT,
    PRIMARY,
    ON_PRIMARY,
    PRIMARY_CONTAINER,
    ON_PRIMARY_CONTAINER,
    SECONDARY,
    ON_SECONDARY,
    SECONDARY_CONTAINER,
    ON_SECONDARY_CONTAINER,
    TERTIARY,
    ERROR,
    ERROR_CONTAINER,
    RECORD_ACCENT,
    RECORD_DEEP,
    RECORD_CONTAINER,
    PAUSED_ACCENT,
    PAUSED_CONTAINER,
    READY_ACCENT,
    READY_CONTAINER,
}

@Immutable
data class NormalizedRegion(
    val left: Float = 0.1f,
    val top: Float = 0.2f,
    val right: Float = 0.9f,
    val bottom: Float = 0.7f,
) {
    fun normalized(minSpan: Float = UiConstants.MIN_REGION_SPAN_FRACTION): NormalizedRegion {
        val clampedLeft = left.coerceIn(0f, 1f - minSpan)
        val clampedTop = top.coerceIn(0f, 1f - minSpan)
        val clampedRight = right.coerceIn(clampedLeft + minSpan, 1f)
        val clampedBottom = bottom.coerceIn(clampedTop + minSpan, 1f)
        return NormalizedRegion(clampedLeft, clampedTop, clampedRight, clampedBottom)
    }

    fun isFullFrame(tolerance: Float = UiConstants.FULL_FRAME_TOLERANCE): Boolean =
        left <= tolerance && top <= tolerance && right >= (1f - tolerance) && bottom >= (1f - tolerance)
}

@Immutable
data class PixelRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val width: Int,
    val height: Int,
)

@Immutable
data class RecordedVideoItem(
    val id: Long,
    val displayName: String,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val uriString: String = "",
)

@Immutable
data class RecordingSettings(
    val recordingMode: RecordingMode = RecordingMode.FULL_SCREEN,
    val orientationMode: OrientationMode = OrientationMode.PORTRAIT,
    val audioSource: AudioSourceOption = AudioSourceOption.INTERNAL,
    val videoBitrate: VideoBitrateOption = VideoBitrateOption.MBPS_150,
    val regionAnimation: RegionAnimationOption = RegionAnimationOption.SHOW,
    val selectedRegion: NormalizedRegion = NormalizedRegion(),
    val bitrateMode: BitrateModeOption = BitrateModeOption.VBR,
    val colorDepth: ColorDepthOption = ColorDepthOption.BIT_8,
    val videoFps: VideoFpsOption = VideoFpsOption.FPS_30,
    val gopInterval: GopIntervalOption = GopIntervalOption.SEC_1,
    val colorRange: ColorRangeOption = ColorRangeOption.LIMITED,
    val audioFormat: AudioFormatOption = AudioFormatOption.MONO_AAC,
    val darkThemeOverride: Boolean? = null,
    val customVideoBitrateMbps: Long? = null,
    val customVideoFps: Int? = null,
    val customGopSeconds: Float? = null,
    val customAudioSampleRateHz: Int? = null,
    val customAudioBitrateBps: Int? = null,
)

object RegionGeometry {
    fun toPixelRegion(
        region: NormalizedRegion,
        canvasWidthPx: Int,
        canvasHeightPx: Int,
        evenAlign: Boolean = true,
    ): PixelRegion {
        val norm = region.normalized()
        val left = (norm.left * canvasWidthPx).roundToInt().coerceIn(0, max(0, canvasWidthPx - 2))
        val top = (norm.top * canvasHeightPx).roundToInt().coerceIn(0, max(0, canvasHeightPx - 2))
        val rawW = ((norm.right - norm.left) * canvasWidthPx).roundToInt().coerceAtLeast(16)
        val rawH = ((norm.bottom - norm.top) * canvasHeightPx).roundToInt().coerceAtLeast(16)
        val alignedW = if (evenAlign) max(2, (rawW / 2) * 2) else rawW
        val alignedH = if (evenAlign) max(2, (rawH / 2) * 2) else rawH
        val width = min(canvasWidthPx - left, alignedW).coerceAtLeast(2)
        val height = min(canvasHeightPx - top, alignedH).coerceAtLeast(2)
        return PixelRegion(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
            width = width,
            height = height,
        )
    }

    fun applyAspectRatio(
        current: NormalizedRegion,
        targetRatio: Float,
        screenW: Float,
        screenH: Float,
        widthCap: Float = 0.90f,
        heightCap: Float = 0.78f,
    ): NormalizedRegion {
        val centerX = (current.left + current.right) / 2f
        val centerY = (current.top + current.bottom) / 2f
        var targetW = (current.right - current.left) * screenW
        var targetH = targetW / targetRatio
        if (targetH > screenH * heightCap) {
            targetH = screenH * heightCap
            targetW = targetH * targetRatio
        }
        if (targetW > screenW * widthCap) {
            targetW = screenW * widthCap
            targetH = targetW / targetRatio
        }
        val halfFracW = (targetW / screenW) / 2f
        val halfFracH = (targetH / screenH) / 2f
        val clampedCx = centerX.coerceIn(halfFracW, 1f - halfFracW)
        val clampedCy = centerY.coerceIn(halfFracH, 1f - halfFracH)
        return NormalizedRegion(
            left = clampedCx - halfFracW,
            top = clampedCy - halfFracH,
            right = clampedCx + halfFracW,
            bottom = clampedCy + halfFracH,
        ).normalized()
    }
}
