package com.framecut.ui.screens.crop

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.framecut.ui.components.common.CropEditorCanvas
import com.framecut.ui.components.panels.FeedbackBannerPanel
import com.framecut.ui.components.sliders.FrameCutLabeledSlider
import com.framecut.ui.components.switches.FrameCutLabeledSwitch
import com.framecut.ui.components.toolbars.FrameCutSubScreenTopBar
import com.framecut.ui.core.AspectRatioPreset
import com.framecut.ui.core.CropMediaCategory
import com.framecut.ui.core.CropMediaState
import com.framecut.ui.core.LayoutDefinitions
import com.framecut.ui.core.NormalizedRegion
import com.framecut.ui.core.RegionGeometry
import com.framecut.ui.core.UiConstants
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.FrameCutShapes
import kotlin.math.roundToInt

@Composable
fun CropToolScreen(
    onBack: () -> Unit,
    cropMedia: CropMediaState = CropMediaState(),
    cropPreviewBitmap: ImageBitmap? = null,
    onPickMedia: (CropMediaCategory) -> Unit = {},
    onBrowseAnyFile: () -> Unit = {},
    onExportCropped: (NormalizedRegion, Int, Boolean) -> Unit = { _, _, _ -> },
    onDismissStatus: () -> Unit = {},
) {
    var category by remember { mutableStateOf(CropMediaCategory.ALL) }
    var cropRect by remember { mutableStateOf(NormalizedRegion(0.08f, 0.08f, 0.92f, 0.92f)) }
    var aspectPreset by remember { mutableStateOf(AspectRatioPreset.FREE) }
    var qualityPercent by remember { mutableFloatStateOf(100f) }
    var preserveMetadata by remember { mutableStateOf(true) }

    val isFullFrame = cropRect.isFullFrame()
    val controlsEnabled = !cropMedia.isExporting
    val canvasAspect = cropMedia.aspectRatio
    val sourceW = if (cropMedia.mediaWidth > 0) cropMedia.mediaWidth else 1920
    val sourceH = if (cropMedia.mediaHeight > 0) cropMedia.mediaHeight else 1080
    val cropPixelPreview = remember(cropRect, sourceW, sourceH, cropMedia.isVideo) {
        RegionGeometry.toPixelRegion(
            region = cropRect,
            canvasWidthPx = sourceW,
            canvasHeightPx = sourceH,
            evenAlign = cropMedia.isVideo,
        )
    }

    Scaffold(
        topBar = {
            FrameCutSubScreenTopBar(
                title = "Media Crop Tool",
                subtitle = UiConstants.CROP_TOOL_SUBTITLE,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = LayoutDefinitions.CropScreenSpec.expandedMaxWidthDp.dp),
            ) {
                val categories = CropMediaCategory.entries
                TabRow(selectedTabIndex = categories.indexOf(category)) {
                    categories.forEach { cat ->
                        Tab(
                            selected = category == cat,
                            enabled = controlsEnabled,
                            onClick = { category = cat },
                            text = { Text(cat.label) },
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(FrameCutDimens.SpaceXl),
                    verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceMd),
                ) {
                    FeedbackBannerPanel(
                        bannerText = cropMedia.statusMessage,
                        isError = cropMedia.statusIsError,
                        onDismiss = onDismissStatus,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceSm),
                    ) {
                        OutlinedButton(
                            onClick = { onPickMedia(category) },
                            enabled = controlsEnabled,
                            modifier = Modifier.weight(1.1f),
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                            Text(
                                when (category) {
                                    CropMediaCategory.PHOTO -> "Pick Photo"
                                    CropMediaCategory.VIDEO -> "Pick Video"
                                    CropMediaCategory.ALL -> "Pick Media"
                                },
                            )
                        }
                        OutlinedButton(
                            onClick = onBrowseAnyFile,
                            enabled = controlsEnabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                            Text("Browse Files")
                        }
                    }

                    if (cropMedia.hasMedia) {
                        Surface(
                            shape = FrameCutShapes.TileShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = FrameCutDimens.SpaceMd, vertical = FrameCutDimens.SpaceSm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = cropMedia.displayName.orEmpty(),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                                Text(
                                    text = "${cropPixelPreview.width}×${cropPixelPreview.height} px • ${if (cropMedia.isVideo) "Video" else "Photo"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    CropEditorCanvas(
                        aspectRatio = canvasAspect,
                        cropRect = cropRect,
                        enabled = controlsEnabled && cropMedia.hasMedia,
                        onCropRectChange = {
                            aspectPreset = AspectRatioPreset.FREE
                            cropRect = it
                        },
                        previewContent = {
                            if (cropPreviewBitmap != null) {
                                Image(
                                    bitmap = cropPreviewBitmap,
                                    contentDescription = cropMedia.displayName ?: "Media preview",
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(enabled = controlsEnabled) { onPickMedia(category) }
                                        .padding(FrameCutDimens.SpaceXl),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Tap Pick Media or Browse Files to load a photo or video",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.72f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceSm),
                    ) {
                        AspectRatioPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = aspectPreset == preset,
                                enabled = controlsEnabled && cropMedia.hasMedia,
                                onClick = {
                                    aspectPreset = preset
                                    preset.ratio?.let { ratio ->
                                        cropRect = RegionGeometry.applyAspectRatio(
                                            current = cropRect,
                                            targetRatio = ratio,
                                            screenW = sourceW.toFloat(),
                                            screenH = sourceH.toFloat(),
                                        )
                                    }
                                },
                                label = { Text(preset.label) },
                            )
                        }
                        FilterChip(
                            selected = false,
                            enabled = controlsEnabled && cropMedia.hasMedia,
                            onClick = {
                                aspectPreset = AspectRatioPreset.FREE
                                cropRect = NormalizedRegion(0.08f, 0.08f, 0.92f, 0.92f)
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Reset") },
                        )
                        FilterChip(
                            selected = isFullFrame,
                            enabled = controlsEnabled && cropMedia.hasMedia,
                            onClick = {
                                aspectPreset = AspectRatioPreset.FREE
                                cropRect = NormalizedRegion(0f, 0f, 1f, 1f)
                            },
                            leadingIcon = { Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Full Frame") },
                        )
                    }

                    Card(
                        shape = FrameCutShapes.SectionCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(FrameCutDimens.SpaceXl),
                            verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceMd),
                        ) {
                            FrameCutLabeledSlider(
                                title = "Export Quality",
                                trailingSummary = "${qualityPercent.roundToInt()}%",
                                value = qualityPercent,
                                valueRange = 25f..100f,
                                enabled = controlsEnabled,
                                onValueChange = { qualityPercent = it },
                                minLabel = "25%",
                                maxLabel = "100%",
                            )
                            FrameCutLabeledSwitch(
                                title = "Preserve EXIF & Color Metadata",
                                subtitle = "Keeps camera tags and normalizes orientation",
                                checked = preserveMetadata,
                                enabled = controlsEnabled,
                                onCheckedChange = { preserveMetadata = it },
                            )
                        }
                    }

                    Button(
                        onClick = { onExportCropped(cropRect, qualityPercent.roundToInt(), preserveMetadata) },
                        enabled = controlsEnabled && cropMedia.hasMedia,
                        modifier = Modifier.fillMaxWidth().height(FrameCutDimens.PrimaryButtonHeight),
                        shape = FrameCutShapes.ActionButtonShape,
                    ) {
                        if (cropMedia.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                            Text(if (cropMedia.isVideo && !isFullFrame) "Cropping Video…" else "Exporting…")
                        } else {
                            Icon(imageVector = Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                            Text(if (isFullFrame) "Export 1:1 Original Stream Copy" else "Export Cropped Media")
                        }
                    }
                }
            }
        }
    }
}
