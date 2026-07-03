package com.dream.echoreview.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dream.echoreview.R
import com.dream.echoreview.domain.repository.RecordingState
import com.dream.echoreview.ui.component.FluidWaveVisualizer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val state by viewModel.recordingState.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val interviewStage by viewModel.interviewStage.collectAsState()
    val duration by viewModel.formattedDuration.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val realtimeText by viewModel.realtimeTranscript.collectAsState()

    val isRecording = state == RecordingState.RECORDING
    var showDiscardDialog by remember { mutableStateOf(false) }
    // 1. 声明控制“密钥缺失”弹窗的状态
    var showApiKeyDialog by remember { mutableStateOf(false) }

    // 2. 核心副作用：监听 ViewModel 的强阻断事件
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is RecordingUiEvent.ShowApiKeyMissingDialog -> {
                    showApiKeyDialog = true
                }
                is RecordingUiEvent.NavigateToDetail->{
                    onNavigateToDetail(event.sessionId)
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_recording_title)) },
            text = { Text(stringResource(R.string.discard_recording_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.discardRecording()
                        showDiscardDialog = false
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.continue_recording))
                }
            }
        )
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("缺少 API 密钥") },
            text = { Text("检测到您的语音转录或 AI 模型的 API Key 未填写，无法开始流式录制。请先前往设置页面完善密钥配置。") },
            confirmButton = {
                Button(
                    onClick = {
                        showApiKeyDialog = false
                        onNavigateToSettings()
                    }
                ) {
                    Text("去设置页填写")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }

    // 动态背景色：录制中采用深色沉浸模式
    val backgroundColor by animateColorAsState(
        targetValue = if (isRecording) Color(0xFF121212) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(1000), label = "bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isRecording) Color.White else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(1000), label = "content"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                showDiscardDialog = true
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!isRecording) {
                Spacer(modifier = Modifier.height(40.dp))
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { viewModel.updateCompanyName(it) },
                    label = { Text(stringResource(R.string.company_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    stringResource(R.string.interview_stage),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val stages = listOf(
                        stringResource(R.string.stage_initial),
                        stringResource(R.string.stage_second),
                        stringResource(R.string.stage_final),
                        stringResource(R.string.stage_hr)
                    )
                    stages.forEach { stage ->
                        FilterChip(
                            selected = interviewStage == stage,
                            onClick = { viewModel.updateInterviewStage(stage) },
                            label = { Text(stage) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { viewModel.startRecording() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        stringResource(R.string.start_interview_recording),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    duration,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                    fontWeight = FontWeight.Light,
                    color = contentColor
                )

                Spacer(modifier = Modifier.height(48.dp))

                FluidWaveVisualizer(
                    amplitude = amplitude,
                    modifier = Modifier.height(150.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // 实时转写文字展示区 - 保持高性能流式渲染
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    color = Color.White.copy(alpha = 0.05f),
                ) {
                    val scrollState = rememberScrollState()

                    // 监听实时文本变化，丝滑滚动到底部
                    LaunchedEffect(realtimeText) {
                        if (realtimeText.isNotEmpty()) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = realtimeText.ifEmpty { stringResource(R.string.listening) },
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                            color = contentColor.copy(alpha = 0.9f)
                        )
                    }
                }



                Spacer(modifier = Modifier.height(40.dp))

                // 长按结束录音按钮
                LongPressStopButton(
                    onLongPressComplete = { viewModel.stopRecording() }
                )
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
        animationSpec = if (isPressed) tween(
            targetDurationMs.toInt(),
            easing = LinearEasing
        ) else tween(300),
        label = "progress"
    )

    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // 逻辑计时器：当按下时启动协程
    LaunchedEffect(isPressed) {
        if (isPressed) {
            progress = 1f
            delay(targetDurationMs)
            onLongPressComplete()
        } else {
            progress = 0f
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(if (isPressed) 1f else pulseScale)
    ) {
        // 背景进度环
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(110.dp),
            color = Color(0xFFEF5350),
            strokeWidth = 4.dp,
            trackColor = Color.White.copy(alpha = 0.1f),
        )

        // 圆形停止按钮
        Surface(
            onClick = { /* 拦截点击 */ },
            interactionSource = interactionSource,
            shape = CircleShape,
            color = Color(0xFFEF5350),
            modifier = Modifier.size(84.dp),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isPressed) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Text(
                        stringResource(R.string.long_press_to_stop),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
