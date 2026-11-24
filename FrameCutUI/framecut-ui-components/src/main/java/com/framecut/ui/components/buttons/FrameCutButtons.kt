package com.framecut.ui.components.buttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.FrameCutShapes
import com.framecut.ui.theme.LocalStudioColors

@Composable
fun FrameCutRecordButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "btn_record",
) {
    val studioColors = LocalStudioColors.current
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(FrameCutDimens.PrimaryButtonHeight)
            .testTag(testTag),
        shape = FrameCutShapes.ActionButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = studioColors.recordAccent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = PaddingValues(horizontal = FrameCutDimens.SpaceXl),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(FrameCutDimens.SpaceSm))
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun FrameCutTonalActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "",
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(FrameCutDimens.SecondaryButtonHeight)
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
        shape = FrameCutShapes.ActionButtonShape,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(FrameCutDimens.SpaceSm))
        Text(text = text)
    }
}

@Composable
fun FrameCutLibraryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "btn_open_recordings",
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(FrameCutDimens.LibraryButtonHeight)
            .testTag(testTag),
        shape = FrameCutShapes.ActionButtonShape,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(FrameCutDimens.SpaceSm))
        Text(text = text)
    }
}
