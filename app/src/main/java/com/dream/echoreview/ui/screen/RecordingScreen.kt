package com.dream.echoreview.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import com.dream.echoreview.domain.repository.RecordingState
import com.dream.echoreview.ui.component.FluidWaveVisualizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onBack: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val state by viewModel.recordingState.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val duration by viewModel.formattedDuration.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val realtimeText by viewModel.realtimeTranscript.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建面试录音") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = companyName,
                onValueChange = { viewModel.updateCompanyName(it) },
                label = { Text("公司名称") },
                modifier = Modifier.fillMaxWidth(),
                enabled = state == RecordingState.IDLE
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (state == RecordingState.RECORDING) {
                Text(duration, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                FluidWaveVisualizer(
                    amplitude = amplitude,
                    modifier = Modifier.height(100.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 实时转写文字展示区
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    // 1. 记住滚动状态
                    val scrollState = rememberScrollState()

                    // 2. 监听文本变化，一旦有新字进来，自动平滑滚到底部
                    LaunchedEffect(realtimeText) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }

                    Text(
                        text = realtimeText.ifEmpty { "正在监听您的发言..." },
                        modifier = Modifier.padding(16.dp)
                            .verticalScroll(scrollState),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text("AI 正在深度倾听中...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.stopRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("停止并保存")
                }
            } else {
                Button(
                    onClick = { viewModel.startRecording() },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("开始录音")
                }
            }
        }
    }
}
