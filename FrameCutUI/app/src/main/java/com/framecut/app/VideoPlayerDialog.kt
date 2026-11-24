package com.framecut.app

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framecut.ui.core.RecordedVideoItem
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.FrameCutShapes

@Composable
fun VideoPlayerDialog(
    item: RecordedVideoItem,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    val videoUri = runCatching { Uri.parse(item.uriString) }.getOrNull()
    val aspect = if (item.width > 0 && item.height > 0) {
        (item.width.toFloat() / item.height.toFloat()).coerceIn(0.45f, 2.2f)
    } else {
        9f / 16f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FrameCutDimens.SpaceXl),
            shape = FrameCutShapes.DialogShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FrameCutDimens.SpaceLg),
                verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceMd),
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (videoUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect),
                        contentAlignment = Alignment.Center,
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setVideoURI(videoUri)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = false
                                        start()
                                    }
                                }
                            },
                        )
                    }
                } else {
                    Text(
                        text = "Unable to resolve video file URI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceSm),
                ) {
                    OutlinedButton(
                        onClick = onOpenExternal,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("External Player")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
