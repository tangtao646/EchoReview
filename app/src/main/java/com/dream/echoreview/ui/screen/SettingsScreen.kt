package com.dream.echoreview.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dream.echoreview.R

/**
 * 自定义固定长度遮罩转换器
 * 无论输入多长，隐藏时仅显示固定个数的点
 */
class FixedLengthMaskTransformation(private val maskChar: Char = '•', private val length: Int = 12) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val out = maskChar.toString().repeat(length)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = if (offset > 0) length else 0
            override fun transformedToOriginal(offset: Int): Int = if (offset > 0) text.length else 0
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

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

    var dashScopeVisible by remember { mutableStateOf(false) }
    var deepSeekVisible by remember { mutableStateOf(false) }
    var geminiVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(dashScopeKey) { dashScopeInput = dashScopeKey ?: "" }
    LaunchedEffect(deepSeekKey) { deepSeekInput = deepSeekKey ?: "" }
    LaunchedEffect(geminiKey) { geminiInput = geminiKey ?: "" }

    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
            Text(stringResource(R.string.model_selection), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val models = listOf("DeepSeek", "Gemini", "DashScope")
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.current_model)) },
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

            Text(stringResource(R.string.api_key_config), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.api_key_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

            Spacer(modifier = Modifier.height(16.dp))

            ApiKeyField(
                label = stringResource(R.string.dashscope_api_key_label),
                value = dashScopeInput,
                onValueChange = { dashScopeInput = it },
                isVisible = dashScopeVisible,
                onVisibilityChange = { dashScopeVisible = it },
                onSave = { viewModel.updateDashScopeApiKey(dashScopeInput) },
                helperText = stringResource(R.string.dashscope_api_key_helper)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ApiKeyField(
                label = stringResource(R.string.deepseek_api_key_label),
                value = deepSeekInput,
                onValueChange = { deepSeekInput = it },
                isVisible = deepSeekVisible,
                onVisibilityChange = { deepSeekVisible = it },
                onSave = { viewModel.updateDeepSeekKey(deepSeekInput) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ApiKeyField(
                label = stringResource(R.string.gemini_api_key_label),
                value = geminiInput,
                onValueChange = { geminiInput = it },
                isVisible = geminiVisible,
                onVisibilityChange = { geminiVisible = it },
                onSave = { viewModel.updateGeminiKey(geminiInput) }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    helperText: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            // 使用自定义的固定长度转换器
            visualTransformation = if (isVisible) VisualTransformation.None else FixedLengthMaskTransformation(),
            trailingIcon = {
                Row {
                    IconButton(onClick = { onVisibilityChange(!isVisible) }) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isVisible) stringResource(R.string.hide) else stringResource(R.string.show)
                        )
                    }
                    TextButton(onClick = onSave, enabled = value.isNotBlank()) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        )
        if (helperText != null) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
