package com.dream.echoreview.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val savedApiKey by viewModel.apiKey.collectAsState()
    var apiKeyInput by remember { mutableStateOf("") }

    LaunchedEffect(savedApiKey) {
        apiKeyInput = savedApiKey ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
                .padding(16.dp)
        ) {
            Text("API 配置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("DashScope API Key") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sk-...") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.updateApiKey(apiKeyInput) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存设置")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "请在阿里云 DashScope 控制台获取 API Key",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
