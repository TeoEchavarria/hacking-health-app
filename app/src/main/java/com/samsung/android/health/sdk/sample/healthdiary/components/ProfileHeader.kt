package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Profile header with circular avatar, name, and email.
 * Includes an edit button for profile modification.
 */
@Composable
fun ProfileHeader(
    name: String,
    email: String,
    avatarUrl: String? = null,
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar with gradient border and edit button
        Box(
            modifier = Modifier.size(128.dp),
            contentAlignment = Alignment.Center
        ) {
            // Gradient border
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SandboxPrimary,
                                SandboxPrimaryContainer
                            )
                        )
                    )
                    .padding(4.dp)
            ) {
                // Avatar content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrEmpty()) {
                        // Load profile picture from URL
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Default avatar icon
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Avatar",
                            tint = SandboxPrimary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
            
            // Edit button (floating at bottom-right)
            FilledIconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SandboxPrimaryContainer,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Editar perfil",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        // Name and email
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = SandboxOnSurface
            )
            
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = SandboxOnSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileHeaderPreview() {
    SandboxTheme {
        ProfileHeader(
            name = "Lucía Méndez",
            email = "lucia.mendez@serenecare.com"
        )
    }
}
