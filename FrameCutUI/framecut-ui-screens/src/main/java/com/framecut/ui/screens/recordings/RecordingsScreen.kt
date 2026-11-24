package com.framecut.ui.screens.recordings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.framecut.ui.components.dialogs.ConfirmDeleteRecordingDialog
import com.framecut.ui.components.icons.FrameCutIcons
import com.framecut.ui.components.toolbars.FrameCutSubScreenTopBar
import com.framecut.ui.core.LayoutDefinitions
import com.framecut.ui.core.RecordedVideoItem
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.FrameCutShapes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsScreen(
    recordings: List<RecordedVideoItem>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlay: (RecordedVideoItem) -> Unit,
    onShare: (RecordedVideoItem) -> Unit,
    onDelete: (RecordedVideoItem) -> Unit,
) {
    BackHandler(onBack = onBack)
    var pendingDelete by remember { mutableStateOf<RecordedVideoItem?>(null) }

    Scaffold(
        topBar = {
            FrameCutSubScreenTopBar(title = "Recorded Videos", onBack = onBack)
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val useGrid = maxWidth >= FrameCutDimens.RecordingsExpandedBreakpoint
            val gridPadding = if (useGrid) FrameCutDimens.SpaceTabletGrid else FrameCutDimens.SpaceXl

            when {
                isLoading && recordings.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                recordings.isEmpty() -> {
                    EmptyRecordingsState(
                        modifier = Modifier.fillMaxSize().padding(FrameCutDimens.SpaceEmptyState),
                    )
                }
                useGrid -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 340.dp),
                        modifier = Modifier.widthIn(max = LayoutDefinitions.RecordingsScreenSpec.expandedMaxWidthDp.dp).fillMaxSize(),
                        contentPadding = PaddingValues(gridPadding),
                        horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceMd),
                        verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceMd),
                    ) {
                        items(recordings, key = { it.id }) { item ->
                            RecordingItemCard(
                                item = item,
                                onPlay = { onPlay(item) },
                                onShare = { onShare(item) },
                                onRequestDelete = { pendingDelete = item },
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = LayoutDefinitions.RecordingsScreenSpec.compactMaxWidthDp.dp).fillMaxSize(),
                        contentPadding = PaddingValues(gridPadding),
                        verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceMd),
                    ) {
                        items(recordings, key = { it.id }) { item ->
                            RecordingItemCard(
                                item = item,
                                onPlay = { onPlay(item) },
                                onShare = { onShare(item) },
                                onRequestDelete = { pendingDelete = item },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        ConfirmDeleteRecordingDialog(
            displayName = item.displayName,
            onConfirm = {
                onDelete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun RecordingItemCard(
    item: RecordedVideoItem,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        shape = FrameCutShapes.RecordingCardShape,
        border = BorderStroke(FrameCutDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(FrameCutDimens.SpaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceMd),
        ) {
            Surface(
                modifier = Modifier.size(FrameCutDimens.RecordingThumbSize),
                shape = FrameCutShapes.CardThumbShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = FrameCutIcons.RecordingMovie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceXs),
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = remember(item) {
                    buildList {
                        if (item.width > 0 && item.height > 0) add("${item.width}×${item.height}")
                        if (item.durationMs > 0) add(formatDuration(item.durationMs))
                        add(formatFileSize(item.sizeBytes))
                        if (item.dateAddedSec > 0) add(formatDate(item.dateAddedSec))
                    }.joinToString(" • ")
                }
                Text(
                    text = details,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onPlay) {
                Icon(imageVector = FrameCutIcons.RecordingPlay, contentDescription = "Play")
            }
            IconButton(onClick = onShare) {
                Icon(imageVector = FrameCutIcons.RecordingShare, contentDescription = "Share")
            }
            IconButton(onClick = onRequestDelete) {
                Icon(imageVector = FrameCutIcons.RecordingDelete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun EmptyRecordingsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(FrameCutDimens.EmptyStateBoxSize),
            shape = FrameCutShapes.EmptyStateBoxShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = FrameCutIcons.NavRecordings,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(FrameCutDimens.SpaceXl))
        Text(text = "No recordings yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(FrameCutDimens.Space6))
        Text(
            text = "Captured MP4 videos will appear here ready to play or share.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSec / 60L, totalSec % 60L)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

private fun formatDate(epochSec: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(epochSec * 1000L))
