package com.dream.echoreview.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dream.echoreview.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val aiSummary by viewModel.aiSummary.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(session?.companyName ?: stringResource(R.string.detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            if (session?.audioPath != null) {
                PlaybackBar(
                    isPlaying = isPlaying,
                    currentPos = currentPos,
                    duration = duration,
                    onTogglePlay = { viewModel.togglePlayback() },
                    onSeek = { viewModel.seekTo(it) }
                )
            }
        }
    ) { padding ->
        session?.let { currentSession ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. 复盘总结标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.review_summary),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    // 当已有内容且不在生成时，显示"重新总结"图标
                    if (aiSummary.isNotBlank() && !isGenerating) {
                        IconButton(onClick = { viewModel.generateAIReview() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.regenerate), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. 总结内容区域
                if (aiSummary.isBlank() && !isGenerating) {
                    // 情况 A: 内容为空且不在生成 -> 显示居中的"开始总结"按钮
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { viewModel.generateAIReview() },
                            modifier = Modifier.height(48.dp),
                            contentPadding = PaddingValues(horizontal = 32.dp)
                        ) {
                            Text(stringResource(R.string.start_summary))
                        }
                    }
                } else {
                    // 情况 B: 正在生成或已有内容 -> 显示文本
                    Text(
                        text = aiSummary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                    )

                    if (isGenerating) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                // 3. 面试转录文本标题
                Text(
                    stringResource(R.string.interview_transcript),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 转录文本内容
                Text(
                    currentSession.transcript ?: stringResource(R.string.no_transcript),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(120.dp)) // 底部预留空间防止被播放栏遮挡
            }
        }
    }
}

@Composable
fun PlaybackBar(
    isPlaying: Boolean,
    currentPos: Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Slider(
                value = currentPos.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatTime(currentPos), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(formatTime(duration.coerceAtLeast(0L)), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
