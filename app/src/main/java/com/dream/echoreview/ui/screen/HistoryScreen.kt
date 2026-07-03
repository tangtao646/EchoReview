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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dream.echoreview.R
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
                title = { Text(stringResource(R.string.all_records), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
