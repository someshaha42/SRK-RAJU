package com.framecut.ui.screens.crop

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
import androidx.compose.ui.unit.dp
import com.framecut.ui.components.common.CropEditorCanvas
import com.framecut.ui.components.sliders.FrameCutLabeledSlider
import com.framecut.ui.components.switches.FrameCutLabeledSwitch
import com.framecut.ui.components.toolbars.FrameCutSubScreenTopBar
import com.framecut.ui.core.AspectRatioPreset
import com.framecut.ui.core.CropMediaCategory
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
    onPickMedia: (CropMediaCategory) -> Unit = {},
    onBrowseAnyFile: () -> Unit = {},
    onExportCropped: (NormalizedRegion, Int, Boolean) -> Unit = { _, _, _ -> },
) {
    var category by remember { mutableStateOf(CropMediaCategory.ALL) }
    var cropRect by remember { mutableStateOf(NormalizedRegion(0.08f, 0.08f, 0.92f, 0.92f)) }
    var aspectPreset by remember { mutableStateOf(AspectRatioPreset.FREE) }
    var qualityPercent by remember { mutableFloatStateOf(100f) }
    var preserveMetadata by remember { mutableStateOf(true) }

    val isFullFrame = cropRect.isFullFrame()

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
                            onClick = {
                                category = cat
                                onPickMedia(cat)
                            },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceSm),
                    ) {
                        OutlinedButton(
                            onClick = { onPickMedia(category) },
                            modifier = Modifier.weight(1.1f),
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                            Text("Pick Media")
                        }
                        OutlinedButton(
                            onClick = onBrowseAnyFile,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                            Text("Browse Files")
                        }
                    }

                    CropEditorCanvas(
                        aspectRatio = 16f / 9f,
                        cropRect = cropRect,
                        enabled = true,
                        onCropRectChange = {
                            aspectPreset = AspectRatioPreset.FREE
                            cropRect = it
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceSm),
                    ) {
                        AspectRatioPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = aspectPreset == preset,
                                onClick = {
                                    aspectPreset = preset
                                    preset.ratio?.let { ratio ->
                                        cropRect = RegionGeometry.applyAspectRatio(cropRect, ratio, 1920f, 1080f)
                                    }
                                },
                                label = { Text(preset.label) },
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = {
                                aspectPreset = AspectRatioPreset.FREE
                                cropRect = NormalizedRegion(0.08f, 0.08f, 0.92f, 0.92f)
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Reset") },
                        )
                        FilterChip(
                            selected = isFullFrame,
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
                                onValueChange = { qualityPercent = it },
                                minLabel = "25%",
                                maxLabel = "100%",
                            )
                            FrameCutLabeledSwitch(
                                title = "Preserve EXIF & Color Metadata",
                                subtitle = "Keeps camera tags and normalizes orientation",
                                checked = preserveMetadata,
                                onCheckedChange = { preserveMetadata = it },
                            )
                        }
                    }

                    Button(
                        onClick = { onExportCropped(cropRect, qualityPercent.roundToInt(), preserveMetadata) },
                        modifier = Modifier.fillMaxWidth().height(FrameCutDimens.PrimaryButtonHeight),
                        shape = FrameCutShapes.ActionButtonShape,
                    ) {
                        Icon(imageVector = Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(FrameCutDimens.SpaceSm))
                        Text(if (isFullFrame) "Export 1:1 Original Stream Copy" else "Export Cropped Media")
                    }
                }
            }
        }
    }
}
