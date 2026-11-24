package com.framecut.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.framecut.ui.components.buttons.FrameCutLibraryButton
import com.framecut.ui.components.buttons.FrameCutRecordButton
import com.framecut.ui.components.buttons.FrameCutTonalActionButton
import com.framecut.ui.components.cards.DefinedOptionTile
import com.framecut.ui.components.cards.DefinedSectionCard
import com.framecut.ui.components.common.RegionBorderOverlayCanvas
import com.framecut.ui.components.icons.FrameCutIcons
import com.framecut.ui.components.inputs.CustomNumericInputBox
import com.framecut.ui.components.panels.FeedbackBannerPanel
import com.framecut.ui.components.panels.SavedVideoBannerPanel
import com.framecut.ui.components.toolbars.FrameCutMainTopBar
import com.framecut.ui.core.AudioSourceOption
import com.framecut.ui.core.FrameCutUiState
import com.framecut.ui.core.LayoutDefinitions
import com.framecut.ui.core.OrientationMode
import com.framecut.ui.core.RecordingMode
import com.framecut.ui.core.RecordingPhase
import com.framecut.ui.core.RegionAnimationOption
import com.framecut.ui.core.UiConstants
import com.framecut.ui.core.VideoBitrateOption
import com.framecut.ui.definitions.UiAnimationDefinitions
import com.framecut.ui.definitions.UiInteractionDefinitions
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.FrameCutShapes
import com.framecut.ui.theme.LocalStudioColors

