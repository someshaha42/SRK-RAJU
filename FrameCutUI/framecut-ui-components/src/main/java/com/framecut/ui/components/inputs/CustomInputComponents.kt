package com.framecut.ui.components.inputs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.framecut.ui.theme.FrameCutDimens
import com.framecut.ui.theme.FrameCutShapes

@Composable
fun CustomNumericInputBox(
    label: String,
    placeholder: String,
    suffixText: String,
    savedValueText: String,
    activeBadgeText: String?,
    allowDecimal: Boolean,
    enabled: Boolean,
    parseAndSave: (String) -> Boolean,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    supportingHint: String? = null,
) {
    var draft by remember(savedValueText) { mutableStateOf(savedValueText) }
    var actionBarVisible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedValueText) {
        draft = savedValueText
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FrameCutDimens.SpaceSm),
    ) {
        if (!activeBadgeText.isNullOrBlank()) {
            Surface(
                shape = FrameCutShapes.ChipShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = activeBadgeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = FrameCutDimens.Space10, vertical = FrameCutDimens.SpaceXs),
                )
            }
        }

        OutlinedTextField(
            value = draft,
            onValueChange = { raw ->
                val filtered = raw.filter { ch ->
                    ch.isDigit() || (allowDecimal && ch == '.')
                }
                draft = filtered
                errorText = null
                actionBarVisible = true
            },
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            suffix = { Text(suffixText) },
            isError = errorText != null,
            supportingText = {
                val msg = errorText ?: supportingHint
                if (msg != null) Text(msg)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (parseAndSave(draft)) {
                        errorText = null
                        actionBarVisible = false
                    } else {
                        errorText = "Enter a valid positive number"
                    }
                },
            ),
            shape = FrameCutShapes.TileShape,
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = enabled && (actionBarVisible || savedValueText.isNotEmpty())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (savedValueText.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            onReset()
                            draft = ""
                            errorText = null
                            actionBarVisible = false
                        },
                        modifier = Modifier.padding(end = FrameCutDimens.SpaceSm),
                    ) {
                        Text("Reset")
                    }
                }
                Button(
                    onClick = {
                        if (parseAndSave(draft)) {
                            errorText = null
                            actionBarVisible = false
                        } else {
                            errorText = "Enter a valid positive number"
                        }
                    },
                ) {
                    Text("Save")
                }
            }
        }
    }
}
