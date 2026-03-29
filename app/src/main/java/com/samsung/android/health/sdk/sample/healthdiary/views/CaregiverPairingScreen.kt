package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.PairingUiState
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.PairingViewModel

/**
 * Caregiver pairing screen - Enter 6-digit code from patient.
 * 
 * Features:
 * - 6 digit input boxes with cursor animation
 * - 3x4 numeric keypad
 * - Auto-validation when 6 digits entered
 * - Loading and error states
 */
@Composable
fun CaregiverPairingScreen(
    onPairingSuccess: (String, String, String) -> Unit, // pairingId, patientId, patientName
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: PairingViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PairingViewModel(context) as T
            }
        }
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val codeInput by viewModel.codeInput.collectAsState()
    
    // Handle success state
    LaunchedEffect(uiState) {
        if (uiState is PairingUiState.PairingSuccess) {
            val successState = uiState as PairingUiState.PairingSuccess
            onPairingSuccess(
                successState.pairingId,
                successState.linkedUserId,
                successState.linkedUserName
            )
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background decorations
        BackgroundDecorations()
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            TopBar(onBack = onBack)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            HeaderSection()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Code input boxes
            CodeInputBoxes(
                code = codeInput,
                isValidating = uiState is PairingUiState.CodeValidating
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error message
            AnimatedVisibility(
                visible = uiState is PairingUiState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (uiState is PairingUiState.Error) {
                    ErrorMessage((uiState as PairingUiState.Error).message)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Numeric keypad
            NumericKeypad(
                onNumberClick = { digit ->
                    if (codeInput.length < 6 && uiState !is PairingUiState.CodeValidating) {
                        viewModel.updateCodeInput(codeInput + digit)
                    }
                },
                onBackspace = {
                    if (uiState !is PairingUiState.CodeValidating) {
                        viewModel.deleteLastDigit()
                    }
                },
                enabled = uiState !is PairingUiState.CodeValidating
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Loading overlay
        if (uiState is PairingUiState.CodeValidating || uiState is PairingUiState.Loading) {
            LoadingOverlay()
        }
    }
}

@Composable
private fun BackgroundDecorations() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top-left blob
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-100).dp)
                .size(400.dp)
                .alpha(0.15f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Regresar",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Vincular familiar",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Ingresa el código de 6 dígitos que aparece en el dispositivo de la persona a cuidar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun CodeInputBoxes(
    code: String,
    isValidating: Boolean
) {
    var cursorVisible by remember { mutableStateOf(true) }
    
    // Cursor blink animation
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            cursorVisible = !cursorVisible
        }
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 2.dp,
                        color = when {
                            isValidating -> MaterialTheme.colorScheme.primary
                            index == code.length -> MaterialTheme.colorScheme.primary
                            index < code.length -> MaterialTheme.colorScheme.tertiary
                            else -> Color.Transparent
                        },
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (index < code.length) {
                    // Show digit
                    Text(
                        text = code[index].toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else if (index == code.length && cursorVisible && !isValidating) {
                    // Show cursor
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit,
    enabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Row 1: 1, 2, 3
        KeypadRow(
            numbers = listOf("1", "2", "3"),
            onNumberClick = onNumberClick,
            enabled = enabled
        )
        
        // Row 2: 4, 5, 6
        KeypadRow(
            numbers = listOf("4", "5", "6"),
            onNumberClick = onNumberClick,
            enabled = enabled
        )
        
        // Row 3: 7, 8, 9
        KeypadRow(
            numbers = listOf("7", "8", "9"),
            onNumberClick = onNumberClick,
            enabled = enabled
        )
        
        // Row 4: backspace, 0, empty
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Backspace button
            KeypadButton(
                content = {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Borrar",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                },
                onClick = onBackspace,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            
            // 0 button
            KeypadButton(
                content = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = { onNumberClick("0") },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            
            // Empty space for symmetry
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeypadRow(
    numbers: List<String>,
    onNumberClick: (String) -> Unit,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        numbers.forEach { number ->
            KeypadButton(
                content = {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = { onNumberClick(number) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KeypadButton(
    content: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Validando código...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
