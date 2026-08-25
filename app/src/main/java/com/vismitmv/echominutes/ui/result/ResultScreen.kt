package com.vismitmv.echominutes.ui.result

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    meetingId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ResultViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(meetingId) { viewModel.loadMeeting(meetingId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.meeting?.title ?: "Meeting",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.meeting != null) {
                        IconButton(onClick = {
                            val text = buildString {
                                append("## Summary\n\n")
                                append(state.meeting!!.summary)
                                append("\n\n## Transcript\n\n")
                                append(state.meeting!!.transcript)
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, "Meeting Notes: ${state.meeting!!.title}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Meeting Notes"))
                        }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            state.meeting != null -> {
                val meeting = state.meeting!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Duration chip
                    val durationStr = formatDuration(meeting.durationSeconds)
                    AssistChip(
                        onClick = {},
                        label = { Text("Duration: $durationStr") }
                    )

                    // Summary Card
                    SectionCard(title = "✦ Summary") {
                        if (meeting.summary.isNotBlank()) {
                            Text(
                                text = meeting.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                            )
                        } else {
                            Text(
                                "No summary available.",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Transcript Card
                    SectionCard(title = "📝 Transcript") {
                        if (meeting.transcript.isNotBlank()) {
                            Text(
                                text = meeting.transcript,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "No transcript available.",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
