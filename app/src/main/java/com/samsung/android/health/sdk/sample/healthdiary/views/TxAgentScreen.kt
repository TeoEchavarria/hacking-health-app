package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.Message
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.TxAgentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TxAgentScreen(
    onNavigateBack: () -> Unit,
    viewModel: TxAgentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            SandboxTopBar(
                title = "TxAgent AI Assistant",
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.messages) { message ->
                    SandboxChatBubble(
                        text = message.text,
                        isUser = message.isUser
                    )
                }
                
                if (uiState.isLoading) {
                    item {
                        SandboxLoader(
                            variant = LoaderVariant.Medium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
                
                if (uiState.error != null) {
                    item {
                        SandboxBadge(
                            text = "Error: ${uiState.error}",
                            variant = BadgeVariant.Error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SandboxInput(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "Ask a health question...",
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                SandboxIconButton(
                    icon = Icons.Default.Send,
                    onClick = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank() && !uiState.isLoading,
                    contentDescription = "Send",
                    variant = IconButtonVariant.Filled
                )
            }
        }
    }
}

