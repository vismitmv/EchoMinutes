package com.vismitmv.echominutes.ui.record

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vismitmv.echominutes.theme.CoralRecord
import com.vismitmv.echominutes.theme.IndigoPrimary

@Composable
fun RecordScreen(
    onNavigateToResult: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: RecordViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh API key status when screen is shown
    LaunchedEffect(Unit) { viewModel.refreshApiKeyStatus() }

    // Handle navigation to result
    LaunchedEffect(state.navigateToResult) {
        state.navigateToResult?.let { id ->
            viewModel.clearNavigation()
            onNavigateToResult(id)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startRecording(context)
    }

    // Pulse animation for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "EchoMinutes",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Meeting Recorder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Center: Recording Button Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 6.dp
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Transcribing with Gemini AI…",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "This may take a moment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Pulse ring
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(CoralRecord.copy(alpha = pulseAlpha))
                            )
                        }

                        // Main button
                        val buttonColor = if (state.isRecording) CoralRecord else IndigoPrimary
                        Button(
                            onClick = {
                                if (state.isRecording) {
                                    viewModel.stopRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                            enabled = state.hasApiKey
                        ) {
                            Icon(
                                imageVector = if (state.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (state.isRecording) "Stop Recording" else "Start Recording",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Timer or label
                    if (state.isRecording) {
                        val h = state.elapsedSeconds / 3600
                        val m = (state.elapsedSeconds % 3600) / 60
                        val s = state.elapsedSeconds % 60
                        val timeStr = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = MaterialTheme.typography.headlineLarge.fontSize
                            ),
                            color = CoralRecord
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Recording… tap to stop",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = if (state.hasApiKey) "Tap to start recording" else "Set your API key in Settings first",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.hasApiKey) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        if (!state.hasApiKey) {
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = onNavigateToSettings) {
                                Text("Go to Settings")
                            }
                        }
                    }
                }

                // Error message
                state.error?.let { error ->
                    Spacer(Modifier.height(24.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                }
            }

            // Bottom hint
            if (!state.isRecording && !state.isProcessing) {
                Text(
                    "Supports English, Hindi & Indian languages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
