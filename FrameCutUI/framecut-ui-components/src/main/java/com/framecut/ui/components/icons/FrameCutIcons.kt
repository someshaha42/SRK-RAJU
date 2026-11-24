package com.framecut.ui.components.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector

object FrameCutIcons {
    val BrandRecord: ImageVector = Icons.Default.Videocam
    val ThemeLight: ImageVector = Icons.Default.LightMode
    val ThemeDark: ImageVector = Icons.Default.DarkMode
    val NavBack: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val NavRecordings: ImageVector = Icons.Default.VideoLibrary
    val NavSettings: ImageVector = Icons.Default.Settings
    val NavCrop: ImageVector = Icons.Default.Crop

    val RecordAction: ImageVector = Icons.Default.FiberManualRecord
    val PauseAction: ImageVector = Icons.Default.Pause
    val ResumeAction: ImageVector = Icons.Default.PlayArrow
    val StopAction: ImageVector = Icons.Default.Stop

    val ModeFullScreen: ImageVector = Icons.Default.Fullscreen
    val ModeRegion: ImageVector = Icons.Default.CropFree
    val OrientationPortrait: ImageVector = Icons.Default.StayCurrentPortrait
    val OrientationLandscape: ImageVector = Icons.Default.StayCurrentLandscape
    val AudioInternal: ImageVector = Icons.Default.GraphicEq
    val AudioMicrophone: ImageVector = Icons.Default.Mic
    val AnimShow: ImageVector = Icons.Default.Visibility
    val AnimHide: ImageVector = Icons.Default.VisibilityOff
    val AdjustRegion: ImageVector = Icons.Default.Tune

    val RegionDrag: ImageVector = Icons.Default.OpenWith
    val RegionReset: ImageVector = Icons.Default.Refresh
    val RegionCancel: ImageVector = Icons.Default.Close
    val RegionConfirm: ImageVector = Icons.Default.Check

    val PipelineSpeed: ImageVector = Icons.Default.Speed
    val PipelinePalette: ImageVector = Icons.Default.Palette
    val RecordingMovie: ImageVector = Icons.Default.Movie
    val RecordingPlay: ImageVector = Icons.Default.PlayArrow
    val RecordingShare: ImageVector = Icons.Default.Share
    val RecordingDelete: ImageVector = Icons.Default.Delete
}
