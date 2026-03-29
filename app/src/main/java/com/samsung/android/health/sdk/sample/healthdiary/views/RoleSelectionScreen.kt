package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UserRole
import androidx.compose.material.icons.filled.Groups

/**
 * Role selection screen for Digital Sanctuary onboarding.
 * Users choose between Caregiver (monitor) or Patient (monitored) roles.
 */
@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit,
    onHelp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background Aesthetic Elements
        BackgroundDecorations()
        
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Brand Identity
            BrandIdentity()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Header Section
            HeaderSection()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Role Cards
            RoleCardsSection(onRoleSelected = onRoleSelected)
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Social Proof & Help
            BottomSection(onHelp = onHelp)
            
            Spacer(modifier = Modifier.height(16.dp))
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
                .alpha(0.2f)
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
        
        // Bottom-right blob
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(350.dp)
                .alpha(0.15f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun BrandIdentity() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.alpha(0.8f)
    ) {
        Icon(
            imageVector = Icons.Default.HealthAndSafety,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Digital Sanctuary",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¿Quién usará este dispositivo?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Selecciona tu rol",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoleCardsSection(onRoleSelected: (UserRole) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        RoleCard(
            title = "Cuidador",
            description = "Monitorea salud y recibe alertas en tiempo real",
            icon = Icons.Default.Groups,
            iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = { onRoleSelected(UserRole.CAREGIVER) }
        )
        
        RoleCard(
            title = "Persona a cuidar",
            description = "Comparte tus signos vitales automáticamente",
            icon = Icons.Default.HealthAndSafety,
            iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = { onRoleSelected(UserRole.PATIENT) }
        )
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "card_scale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isHovered) 8f else 2f,
        animationSpec = tween(durationMillis = 200),
        label = "card_elevation"
    )
    
    Card(
        onClick = {
            isHovered = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation.dp,
            pressedElevation = 12.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = iconBackgroundColor,
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Indicator
            Surface(
                shape = CircleShape,
                color = if (isHovered) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Continuar",
                        tint = if (isHovered) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomSection(onHelp: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Social Proof Badge
        SocialProofBadge()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Help Button
        TextButton(
            onClick = onHelp,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Necesito ayuda con la configuración",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.HelpOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Footer
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "SEGURIDAD · PRIVACIDAD · CONFIANZA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SocialProofBadge() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp)
        ) {
            // Avatar stack placeholder (using colored circles)
            Row(
                modifier = Modifier.padding(end = 16.dp)
            ) {
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary
                ).forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .offset(x = (-8 * index).dp)
                            .background(color, CircleShape)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                }
            }
            
            Text(
                text = "+500 familias ya usan Sanctuary",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
