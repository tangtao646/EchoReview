package com.dream.echoreview.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dream.echoreview.R
import com.dream.echoreview.domain.model.InterviewSession
import java.text.SimpleDateFormat
import java.util.*


@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRecording: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val totalDuration by viewModel.totalDurationText.collectAsState()
    val completionRate by viewModel.completionRate.collectAsState()
    var sessionToDelete by remember { mutableStateOf<InterviewSession?>(null) }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = {
                Text(
                    stringResource(
                        R.string.confirm_delete_message,
                        sessionToDelete?.companyName?.ifEmpty { stringResource(R.string.unnamed_company) } ?: ""
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionToDelete?.let { viewModel.deleteSession(it.id) }
                        sessionToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToRecording,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    Icons.Default.MicNone,
                    contentDescription = stringResource(R.string.start_recording),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                stringResource(R.string.overview),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            BentoDashboard(
                sessionCount = sessions.size,
                totalDuration = totalDuration,
                completionRate = completionRate
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.recent_records),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToHistory, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.view_all), style = MaterialTheme.typography.labelMedium)
                }
            }

            if (sessions.isEmpty()) {
                EmptyState(onStart = onNavigateToRecording)
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 80.dp) // 留出 FAB 空间
                ) {
                    val availableHeight = maxHeight
                    val itemHeight = 76.dp // ModernSessionItem 大约 76dp 高度

                    val displayCount = (availableHeight / itemHeight).toInt().coerceAtLeast(1)
                    val displaySessions = sessions.take(displayCount)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        displaySessions.forEach { session ->
                            ModernSessionItem(
                                session = session,
                                onClick = { onNavigateToDetail(session.id) },
                                onDelete = { sessionToDelete = session }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoDashboard(
    sessionCount: Int,
    totalDuration: String,
    completionRate: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 主卡片：复盘进度
        Surface(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${(completionRate * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        stringResource(R.string.review_completion_rate),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // 右侧堆叠卡片
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 累计面试
            BentoSmallCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.History,
                label = stringResource(R.string.total_interviews),
                value = "$sessionCount",
                containerColor = Color(0xFFFFE0B2)
            )
            // 录音时长
            BentoSmallCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MicNone,
                label = stringResource(R.string.total_duration),
                value = totalDuration.replace(" 分钟", "m"),
                containerColor = Color(0xFFE1F5FE)
            )
        }
    }
}

@Composable
fun BentoSmallCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: Color
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Row {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black.copy(alpha = 0.3f)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(alpha = 0.5f),
                    maxLines = 1,
                    fontSize = 10.sp
                )
            }

            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )


        }
    }
}

@Composable
fun ModernSessionItem(
    session: InterviewSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 企业图标占位符
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFEDEDED), Color(0xFFF5F5F5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = session.companyName.take(1).ifEmpty { "?" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.companyName.ifEmpty { stringResource(R.string.unnamed_company) },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = session.interviewStage,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SimpleDateFormat(stringResource(R.string.date_format), Locale.getDefault()).format(
                            Date(
                                session.timestamp
                            )
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (tagText, tagColor) = if (!session.aiSummary.isNullOrEmpty()) {
                        stringResource(R.string.reviewed) to Color(0xFF4CAF50)
                    } else {
                        stringResource(R.string.pending) to Color(0xFFFF9800)
                    }

                    Text(
                        text = tagText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = tagColor,
                        modifier = Modifier
                            .background(tagColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )

                    if (session.durationMillis > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.MicNone,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.Gray.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = formatDuration(session.durationMillis),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = (ms / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun EmptyState(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color(0xFFF0F2F5)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.MicNone,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.start_first_review),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.click_to_start_recording),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.start_recording))
        }
    }
}
