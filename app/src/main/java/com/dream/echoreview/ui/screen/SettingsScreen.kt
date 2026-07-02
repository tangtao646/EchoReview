package com.dream.echoreview.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val dashScopeKey by viewModel.dashScopeApiKey.collectAsState()
    val deepSeekKey by viewModel.deepSeekApiKey.collectAsState()
    val geminiKey by viewModel.geminiApiKey.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    var dashScopeInput by remember { mutableStateOf("") }
    var deepSeekInput by remember { mutableStateOf("") }
    var geminiInput by remember { mutableStateOf("") }

    LaunchedEffect(dashScopeKey) { dashScopeInput = dashScopeKey ?: "" }
    LaunchedEffect(deepSeekKey) { deepSeekInput = deepSeekKey ?: "" }
    LaunchedEffect(geminiKey) { geminiInput = geminiKey ?: "" }

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
                .verticalScroll(rememberScrollState())
        ) {
            Text("模型选择 (用于复盘总结)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            val models = listOf("DeepSeek", "Gemini")
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("当前模型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                viewModel.updateSelectedModel(model)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("API Key 配置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // DashScope
            OutlinedTextField(
                value = dashScopeInput,
                onValueChange = { dashScopeInput = it },
                label = { Text("DashScope API Key (用于语音转文字)") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { viewModel.updateDashScopeApiKey(dashScopeInput) }) {
                        Text("保存")
                    }
                }
            )
            Text("用于阿里 Paraformer 语音识别", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // DeepSeek
            OutlinedTextField(
                value = deepSeekInput,
                onValueChange = { deepSeekInput = it },
                label = { Text("DeepSeek API Key") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { viewModel.updateDeepSeekKey(deepSeekInput) }) {
                        Text("保存")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gemini
            OutlinedTextField(
                value = geminiInput,
                onValueChange = { geminiInput = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { viewModel.updateGeminiKey(geminiInput) }) {
                        Text("保存")
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
