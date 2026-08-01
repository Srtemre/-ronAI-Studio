package com.htmltoapk.studio.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.htmltoapk.studio.R
import com.htmltoapk.studio.data.model.SourceType
import com.htmltoapk.studio.ui.components.ChoiceChips
import com.htmltoapk.studio.ui.components.LabeledTextField
import com.htmltoapk.studio.ui.components.RoundedCard
import com.htmltoapk.studio.ui.components.SectionHeader

@Composable
fun SourceSection(viewModel: EditorViewModel, state: EditorUiState, enabled: Boolean = true) {
    SectionHeader(stringResource(R.string.editor_section_source))
    RoundedCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.field_source), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(
                options = listOf(
                    SourceType.HTML_FILE to stringResource(R.string.source_html_file),
                    SourceType.ZIP to stringResource(R.string.source_zip),
                    SourceType.FOLDER to stringResource(R.string.source_folder),
                    SourceType.URL to stringResource(R.string.source_website_url),
                    SourceType.PASTE_HTML to stringResource(R.string.source_paste_html)
                ),
                selected = state.sourceType,
                onSelect = { t -> viewModel.update { it.copy(sourceType = t) } }
            )
        }

        when (state.sourceType) {
            SourceType.HTML_FILE -> FilePickerRow(
                label = stringResource(R.string.action_pick_html_file),
                value = state.sourceUri,
                mimeTypes = arrayOf("text/html", "*/*"),
                onPicked = viewModel::setSourceUri
            )
            SourceType.ZIP -> FilePickerRow(
                label = stringResource(R.string.action_pick_zip),
                value = state.sourceUri,
                mimeTypes = arrayOf("application/zip", "*/*"),
                onPicked = viewModel::setSourceUri
            )
            SourceType.FOLDER -> FolderPickerRow(
                label = stringResource(R.string.action_pick_folder),
                value = state.sourceUri,
                onPicked = viewModel::setSourceUri
            )
            SourceType.URL -> LabeledTextField(
                label = stringResource(R.string.source_website_url),
                value = state.websiteUrl,
                onValueChange = { v -> viewModel.update { it.copy(websiteUrl = v) } },
                placeholder = "https://example.com"
            )
            SourceType.PASTE_HTML -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.source_paste_html), style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = state.pastedHtml,
                    onValueChange = { v -> viewModel.update { it.copy(pastedHtml = v) } },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    minLines = 6,
                    maxLines = 12,
                    shape = MaterialTheme.shapes.small
                )
            }
        }
    }
}

@Composable
private fun FilePickerRow(
    label: String,
    value: String,
    mimeTypes: Array<String>,
    onPicked: (android.net.Uri?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onPicked(uri)
    }
    LabeledTextField(
        label = label,
        value = if (value.isBlank()) "(not selected)" else value,
        onValueChange = {},
        trailing = {
            TextButton(onClick = { launcher.launch(mimeTypes) }) { Text(label) }
        },
        enabled = false
    )
}

@Composable
private fun FolderPickerRow(
    label: String,
    value: String,
    onPicked: (android.net.Uri?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> onPicked(uri) }
    LabeledTextField(
        label = label,
        value = if (value.isBlank()) "(not selected)" else value,
        onValueChange = {},
        trailing = {
            TextButton(onClick = { launcher.launch(null) }) { Text(label) }
        },
        enabled = false
    )
}
