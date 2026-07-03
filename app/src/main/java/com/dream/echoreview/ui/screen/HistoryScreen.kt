package com.dream.echoreview.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dream.echoreview.domain.model.InterviewSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsState()
    var sessionToDelete by remember { mutableStateOf<InterviewSession?>(null) }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除与 \"${sessionToDelete?.companyName?.ifEmpty { "未命名公司" }}\" 的面试记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionToDelete?.let { viewModel.deleteSession(it.id) }
                        sessionToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("全部记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                ModernSessionItem(
                    session = session,
                    onClick = { onNavigateToDetail(session.id) },
                    onDelete = { sessionToDelete = session }
                )
            }
        }
    }
}
