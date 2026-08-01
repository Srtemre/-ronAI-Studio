package com.htmltoapk.studio.ui.editor

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htmltoapk.studio.R
import com.htmltoapk.studio.core.result.Result
import com.htmltoapk.studio.ui.components.RoundedCard
import com.htmltoapk.studio.ui.components.SectionHeader

@Composable
fun EditorScreen(
    expertMode: Boolean,
    projectId: Long?,
    onClose: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.saveResult, state.generationResult, state.buildResult, state.validationErrorRes) {
        state.validationErrorRes?.let { resId ->
            snackbar.showSnackbar(androidx.compose.ui.res.stringResource(resId))
            viewModel.consumeResults()
        }
        when (val r = state.saveResult) {
            is Result.Success -> { viewModel.consumeResults() }
            is Result.Failure -> { snackbar.showSnackbar("Save failed: ${r.error.message}"); viewModel.consumeResults() }
            Result.Loading -> {}
            null -> {}
        }
        when (val r = state.generationResult) {
            is Result.Success -> { snackbar.showSnackbar("Project generated at:\n${r.value}"); viewModel.consumeResults() }
            is Result.Failure -> { snackbar.showSnackbar("Generation failed: ${r.error.message}"); viewModel.consumeResults() }
            Result.Loading -> {}
            null -> {}
        }
        when (val r = state.buildResult) {
            is Result.Success -> { snackbar.showSnackbar("APK built: ${r.value}"); viewModel.consumeResults() }
            is Result.Failure -> { snackbar.showSnackbar("Build failed: ${r.error.message}"); viewModel.consumeResults() }
            Result.Loading -> {}
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (expertMode) stringResource(R.string.editor_expert_mode)
                        else stringResource(R.string.editor_fast_mode),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.generationResult == Result.Loading || state.buildResult == Result.Loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                val working = state.generationResult == Result.Loading || state.buildResult == Result.Loading

                BasicSection(viewModel, state, enabled = !working)
                SourceSection(viewModel, state, enabled = !working)

                if (expertMode) {
                    ExpertSections(viewModel, state, enabled = !working)
                }

                Spacer(Modifier.height(8.dp))
                ActionRow(
                    onSave = { viewModel.save(onSaved) },
                    onGenerate = viewModel::generate,
                    onBuild = viewModel::buildApk,
                    isWorking = working
                )
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun ActionRow(
    onSave: () -> Unit,
    onGenerate: () -> Unit,
    onBuild: () -> Unit,
    isWorking: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            enabled = !isWorking
        ) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text(stringResource(R.string.action_save))
        }
        Button(
            onClick = onGenerate,
            modifier = Modifier.weight(1f),
            enabled = !isWorking
        ) {
            Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text(stringResource(R.string.action_generate))
        }
        Button(
            onClick = onBuild,
            modifier = Modifier.weight(1f),
            enabled = !isWorking
        ) {
            if (isWorking) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.size(4.dp))
            Text(stringResource(R.string.action_build_apk))
        }
    }
}
