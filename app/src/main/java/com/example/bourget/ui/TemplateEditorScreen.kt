package com.example.bourget.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bourget.R
import com.example.bourget.data.DEFAULT_TEMPLATE
import com.example.bourget.data.LaunchableApp
import com.example.bourget.data.NAME_PLACEHOLDER
import com.example.bourget.data.TEMPLATE_EXAMPLES
import com.example.bourget.data.TEMPLATE_MAX_LENGTH
import com.example.bourget.data.TemplateError
import com.example.bourget.data.applyTemplate
import com.example.bourget.data.templateError
import com.example.bourget.data.templateLength

/**
 * Edits the one template every confirm popup uses. The preview shows the popup as it will look.
 * An empty template is reported only on save; other errors as soon as they appear. Leaving
 * with unsaved changes asks first.
 */
@Composable
fun TemplateEditorScreen(
    saved: String,
    exampleApp: LaunchableApp?,
    exampleSite: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(saved, TextRange(saved.length)))
    }
    var showEmptyError by rememberSaveable { mutableStateOf(false) }
    var previewTab by rememberSaveable { mutableIntStateOf(0) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val text = value.text
    val dirty = text != saved
    val error = templateError(text)?.takeIf { it != TemplateError.EMPTY || showEmptyError }
    val tryBack = { if (dirty) confirmDiscard = true else onBack() }
    BackHandler(onBack = tryBack)

    // At large font sizes the preview would crowd out the field, so it scrolls with the rest.
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val preview = @Composable {
        Column(Modifier.padding(horizontal = ScreenMargin), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PreviewTab(stringResource(R.string.template_preview_app), previewTab == 0) { previewTab = 0 }
                PreviewTab(stringResource(R.string.template_preview_website), previewTab == 1) { previewTab = 1 }
            }
            PopupPreview(
                title = applyTemplate(text, if (previewTab == 0) exampleApp?.label ?: "YouTube" else exampleSite),
                app = exampleApp.takeIf { previewTab == 0 },
            )
        }
    }

    Column(modifier) {
        BackBar(onBack = tryBack)
        ScreenTitle(stringResource(R.string.template_title), Modifier.padding(bottom = 16.dp))
        if (!largeFont) preview()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
            if (largeFont) preview()
            CompactTextField(
                value = value,
                onValueChange = {
                    value = it
                    showEmptyError = false
                },
                placeholder = stringResource(R.string.template_input),
                isError = error != null,
                supportingText = error?.let { stringResource(it.message()) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
            )
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = ScreenMargin), verticalAlignment = Alignment.CenterVertically) {
                UnderlineButton(stringResource(R.string.template_insert_name), onClick = {
                    val start = minOf(value.selection.start, value.selection.end)
                    val end = maxOf(value.selection.start, value.selection.end)
                    val inserted = text.replaceRange(start, end, NAME_PLACEHOLDER)
                    value = TextFieldValue(inserted, TextRange(start + NAME_PLACEHOLDER.length))
                })
                Text(
                    stringResource(R.string.template_count, templateLength(text), TEMPLATE_MAX_LENGTH),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (templateLength(text) > TEMPLATE_MAX_LENGTH) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
            Text(
                stringResource(if (NAME_PLACEHOLDER in text || text.isBlank()) R.string.template_hint else R.string.template_no_name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ScreenMargin),
            )
            SectionHeader(stringResource(R.string.template_examples), Modifier.padding(top = 32.dp))
            Column(Modifier.selectableGroup()) {
                TEMPLATE_EXAMPLES.forEachIndexed { i, example ->
                    if (i > 0) RowRule()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(example == text, role = Role.RadioButton) {
                                value = TextFieldValue(example, TextRange(example.length))
                                showEmptyError = false
                            }
                            .padding(horizontal = ScreenMargin, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (example == DEFAULT_TEMPLATE) "$example ${stringResource(R.string.template_default_suffix)}" else example,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (example == text) Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PrimaryButton(stringResource(R.string.template_save), onClick = {
                if (templateError(text) == null) onSave(text) else showEmptyError = true
            })
            // Changes only the field; saving is still a separate step.
            UnderlineButton(
                stringResource(R.string.template_reset),
                onClick = { value = TextFieldValue(DEFAULT_TEMPLATE, TextRange(DEFAULT_TEMPLATE.length)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.template_discard_title)) },
            text = { Text(stringResource(R.string.template_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onBack()
                }) { Text(stringResource(R.string.template_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.template_keep_editing)) }
            },
        )
    }
}

private fun TemplateError.message(): Int = when (this) {
    TemplateError.EMPTY -> R.string.template_error_empty
    TemplateError.TOO_LONG -> R.string.template_error_too_long
    TemplateError.BAD_BRACES -> R.string.template_error_braces
}

@Composable
private fun PreviewTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        textDecoration = if (selected) TextDecoration.Underline else null,
        modifier = Modifier
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

/** The confirm popup in miniature: same colors and type as the real overlay, over a dark app. */
@Composable
private fun PopupPreview(title: String, app: LaunchableApp?) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF141312))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (app != null) {
            AppIcon(app, 28.dp)
        } else {
            Icon(painterResource(R.drawable.ic_link), contentDescription = null, tint = Color(0xFFBDB7AE), modifier = Modifier.size(28.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 30.sp),
            color = Color(0xFFF4F1EC),
        )
    }
}
