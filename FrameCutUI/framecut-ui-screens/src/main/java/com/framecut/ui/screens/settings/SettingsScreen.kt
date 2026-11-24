package com.framecut.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.framecut.ui.components.cards.DefinedOptionTile
import com.framecut.ui.components.cards.DefinedSectionCard
import com.framecut.ui.components.icons.FrameCutIcons
import com.framecut.ui.components.inputs.CustomNumericInputBox
import com.framecut.ui.components.panels.SettingsLockedBannerPanel
import com.framecut.ui.components.toolbars.FrameCutSubScreenTopBar
import com.framecut.ui.core.AudioFormatOption
import com.framecut.ui.core.BitrateModeOption
import com.framecut.ui.core.ColorDepthOption
import com.framecut.ui.core.ColorRangeOption
import com.framecut.ui.core.GopIntervalOption
import com.framecut.ui.core.LayoutDefinitions
import com.framecut.ui.core.RecordingSettings
import com.framecut.ui.core.UiConstants
import com.framecut.ui.core.VideoFpsOption
import com.framecut.ui.definitions.UiInteractionDefinitions
import com.framecut.ui.theme.FrameCutDimens

@Composable
fun SettingsScreen(
    settings: RecordingSettings,
    isIdle: Boolean,
    onBack: () -> Unit,
    onSelectBitrateMode: (BitrateModeOption) -> Unit,
    onSelectColorDepth: (ColorDepthOption) -> Unit,
    onSelectVideoFps: (VideoFpsOption) -> Unit,
    onSelectGopInterval: (GopIntervalOption) -> Unit,
    onSelectColorRange: (ColorRangeOption) -> Unit,
    onSelectAudioFormat: (AudioFormatOption) -> Unit,
    onSaveCustomFps: (Int) -> Unit,
    onResetCustomFps: () -> Unit,
    onSaveCustomGopSeconds: (Float) -> Unit,
    onResetCustomGop: () -> Unit,
    onSaveCustomAudio: (sampleRateHz: Int?, bitrateBps: Int?) -> Unit,
    onResetCustomAudio: () -> Unit,
) {
    val contentAlpha = if (isIdle) {
        UiInteractionDefinitions.States.idleContentAlpha
    } else {
        UiInteractionDefinitions.States.lockedContentAlpha
    }

    Scaffold(
        topBar = {
            FrameCutSubScreenTopBar(
                title = "Encoding Settings",
                subtitle = UiConstants.SETTINGS_SUBTITLE,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val useTwoColumns = maxWidth >= FrameCutDimens.SettingsExpandedBreakpoint
            val horizontalPadding =
                if (useTwoColumns) FrameCutDimens.Space3xl else FrameCutDimens.SpaceXl

            Column(
                modifier = Modifier
                    .widthIn(max = LayoutDefinitions.SettingsScreenSpec.expandedMaxWidthDp.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = FrameCutDimens.Space10),
                verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
            ) {
                if (!isIdle) {
                    SettingsLockedBannerPanel(
                        text = "Stop active recording to change pipeline settings.",
                        modifier = Modifier.testTag(UiConstants.TestTags.SETTINGS_LOCKED_BANNER),
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha),
                    verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
                ) {
                    if (useTwoColumns) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
                            ) {
                                BitrateModeCard(settings.bitrateMode, isIdle, onSelectBitrateMode)
                                VideoFpsCard(
                                    selected = settings.videoFps,
                                    customFps = settings.customVideoFps,
                                    enabled = isIdle,
                                    onSelect = onSelectVideoFps,
                                    onSaveCustomFps = onSaveCustomFps,
                                    onResetCustomFps = onResetCustomFps,
                                )
                                OutputContainerCard()
                                GopIntervalCard(
                                    selected = settings.gopInterval,
                                    customGopSeconds = settings.customGopSeconds,
                                    enabled = isIdle,
                                    onSelect = onSelectGopInterval,
                                    onSaveCustomGopSeconds = onSaveCustomGopSeconds,
                                    onResetCustomGop = onResetCustomGop,
                                )
                                AudioFormatCard(
                                    selected = settings.audioFormat,
                                    customSampleRateHz = settings.customAudioSampleRateHz,
                                    customBitrateBps = settings.customAudioBitrateBps,
                                    enabled = isIdle,
                                    onSelect = onSelectAudioFormat,
                                    onSaveCustomAudio = onSaveCustomAudio,
                                    onResetCustomAudio = onResetCustomAudio,
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
                            ) {
                                ColorDepthCard(settings.colorDepth, isIdle, onSelectColorDepth)
                                ColorStandardCard()
                                RenderingModeCard()
                                ColorRangeCard(settings.colorRange, isIdle, onSelectColorRange)
                                ChromaSubsamplingCard()
                            }
                        }
                    } else {
                        BitrateModeCard(settings.bitrateMode, isIdle, onSelectBitrateMode)
                        ColorDepthCard(settings.colorDepth, isIdle, onSelectColorDepth)
                        VideoFpsCard(
                            selected = settings.videoFps,
                            customFps = settings.customVideoFps,
                            enabled = isIdle,
                            onSelect = onSelectVideoFps,
                            onSaveCustomFps = onSaveCustomFps,
                            onResetCustomFps = onResetCustomFps,
                        )
                        ColorStandardCard()
                        OutputContainerCard()
                        RenderingModeCard()
                        GopIntervalCard(
                            selected = settings.gopInterval,
                            customGopSeconds = settings.customGopSeconds,
                            enabled = isIdle,
                            onSelect = onSelectGopInterval,
                            onSaveCustomGopSeconds = onSaveCustomGopSeconds,
                            onResetCustomGop = onResetCustomGop,
                        )
                        ColorRangeCard(settings.colorRange, isIdle, onSelectColorRange)
                        AudioFormatCard(
                            selected = settings.audioFormat,
                            customSampleRateHz = settings.customAudioSampleRateHz,
                            customBitrateBps = settings.customAudioBitrateBps,
                            enabled = isIdle,
                            onSelect = onSelectAudioFormat,
                            onSaveCustomAudio = onSaveCustomAudio,
                            onResetCustomAudio = onResetCustomAudio,
                        )
                        ChromaSubsamplingCard()
                    }
                }
                Spacer(Modifier.height(FrameCutDimens.SpaceMd))
            }
        }
    }
}

