package com.dream.echoreview.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dream.echoreview.domain.repository.RecordingState
import com.dream.echoreview.ui.component.FluidWaveVisualizer
import kotlinx.coroutines.delay

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
                    val scrollState = rememberScrollState()
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
                
                Spacer(modifier = Modifier.height(32.dp))

                // 长按结束录音按钮
                LongPressStopButton(
                    onLongPressComplete = { viewModel.stopRecording() }
                )
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

@Composable
fun LongPressStopButton(
    onLongPressComplete: () -> Unit,
    targetDurationMs: Long = 2000L
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 进度动画状态 (0f 到 1f)
    var progress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (isPressed) tween(targetDurationMs.toInt(), easing = LinearEasing) else tween(300),
        label = "progress"
    )

    // 逻辑计时器：当按下时启动协程
    LaunchedEffect(isPressed) {
        if (isPressed) {
            progress = 1f
            val startTime = System.currentTimeMillis()
            // 循环检查直到达到目标时长或用户松开
            while (System.currentTimeMillis() - startTime < targetDurationMs) {
                delay(10)
            }
            // 时间到了且依然处于按下状态
            onLongPressComplete()
        } else {
            progress = 0f
        }
    }

    Box(contentAlignment = Alignment.Center) {
        // 背景进度环
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(110.dp),
            color = MaterialTheme.colorScheme.error,
            strokeWidth = 6.dp,
            trackColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        )

        // 圆形停止按钮
        Surface(
            onClick = { /* 拦截点击，防止误触，交互全靠长按 */ },
            interactionSource = interactionSource,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(90.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "长按结束",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
