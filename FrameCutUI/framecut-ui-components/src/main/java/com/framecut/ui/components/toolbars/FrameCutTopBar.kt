package com.framecut.ui.components.toolbars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.framecut.ui.core.UiConstants
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.FrameCutShapes
import com.framecut.ui.theme.LocalStudioColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameCutMainTopBar(
    isDarkTheme: Boolean,
    recordingsCount: Int,
    onToggleTheme: () -> Unit,
    onOpenCropTool: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val studioColors = LocalStudioColors.current
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10),
            ) {
                Box(
                    modifier = Modifier
                        .size(FrameCutDimens.BrandBadgeSize)
                        .clip(FrameCutShapes.BrandBadgeShape)
                        .background(studioColors.recordContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = studioColors.recordAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = UiConstants.APP_NAME,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.testTag(UiConstants.TestTags.THEME_TOGGLE),
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
                )
            }
            IconButton(
                onClick = onOpenCropTool,
                modifier = Modifier.testTag(UiConstants.TestTags.NAV_CROP),
            ) {
                Icon(
                    imageVector = Icons.Default.Crop,
                    contentDescription = "Open photo and video crop tool",
                )
            }
            IconButton(
                onClick = onOpenRecordings,
                modifier = Modifier.testTag(UiConstants.TestTags.NAV_RECORDINGS),
            ) {
                BadgedBox(
                    badge = {
                        if (recordingsCount > 0) {
                            Badge { Text(recordingsCount.toString()) }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Open recorded videos",
                    )
                }
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag(UiConstants.TestTags.NAV_SETTINGS),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open encoding settings",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameCutSubScreenTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
