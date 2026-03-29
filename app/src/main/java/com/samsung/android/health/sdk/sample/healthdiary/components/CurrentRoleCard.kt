package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.UserRole
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Current role card displaying user's role (Caregiver/Patient) and protected person info.
 */
@Composable
fun CurrentRoleCard(
    role: UserRole,
    protectedPersonName: String? = null,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SandboxSurfaceContainerLowest
        ),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Badge + Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Role Badge
                    Surface(
                        color = SandboxPrimaryFixed,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = "ROL ACTUAL",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SandboxPrimaryDark,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    // Role Title
                    Text(
                        text = when (role) {
                            UserRole.CAREGIVER -> "Cuidador Principal"
                            UserRole.PATIENT -> "Familiar Protegido"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SandboxPrimary
                    )
                }
                
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SandboxPrimary.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolunteerActivism,
                        contentDescription = null,
                        tint = SandboxPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Protected person info (only for caregivers)
            if (role == UserRole.CAREGIVER && protectedPersonName != null) {
                Surface(
                    color = SandboxSurfaceContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar placeholder
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SandboxSecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = protectedPersonName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SandboxOnSecondaryContainer
                            )
                        }
                        
                        // Info
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = protectedPersonName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SandboxOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Familiar Protegido • Activa ahora",
                                style = MaterialTheme.typography.bodySmall,
                                color = SandboxOnSurfaceVariant
                            )
                        }
                        
                        // Status indicator with pulse animation
                        if (isActive) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.3f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse_scale"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E)) // green-500
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentRoleCardPreview() {
    SandboxTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CurrentRoleCard(
                role = UserRole.CAREGIVER,
                protectedPersonName = "Marta García",
                isActive = true
            )
            
            CurrentRoleCard(
                role = UserRole.PATIENT,
                protectedPersonName = null,
                isActive = false
            )
        }
    }
}
