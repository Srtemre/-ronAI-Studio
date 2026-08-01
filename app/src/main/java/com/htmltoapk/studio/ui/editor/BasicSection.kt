package com.htmltoapk.studio.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.htmltoapk.studio.R
import com.htmltoapk.studio.data.model.Orientation
import com.htmltoapk.studio.data.model.SourceType
import com.htmltoapk.studio.ui.components.ChoiceChips
import com.htmltoapk.studio.ui.components.LabeledTextField
import com.htmltoapk.studio.ui.components.RoundedCard
import com.htmltoapk.studio.ui.components.SectionHeader

@Composable
fun BasicSection(viewModel: EditorViewModel, state: EditorUiState, enabled: Boolean = true) {
    SectionHeader(stringResource(R.string.editor_section_basic))
    RoundedCard {
        LabeledTextField(
            label = stringResource(R.string.field_app_name),
            value = state.appName,
            onValueChange = { v -> viewModel.update { it.copy(appName = v) } },
            placeholder = stringResource(R.string.placeholder_app_name),
            isError = state.appName.isBlank() && state.validationErrorRes == R.string.error_app_name_required,
            errorMessage = if (state.appName.isBlank() && state.validationErrorRes == R.string.error_app_name_required)
                stringResource(R.string.error_app_name_required) else null,
            enabled = enabled
        )

        LabeledTextField(
            label = stringResource(R.string.field_package),
            value = state.packageName,
            onValueChange = { v -> viewModel.update { it.copy(packageName = v) } },
            placeholder = stringResource(R.string.placeholder_package),
            isError = state.packageName.isNotBlank() && state.validationErrorRes == R.string.error_package_invalid,
            errorMessage = if (state.packageName.isNotBlank() && state.validationErrorRes == R.string.error_package_invalid)
                stringResource(R.string.error_package_invalid) else null,
            enabled = enabled
        )

        LabeledTextField(
            label = stringResource(R.string.field_version),
            value = state.version,
            onValueChange = { v -> viewModel.update { it.copy(version = v) } },
            placeholder = stringResource(R.string.placeholder_version),
            enabled = enabled
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.field_orientation), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(
                options = listOf(
                    Orientation.AUTO to stringResource(R.string.orientation_auto),
                    Orientation.PORTRAIT to stringResource(R.string.orientation_portrait),
                    Orientation.LANDSCAPE to stringResource(R.string.orientation_landscape)
                ),
                selected = state.orientation,
                onSelect = { o -> viewModel.update { it.copy(orientation = o) } }
            )
        }

        LogoPicker(
            label = stringResource(R.string.field_logo),
            currentUri = state.logoUri,
            onPicked = viewModel::setLogoUri,
            pickLabel = stringResource(R.string.action_pick_logo),
            enabled = enabled
        )
        LogoPicker(
            label = stringResource(R.string.field_splash),
            currentUri = state.splashUri,
            onPicked = viewModel::setSplashUri,
            pickLabel = stringResource(R.string.action_pick_splash),
            enabled = enabled
        )
    }
}

@Composable
private fun LogoPicker(
    label: String,
    currentUri: String,
    onPicked: (android.net.Uri?) -> Unit,
    pickLabel: String,
    enabled: Boolean
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> onPicked(uri) }
    LabeledTextField(
        label = label,
        value = if (currentUri.isBlank()) "(not set)" else currentUri,
        onValueChange = {},
        placeholder = "",
        leadingIcon = Icons.Filled.Image,
        trailing = {
            androidx.compose.material3.TextButton(
                onClick = { launcher.launch(arrayOf("image/*")) },
                enabled = enabled
            ) {
                Text(pickLabel)
            }
        },
        enabled = false
    )
}
