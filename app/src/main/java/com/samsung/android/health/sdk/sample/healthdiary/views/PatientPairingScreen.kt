package com.samsung.android.health.sdk.sample.healthdiary.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * Patient pairing screen - Generate and display 6-digit code.
 * 
 * Features:
 * - Display code in XXX-XXX format
 * - Copy to clipboard button
 * - Regenerate code button
 * - 10-minute countdown timer
 * - Auto-polling for pairing status (every 3 seconds)
 * - Animated "active code" indicator
 */
@Composable
fun PatientPairingScreen(
    onPairingSuccess: (String, String, String) -> Unit, // pairingId, caregiverId, caregiverName
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
    
    // Generate code on first composition
    LaunchedEffect(Unit) {
        if (uiState is PairingUiState.Idle) {
            viewModel.generatePairingCode()
        }
    }
    
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
    
    // Stop polling when screen is destroyed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
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
            
            when (val state = uiState) {
                is PairingUiState.Loading -> {
                    LoadingState()
                }
                is PairingUiState.CodeGenerated -> {
                    CodeGeneratedContent(
                        code = state.code,
                        expiresAt = state.expiresAt,
                        onCopy = { copyToClipboard(context, state.code) },
                        onRegenerate = { viewModel.refreshCode() }
                    )
                }
                is PairingUiState.PairingSuccess -> {
                    SuccessState(state.linkedUserName)
                }
                is PairingUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.generatePairingCode() }
                    )
                }
                else -> {
                    LoadingState()
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BackgroundDecorations() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top-right blob
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-100).dp)
                .size(400.dp)
                .alpha(0.15f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary,
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
            imageVector = Icons.Default.QrCode,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tu código de vinculación",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Comparte este código con tu cuidador para vincular las cuentas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun LoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Generando código...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CodeGeneratedContent(
    code: String,
    expiresAt: Long,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Active status indicator
        ActiveStatusIndicator()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Code card
        CodeCard(code = code)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Timer
        CountdownTimer(expiresAt = expiresAt)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onRegenerate,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Regenerar")
            }
            
            Button(
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copiar")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Info card
        InfoCard()
    }
}

@Composable
private fun ActiveStatusIndicator() {
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .scale(scale)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Código activo",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun CodeCard(code: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Format code as XXX-XXX
            val formattedCode = if (code.length == 6) {
                "${code.substring(0, 3)}-${code.substring(3, 6)}"
            } else {
                code
            }
            
            Text(
                text = formattedCode,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                letterSpacing = 8.sp,
                fontSize = 56.sp
            )
        }
    }
}

@Composable
private fun CountdownTimer(expiresAt: Long) {
    var remainingSeconds by remember { mutableStateOf(getRemainingSeconds(expiresAt)) }
    
    LaunchedEffect(expiresAt) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds = getRemainingSeconds(expiresAt)
        }
    }
    
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (remainingSeconds > 0) {
                "Expira en %02d:%02d".format(minutes, seconds)
            } else {
                "Código expirado"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (remainingSeconds > 0) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Detectaremos automáticamente cuando tu cuidador ingrese el código",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SuccessState(linkedUserName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¡Vinculación exitosa!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Conectado con: $linkedUserName",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

private fun getRemainingSeconds(expiresAt: Long): Int {
    val now = System.currentTimeMillis()
    val remaining = expiresAt - now
    return max(0, (remaining / 1000).toInt())
}

private fun copyToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Código de vinculación", code)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show()
}
