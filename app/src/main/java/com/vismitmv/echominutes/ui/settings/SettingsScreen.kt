package com.vismitmv.echominutes.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showGeminiKey by remember { mutableStateOf(false) }
    var showSyncKey by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ----------------- GEMINI API SECTION ----------------- //
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Google Gemini API",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Stored securely on-device with AES-256 GCM encryption. Used directly to transcribe & summarize audio with Gemini 3.6 Flash.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = state.apiKey,
                onValueChange = viewModel::onApiKeyChanged,
                label = { Text("Gemini API Key") },
                placeholder = { Text("AIza…") },
                singleLine = true,
                visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                        Icon(
                            if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showGeminiKey) "Hide key" else "Show key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ----------------- CLOUDFLARE SYNC SECTION ----------------- //
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Self-Hosted Cloud Sync",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Automatically syncs audio files, transcripts, and summaries to your self-hosted backend exposed via Cloudflare Tunnel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChanged,
                label = { Text("Cloudflare Server URL") },
                placeholder = { Text("https://echominutes.vismitmv.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.syncApiKey,
                onValueChange = viewModel::onSyncApiKeyChanged,
                label = { Text("Sync API Key") },
                placeholder = { Text("Enter your backend sync key") },
                singleLine = true,
                visualTransformation = if (showSyncKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { showSyncKey = !showSyncKey }) {
                        Icon(
                            if (showSyncKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showSyncKey) "Hide key" else "Show key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Auto-Sync Recordings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Upload in background using WorkManager",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.autoSyncEnabled,
                    onCheckedChange = viewModel::onAutoSyncToggled
                )
            }

            // Sync Now button
            OutlinedButton(
                onClick = viewModel::syncNow,
                enabled = !state.isSyncingNow && state.syncApiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isSyncingNow) "Sync Enqueued..." else "Sync Unsynced Meetings Now")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Save button
            Button(
                onClick = viewModel::saveAllSettings,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.saved) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Settings Saved!")
                } else {
                    Text("Save All Settings")
                }
            }

            // About section
            Text(
                "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "EchoMinutes uses Gemini 3.6 Flash to transcribe and summarize meetings, with automatic background sync to your private Cloudflare backend.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Version 1.1.0-beta • com.vismitmv.echominutes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