@Composable
private fun BitrateModeCard(selected: BitrateModeOption, enabled: Boolean, onSelect: (BitrateModeOption) -> Unit) {
    DefinedSectionCard(title = "Bitrate Mode", subtitle = "Controls bitrate allocation behavior") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
            BitrateModeOption.entries.forEach { opt ->
                DefinedOptionTile(
                    title = opt.label,
                    subtitle = opt.subtitle,
                    icon = FrameCutIcons.PipelineSpeed,
                    selected = selected == opt,
                    enabled = enabled,
                    onClick = { onSelect(opt) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ColorDepthCard(selected: ColorDepthOption, enabled: Boolean, onSelect: (ColorDepthOption) -> Unit) {
    DefinedSectionCard(title = "Color Depth", subtitle = "Selects hardware HEVC bit precision") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
            ColorDepthOption.entries.forEach { opt ->
                DefinedOptionTile(
                    title = opt.label,
                    subtitle = opt.subtitle,
                    icon = FrameCutIcons.PipelinePalette,
                    selected = selected == opt,
                    enabled = enabled,
                    onClick = { onSelect(opt) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun VideoFpsCard(
    selected: VideoFpsOption,
    customFps: Int?,
    enabled: Boolean,
    onSelect: (VideoFpsOption) -> Unit,
    onSaveCustomFps: (Int) -> Unit,
    onResetCustomFps: () -> Unit,
) {
    DefinedSectionCard(title = "Frame Rate (FPS)", subtitle = "Sets capture smoothness per second") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
            VideoFpsOption.entries.forEach { opt ->
                DefinedOptionTile(
                    title = opt.label,
                    subtitle = opt.subtitle,
                    icon = FrameCutIcons.PipelineSpeed,
                    selected = customFps == null && selected == opt,
                    enabled = enabled,
                    onClick = { onSelect(opt) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        CustomNumericInputBox(
            label = "Custom FPS",
            placeholder = "Enter target FPS",
            suffixText = "FPS",
            savedValueText = customFps?.toString().orEmpty(),
            activeBadgeText = customFps?.let { "Custom FPS: $it FPS" },
            allowDecimal = false,
            enabled = enabled,
            parseAndSave = { raw ->
                val v = raw.trim().toIntOrNull()
                if (v != null && v > 0) {
                    onSaveCustomFps(v)
                    true
                } else false
            },
            onReset = onResetCustomFps,
        )
    }
}

@Composable
private fun ColorStandardCard() {
    DefinedSectionCard(title = "Color Standard", subtitle = "Fixed hardware display color gamut") {
        DefinedOptionTile(
            title = "BT.709 (SDR)",
            subtitle = "Fixed accurate SDR color",
            icon = FrameCutIcons.PipelinePalette,
            selected = true,
            enabled = true,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OutputContainerCard() {
    DefinedSectionCard(title = "Output Container", subtitle = "Active MP4 muxing container format") {
        DefinedOptionTile(
            title = "Standard MP4",
            subtitle = "Native Android MediaMuxer",
            icon = FrameCutIcons.RecordingMovie,
            selected = true,
            enabled = true,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RenderingModeCard() {
    DefinedSectionCard(title = "Rendering Pipeline", subtitle = "Fixed zero-copy surface callback pipeline") {
        DefinedOptionTile(
            title = "Asynchronous Rendering",
            subtitle = "Direct Surface + Async Callback",
            icon = FrameCutIcons.PipelineSpeed,
            selected = true,
            enabled = true,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GopIntervalCard(
    selected: GopIntervalOption,
    customGopSeconds: Float?,
    enabled: Boolean,
    onSelect: (GopIntervalOption) -> Unit,
    onSaveCustomGopSeconds: (Float) -> Unit,
    onResetCustomGop: () -> Unit,
) {
    DefinedSectionCard(title = "Keyframe Interval (GOP)", subtitle = "Controls seeking & edit precision") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
            GopIntervalOption.entries.forEach { opt ->
                DefinedOptionTile(
                    title = opt.label,
                    subtitle = opt.subtitle,
                    icon = FrameCutIcons.PipelineSpeed,
                    selected = customGopSeconds == null && selected == opt,
                    enabled = enabled,
                    onClick = { onSelect(opt) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        CustomNumericInputBox(
            label = "Custom Keyframe Interval (sec)",
            placeholder = "≤ 1s supports decimal (e.g. 0.5)",
            suffixText = "s",
            savedValueText = customGopSeconds?.toString().orEmpty(),
            activeBadgeText = customGopSeconds?.let { "Custom GOP: $it s" },
            allowDecimal = true,
            enabled = enabled,
            supportingHint = "≤ 1s supports decimal (e.g. 0.5), > 1s seconds",
            parseAndSave = { raw ->
                val v = raw.trim().toFloatOrNull()
                if (v != null && v > 0f) {
                    onSaveCustomGopSeconds(v)
                    true
                } else false
            },
            onReset = onResetCustomGop,
        )
    }
}

@Composable
private fun ColorRangeCard(selected: ColorRangeOption, enabled: Boolean, onSelect: (ColorRangeOption) -> Unit) {
    DefinedSectionCard(title = "Color Range", subtitle = "Sets luminance & contrast span") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
            ColorRangeOption.entries.forEach { opt ->
                DefinedOptionTile(
                    title = opt.label,
                    subtitle = opt.subtitle,
                    icon = FrameCutIcons.PipelinePalette,
                    selected = selected == opt,
                    enabled = enabled,
                    onClick = { onSelect(opt) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AudioFormatCard(
    selected: AudioFormatOption,
    customSampleRateHz: Int?,
    customBitrateBps: Int?,
    enabled: Boolean,
    onSelect: (AudioFormatOption) -> Unit,
    onSaveCustomAudio: (sampleRateHz: Int?, bitrateBps: Int?) -> Unit,
    onResetCustomAudio: () -> Unit,
) {
    DefinedSectionCard(title = "Audio Format", subtitle = "Selects mono 48 kHz audio encoder") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
            AudioFormatOption.entries.forEach { opt ->
                DefinedOptionTile(
                    title = opt.label,
                    subtitle = opt.subtitle,
                    icon = FrameCutIcons.AudioInternal,
                    selected = selected == opt,
                    enabled = enabled,
                    onClick = { onSelect(opt) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
            CustomNumericInputBox(
                label = "Sample Rate (kHz)",
                placeholder = "e.g. 48",
                suffixText = "kHz",
                savedValueText = customSampleRateHz?.let { (it / 1000).toString() }.orEmpty(),
                activeBadgeText = customSampleRateHz?.let { "${it / 1000} kHz" },
                allowDecimal = false,
                enabled = enabled,
                parseAndSave = { raw ->
                    val khz = raw.trim().toIntOrNull()
                    if (khz != null && khz >= 8) {
                        onSaveCustomAudio(khz * 1000, customBitrateBps)
                        true
                    } else false
                },
                onReset = onResetCustomAudio,
                modifier = Modifier.weight(1f),
            )
            CustomNumericInputBox(
                label = "Audio Bitrate (kbps)",
                placeholder = "e.g. 128",
                suffixText = "kbps",
                savedValueText = customBitrateBps?.let { (it / 1000).toString() }.orEmpty(),
                activeBadgeText = customBitrateBps?.let { "${it / 1000} kbps" },
                allowDecimal = false,
                enabled = enabled,
                parseAndSave = { raw ->
                    val kbps = raw.trim().toIntOrNull()
                    if (kbps != null && kbps >= 16) {
                        onSaveCustomAudio(customSampleRateHz, kbps * 1000)
                        true
                    } else false
                },
                onReset = onResetCustomAudio,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChromaSubsamplingCard() {
    DefinedSectionCard(title = "Chroma Subsampling", subtitle = "Active hardware surface color sampling") {
        DefinedOptionTile(
            title = "4:2:0",
            subtitle = "Hardware native YUV420/P010",
            icon = FrameCutIcons.PipelinePalette,
            selected = true,
            enabled = true,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
