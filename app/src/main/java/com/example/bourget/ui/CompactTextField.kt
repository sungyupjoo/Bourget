package com.example.bourget.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * A field drawn as text on a single bottom line (no box), 44dp tall with body-size text.
 * Single-line unless [minLines] > 1. A trailing icon must fit that height (no 48dp IconButton).
 */
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    minLines: Int = 1,
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = 44.dp),
        textStyle = fieldTextStyle(isError),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = minLines == 1,
        minLines = minLines,
        interactionSource = interactionSource,
    ) { innerTextField ->
        Decoration(value, innerTextField, minLines == 1, interactionSource, isError, placeholder, supportingText, trailingIcon)
    }
}

/** The same field with a [TextFieldValue], for callers that need the cursor position. */
@Composable
fun CompactTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    minLines: Int = 1,
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = 44.dp),
        textStyle = fieldTextStyle(isError),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        singleLine = minLines == 1,
        minLines = minLines,
        interactionSource = interactionSource,
    ) { innerTextField ->
        Decoration(value.text, innerTextField, minLines == 1, interactionSource, isError, placeholder, supportingText, null)
    }
}

@Composable
private fun fieldTextStyle(isError: Boolean): TextStyle = MaterialTheme.typography.bodyLarge.copy(
    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Decoration(
    text: String,
    innerTextField: @Composable () -> Unit,
    singleLine: Boolean,
    interactionSource: MutableInteractionSource,
    isError: Boolean,
    placeholder: String,
    supportingText: String?,
    trailingIcon: (@Composable () -> Unit)?,
) {
    TextFieldDefaults.DecorationBox(
        value = text,
        innerTextField = innerTextField,
        enabled = true,
        singleLine = singleLine,
        visualTransformation = VisualTransformation.None,
        interactionSource = interactionSource,
        isError = isError,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge) },
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
        ),
        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 10.dp, bottom = 10.dp),
    )
}