@Composable
fun MainScreen(
    uiState: FrameCutUiState,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onStartRecordClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCropTool: () -> Unit,
    onSelectMode: (RecordingMode) -> Unit,
    onSelectOrientationMode: (OrientationMode) -> Unit,
    onSelectAudioSource: (AudioSourceOption) -> Unit,
    onSelectBitrate: (VideoBitrateOption) -> Unit,
    onSelectRegionAnimation: (RegionAnimationOption) -> Unit,
    onEditRegion: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onDismissMessages: () -> Unit,
    onSaveCustomBitrateMbps: (Long) -> Unit,
    onResetCustomBitrate: () -> Unit,
) {
    val isIdle = uiState.isIdle
    val stateAlphas = UiInteractionDefinitions.States
    val settingsAlpha = if (isIdle) stateAlphas.idleContentAlpha else stateAlphas.lockedContentAlpha

    Scaffold(
        topBar = {
            FrameCutMainTopBar(
                isDarkTheme = isDarkTheme,
                recordingsCount = uiState.recordings.size,
                onToggleTheme = onToggleTheme,
                onOpenCropTool = onOpenCropTool,
                onOpenRecordings = onOpenRecordings,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isExpandedTwoPane = maxWidth >= FrameCutDimens.MainExpandedBreakpoint
            val useTwoColRight = maxWidth >= FrameCutDimens.MainRightTwoColBreakpoint
            val isWideGrid = maxWidth >= FrameCutDimens.MainSettingsGridBreakpoint
            val horizontalPadding =
                if (isExpandedTwoPane) FrameCutDimens.Space3xl else FrameCutDimens.SpaceXl
            val verticalPadding =
                if (isExpandedTwoPane) FrameCutDimens.SpaceLg else FrameCutDimens.SpaceSm
            val bannerText =
                uiState.session.errorMessage ?: uiState.session.warningMessage ?: uiState.recoveryNotice

            if (isExpandedTwoPane) {
                Row(
                    modifier = Modifier
                        .widthIn(max = LayoutDefinitions.MainScreenSpec.expandedMaxWidthDp.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                    horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space2xl),
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = FrameCutDimens.MainLeftPaneMinWidth, max = FrameCutDimens.MainLeftPaneMaxWidth)
                            .weight(0.38f),
                        verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
                    ) {
                        FeedbackBannerPanel(
                            bannerText = bannerText,
                            isError = uiState.session.errorMessage != null,
                            onDismiss = onDismissMessages,
                        )
                        RecordingConsoleCard(
                            uiState = uiState,
                            onStartRecordClick = onStartRecordClick,
                            onPauseResumeClick = onPauseResumeClick,
                            onStopClick = onStopClick,
                        )
                        SavedVideoBannerPanel(
                            visible = isIdle && uiState.session.lastSavedName != null,
                            fileName = uiState.session.lastSavedName,
                            onOpenRecordings = onOpenRecordings,
                        )
                        FrameCutLibraryButton(
                            text = if (uiState.recordings.isEmpty()) "Recorded Videos" else "Recorded Videos (${uiState.recordings.size})",
                            icon = FrameCutIcons.NavRecordings,
                            onClick = onOpenRecordings,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.62f),
                        verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
                    ) {
                        SettingsCardsSection(
                            uiState = uiState,
                            settingsAlpha = settingsAlpha,
                            twoColumns = useTwoColRight,
                            onSelectMode = onSelectMode,
                            onSelectOrientationMode = onSelectOrientationMode,
                            onSelectAudioSource = onSelectAudioSource,
                            onSelectBitrate = onSelectBitrate,
                            onSaveCustomBitrateMbps = onSaveCustomBitrateMbps,
                            onResetCustomBitrate = onResetCustomBitrate,
                            onSelectRegionAnimation = onSelectRegionAnimation,
                            onEditRegion = onEditRegion,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = LayoutDefinitions.MainScreenSpec.compactMaxWidthDp.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                    verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
                ) {
                    FeedbackBannerPanel(
                        bannerText = bannerText,
                        isError = uiState.session.errorMessage != null,
                        onDismiss = onDismissMessages,
                    )
                    RecordingConsoleCard(
                        uiState = uiState,
                        onStartRecordClick = onStartRecordClick,
                        onPauseResumeClick = onPauseResumeClick,
                        onStopClick = onStopClick,
                    )
                    SavedVideoBannerPanel(
                        visible = isIdle && uiState.session.lastSavedName != null,
                        fileName = uiState.session.lastSavedName,
                        onOpenRecordings = onOpenRecordings,
                    )
                    SettingsCardsSection(
                        uiState = uiState,
                        settingsAlpha = settingsAlpha,
                        twoColumns = isWideGrid,
                        onSelectMode = onSelectMode,
                        onSelectOrientationMode = onSelectOrientationMode,
                        onSelectAudioSource = onSelectAudioSource,
                        onSelectBitrate = onSelectBitrate,
                        onSaveCustomBitrateMbps = onSaveCustomBitrateMbps,
                        onResetCustomBitrate = onResetCustomBitrate,
                        onSelectRegionAnimation = onSelectRegionAnimation,
                        onEditRegion = onEditRegion,
                        onRequestOverlayPermission = onRequestOverlayPermission,
                    )
                    FrameCutLibraryButton(
                        text = if (uiState.recordings.isEmpty()) "Recorded Videos" else "Recorded Videos (${uiState.recordings.size})",
                        icon = FrameCutIcons.NavRecordings,
                        onClick = onOpenRecordings,
                    )
                    Spacer(Modifier.height(FrameCutDimens.SpaceMd))
                }
            }
        }
    }
}

@Composable
private fun RecordingConsoleCard(
    uiState: FrameCutUiState,
    onStartRecordClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    val studioColors = LocalStudioColors.current
    val phase = uiState.session.phase

    val statusLabel = when (phase) {
        RecordingPhase.IDLE -> "Ready to record"
        RecordingPhase.RECORDING -> "Recording"
        RecordingPhase.PAUSED -> "Paused"
        RecordingPhase.FINALIZING -> "Finalizing MP4…"
    }

    val statusColor by animateColorAsState(
        targetValue = when (phase) {
            RecordingPhase.IDLE -> studioColors.readyAccent
            RecordingPhase.RECORDING -> studioColors.recordAccent
            RecordingPhase.PAUSED -> studioColors.pausedAccent
            RecordingPhase.FINALIZING -> MaterialTheme.colorScheme.secondary
        },
        animationSpec = tween(UiAnimationDefinitions.StatusBadgeColor.durationMillis),
        label = UiAnimationDefinitions.StatusBadgeColor.label,
    )

    val statusContainer by animateColorAsState(
        targetValue = when (phase) {
            RecordingPhase.IDLE -> studioColors.readyContainer
            RecordingPhase.RECORDING -> studioColors.recordContainer
            RecordingPhase.PAUSED -> studioColors.pausedContainer
            RecordingPhase.FINALIZING -> MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = tween(UiAnimationDefinitions.StatusBadgeContainer.durationMillis),
        label = UiAnimationDefinitions.StatusBadgeContainer.label,
    )

    val pulseSpec = UiAnimationDefinitions.RecordingDotPulse
    val dotAlphaState: State<Float>? = if (phase == RecordingPhase.RECORDING) {
        val transition = rememberInfiniteTransition(label = pulseSpec.transitionLabel)
        transition.animateFloat(
            initialValue = pulseSpec.initialValue,
            targetValue = pulseSpec.targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(pulseSpec.durationMillis, easing = pulseSpec.easing),
                repeatMode = pulseSpec.repeatMode,
            ),
            label = pulseSpec.valueLabel,
        )
    } else null

    val surfaceColor = MaterialTheme.colorScheme.surface
    val headerGradient = remember(statusContainer, surfaceColor) {
        Brush.verticalGradient(
            colors = listOf(
                statusContainer.copy(alpha = UiInteractionDefinitions.States.statusHeaderGradientAlpha),
                surfaceColor,
            ),
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FrameCutShapes.ConsoleCardShape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(FrameCutDimens.BorderThin, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(headerGradient).padding(FrameCutDimens.Space2xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
        ) {
            Surface(
                shape = CircleShape,
                color = statusContainer,
                modifier = Modifier.testTag(UiConstants.TestTags.RECORDING_STATUS_BADGE),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = FrameCutDimens.SpaceLg, vertical = FrameCutDimens.Space6),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceSm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(FrameCutDimens.StatusDotSize)
                            .graphicsLayer { alpha = dotAlphaState?.value ?: 1f }
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Text(text = statusLabel, style = MaterialTheme.typography.labelLarge, color = statusColor)
                }
            }

            Text(
                text = formatElapsedTimer(uiState.session.elapsedMillis),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(UiConstants.TestTags.RECORDING_TIMER),
            )

            val modeLabel = if (uiState.settings.recordingMode == RecordingMode.FULL_SCREEN) {
                "Full Screen (${uiState.canvasWidthPx}×${uiState.canvasHeightPx})"
            } else {
                "Region / Area (${uiState.selectedPixelRegion.width}×${uiState.selectedPixelRegion.height})"
            }
            val orientationLabel = if (uiState.settings.orientationMode == OrientationMode.PORTRAIT) "Portrait" else "Landscape"
            val audioLabel = if (uiState.settings.audioSource == AudioSourceOption.INTERNAL) "Internal Audio" else "Microphone"

            Text(
                text = "$modeLabel • $orientationLabel • $audioLabel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (phase) {
                RecordingPhase.IDLE -> {
                    FrameCutRecordButton(
                        text = "Record",
                        icon = FrameCutIcons.RecordAction,
                        onClick = onStartRecordClick,
                    )
                }
                RecordingPhase.RECORDING,
                RecordingPhase.PAUSED -> {
                    val isPaused = phase == RecordingPhase.PAUSED
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceMd),
                    ) {
                        FrameCutTonalActionButton(
                            text = if (isPaused) "Resume" else "Pause",
                            icon = if (isPaused) FrameCutIcons.ResumeAction else FrameCutIcons.PauseAction,
                            onClick = onPauseResumeClick,
                            modifier = Modifier.weight(1f),
                            testTag = UiConstants.TestTags.BTN_PAUSE_RESUME,
                        )
                        Button(
                            onClick = onStopClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(FrameCutDimens.SecondaryButtonHeight)
                                .testTag(UiConstants.TestTags.BTN_STOP),
                            shape = FrameCutShapes.ActionButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = studioColors.recordAccent,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(imageVector = FrameCutIcons.StopAction, contentDescription = null)
                            Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                            Text("Stop")
                        }
                    }
                }
                RecordingPhase.FINALIZING -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(FrameCutDimens.PrimaryButtonHeight),
                        shape = FrameCutShapes.ActionButtonShape,
                    ) {
                        Text("Finalizing MP4…")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCardsSection(
    uiState: FrameCutUiState,
    settingsAlpha: Float,
    twoColumns: Boolean,
    onSelectMode: (RecordingMode) -> Unit,
    onSelectOrientationMode: (OrientationMode) -> Unit,
    onSelectAudioSource: (AudioSourceOption) -> Unit,
    onSelectBitrate: (VideoBitrateOption) -> Unit,
    onSaveCustomBitrateMbps: (Long) -> Unit,
    onResetCustomBitrate: () -> Unit,
    onSelectRegionAnimation: (RegionAnimationOption) -> Unit,
    onEditRegion: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
) {
    val settings = uiState.settings
    val isIdle = uiState.isIdle

    val leftColumnCards: @Composable () -> Unit = {
        Box(modifier = Modifier.graphicsLayer { alpha = settingsAlpha }) {
            DefinedSectionCard(title = "Recording Mode") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
                    DefinedOptionTile(
                        title = "Full Screen",
                        subtitle = "Entire display capture",
                        icon = FrameCutIcons.ModeFullScreen,
                        selected = settings.recordingMode == RecordingMode.FULL_SCREEN,
                        enabled = isIdle,
                        onClick = { onSelectMode(RecordingMode.FULL_SCREEN) },
                        modifier = Modifier.weight(1f),
                    )
                    DefinedOptionTile(
                        title = "Region / Area",
                        subtitle = "Hardware-sized region capture",
                        icon = FrameCutIcons.ModeRegion,
                        selected = settings.recordingMode == RecordingMode.REGION,
                        enabled = isIdle,
                        onClick = { onSelectMode(RecordingMode.REGION) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = settings.recordingMode == RecordingMode.REGION,
            enter = UiAnimationDefinitions.RegionPreviewVisibility.enterTransition(),
            exit = UiAnimationDefinitions.RegionPreviewVisibility.exitTransition(),
        ) {
            Box(modifier = Modifier.graphicsLayer { alpha = settingsAlpha }) {
                RegionPreviewCard(
                    uiState = uiState,
                    onEditRegion = onEditRegion,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                )
            }
        }

        Box(modifier = Modifier.graphicsLayer { alpha = settingsAlpha }) {
            DefinedSectionCard(title = "Orientation") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
                    DefinedOptionTile(
                        title = "Portrait",
                        subtitle = "Vertical for Full Screen & Region",
                        icon = FrameCutIcons.OrientationPortrait,
                        selected = settings.orientationMode == OrientationMode.PORTRAIT,
                        enabled = isIdle,
                        onClick = { onSelectOrientationMode(OrientationMode.PORTRAIT) },
                        modifier = Modifier.weight(1f),
                    )
                    DefinedOptionTile(
                        title = "Landscape",
                        subtitle = "Horizontal for Full Screen & Region",
                        icon = FrameCutIcons.OrientationLandscape,
                        selected = settings.orientationMode == OrientationMode.LANDSCAPE,
                        enabled = isIdle,
                        onClick = { onSelectOrientationMode(OrientationMode.LANDSCAPE) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    val rightColumnCards: @Composable () -> Unit = {
        Box(modifier = Modifier.graphicsLayer { alpha = settingsAlpha }) {
            DefinedSectionCard(title = "Audio Source") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
                    DefinedOptionTile(
                        title = "Internal Audio",
                        subtitle = "Device media & app playback",
                        icon = FrameCutIcons.AudioInternal,
                        selected = settings.audioSource == AudioSourceOption.INTERNAL,
                        enabled = isIdle,
                        onClick = { onSelectAudioSource(AudioSourceOption.INTERNAL) },
                        modifier = Modifier.weight(1f),
                    )
                    DefinedOptionTile(
                        title = "Microphone",
                        subtitle = "External mic prioritized automatically",
                        icon = FrameCutIcons.AudioMicrophone,
                        selected = settings.audioSource == AudioSourceOption.MICROPHONE,
                        enabled = isIdle,
                        onClick = { onSelectAudioSource(AudioSourceOption.MICROPHONE) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Box(modifier = Modifier.graphicsLayer { alpha = settingsAlpha }) {
            VideoBitrateCard(
                selectedBitrate = settings.videoBitrate,
                customBitrateMbps = settings.customVideoBitrateMbps,
                enabled = isIdle,
                onSelectBitrate = onSelectBitrate,
                onSaveCustomBitrateMbps = onSaveCustomBitrateMbps,
                onResetCustomBitrate = onResetCustomBitrate,
            )
        }

        DefinedSectionCard(title = "Region Border Animation") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10)) {
                DefinedOptionTile(
                    title = "Show",
                    subtitle = "Animated red capture frame",
                    icon = FrameCutIcons.AnimShow,
                    selected = settings.regionAnimation == RegionAnimationOption.SHOW,
                    enabled = true,
                    onClick = { onSelectRegionAnimation(RegionAnimationOption.SHOW) },
                    modifier = Modifier.weight(1f),
                )
                DefinedOptionTile(
                    title = "Hide",
                    subtitle = "Clean borderless capture",
                    icon = FrameCutIcons.AnimHide,
                    selected = settings.regionAnimation == RegionAnimationOption.HIDE,
                    enabled = true,
                    onClick = { onSelectRegionAnimation(RegionAnimationOption.HIDE) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (twoColumns) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg)) {
                leftColumnCards()
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg)) {
                rightColumnCards()
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg)) {
            leftColumnCards()
            rightColumnCards()
        }
    }
}

@Composable
private fun RegionPreviewCard(
    uiState: FrameCutUiState,
    onEditRegion: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
) {
    val norm = uiState.settings.selectedRegion
    val pixel = uiState.selectedPixelRegion
    val aspect = (uiState.canvasWidthPx.toFloat() / uiState.canvasHeightPx.toFloat().coerceAtLeast(1f)).coerceIn(0.35f, 2.5f)
    val showAnim = uiState.settings.regionAnimation == RegionAnimationOption.SHOW
    val studioColors = LocalStudioColors.current

    DefinedSectionCard(title = "Selected Region") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(FrameCutDimens.RegionPreviewWidth)
                    .aspectRatio(aspect)
                    .clip(FrameCutShapes.CardThumbShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                RegionBorderOverlayCanvas(
                    leftFraction = norm.left,
                    topFraction = norm.top,
                    rightFraction = norm.right,
                    bottomFraction = norm.bottom,
                    animate = showAnim && uiState.session.phase == RecordingPhase.RECORDING,
                    showBorder = showAnim,
                    scrimColor = studioColors.regionPreviewScrim,
                    neutralGuideColor = MaterialTheme.colorScheme.outline,
                    strokeWidth = FrameCutDimens.BorderRegionPreviewStroke,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FrameCutDimens.Space6)) {
                Text(text = "${pixel.width} × ${pixel.height} px", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Position (${pixel.left}, ${pixel.top})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = onEditRegion,
                    enabled = uiState.isIdle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = FrameCutIcons.AdjustRegion, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                    Text("Adjust Region")
                }
            }
        }

        Surface(
            shape = FrameCutShapes.SubRowShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = UiInteractionDefinitions.States.subRowContainerAlpha),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FrameCutDimens.SpaceMd, vertical = FrameCutDimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Floating border over other apps", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = if (uiState.hasOverlayPermission) "Active during region recording"
                        else "Optional: region still records cleanly without it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!uiState.hasOverlayPermission) {
                    TextButton(onClick = onRequestOverlayPermission, enabled = uiState.isIdle) {
                        Text("Enable (Optional)")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoBitrateCard(
    selectedBitrate: VideoBitrateOption,
    customBitrateMbps: Long?,
    enabled: Boolean,
    onSelectBitrate: (VideoBitrateOption) -> Unit,
    onSaveCustomBitrateMbps: (Long) -> Unit,
    onResetCustomBitrate: () -> Unit,
) {
    DefinedSectionCard(title = "Video Bitrate") {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceSm),
        ) {
            VideoBitrateOption.entries.forEach { option ->
                val isSelected = customBitrateMbps == null && option == selectedBitrate
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectBitrate(option) },
                    enabled = enabled,
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
            }
        }

        CustomNumericInputBox(
            label = "Custom Bitrate (Mbps)",
            placeholder = "Enter any Mbps value",
            suffixText = "Mbps",
            savedValueText = customBitrateMbps?.toString().orEmpty(),
            activeBadgeText = customBitrateMbps?.let { "Custom Bitrate: $it Mbps" },
            allowDecimal = false,
            enabled = enabled,
            parseAndSave = { raw ->
                val mbps = raw.trim().toLongOrNull()
                if (mbps != null && mbps > 0L) {
                    onSaveCustomBitrateMbps(mbps)
                    true
                } else false
            },
            onReset = onResetCustomBitrate,
        )
    }
}

private fun formatElapsedTimer(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
