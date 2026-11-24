package com.framecut.ui.components.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.framecut.ui.definitions.UiAnimationDefinitions
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.FrameCutShapes
import com.framecut.ui.theme.LocalStudioColors

@Composable
fun FeedbackBannerPanel(
    bannerText: String?,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anim = UiAnimationDefinitions.FeedbackBannerVisibility
    AnimatedVisibility(
        visible = bannerText != null,
        enter = anim.enterTransition(),
        exit = anim.exitTransition(),
        label = anim.label,
        modifier = modifier,
    ) {
        if (bannerText != null) {
            Surface(
                shape = FrameCutShapes.BannerShape,
                color =
                    if (isError) MaterialTheme.colorScheme.errorContainer
                    else LocalStudioColors.current.pausedContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FrameCutDimens.SpaceXl, vertical = FrameCutDimens.Space10),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun SavedVideoBannerPanel(
    visible: Boolean,
    fileName: String?,
    onOpenRecordings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anim = UiAnimationDefinitions.SavedBannerVisibility
    val studioColors = LocalStudioColors.current
    AnimatedVisibility(
        visible = visible && fileName != null,
        enter = anim.enterTransition(),
        exit = anim.exitTransition(),
        label = anim.label,
        modifier = modifier,
    ) {
        if (fileName != null) {
            Surface(
                onClick = onOpenRecordings,
                shape = FrameCutShapes.BannerShape,
                color = studioColors.readyContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FrameCutDimens.SpaceLg, vertical = FrameCutDimens.SpaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved to Movies/FrameCut",
                            style = MaterialTheme.typography.labelLarge,
                            color = studioColors.readyAccent,
                        )
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "Play",
                        style = MaterialTheme.typography.labelLarge,
                        color = studioColors.readyAccent,
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsLockedBannerPanel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val studioColors = LocalStudioColors.current
    Surface(
        shape = FrameCutShapes.BannerShape,
        color = studioColors.pausedContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FrameCutDimens.SpaceLg, vertical = FrameCutDimens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.Space10),
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = studioColors.pausedAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
