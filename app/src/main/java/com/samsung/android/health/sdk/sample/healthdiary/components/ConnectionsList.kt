package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.ConnectionInfo

/**
 * List of active family connections with add button.
 */
@Composable
fun ConnectionsList(
    connections: List<ConnectionInfo>,
    onConnectionClick: (ConnectionInfo) -> Unit = {},
    onAddConnectionClick: () -> Unit = {},
    onManageClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Conexiones Activas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SandboxOnSurface
            )
            
            TextButton(onClick = onManageClick) {
                Text(
                    text = "Gestionar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SandboxPrimary
                )
            }
        }
        
        // Connections list
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (connections.isEmpty()) {
                // Empty state
                SandboxEmptyState(
                    title = "Sin conexiones",
                    message = "Aún no has vinculado ningún familiar",
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                connections.forEach { connection ->
                    ConnectionItem(
                        connection = connection,
                        onClick = { onConnectionClick(connection) }
                    )
                }
            }
            
            // Add connection button
            OutlinedCard(
                onClick = onAddConnectionClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    width = 2.dp,
                    color = SandboxOutlineVariant
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                        tint = SandboxOnSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Vincular Familiar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SandboxOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionItem(
    connection: ConnectionInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SandboxSurfaceContainerLowest
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SandboxSecondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = SandboxSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = connection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SandboxOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = connection.relationship,
                    style = MaterialTheme.typography.bodySmall,
                    color = SandboxOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Chevron
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Ver más",
                tint = SandboxOutlineVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectionsListPreview() {
    SandboxTheme {
        ConnectionsList(
            connections = listOf(
                ConnectionInfo(
                    userId = "1",
                    name = "Marta García",
                    relationship = "Madre • Monitoreo Activo",
                    isActive = true
                ),
                ConnectionInfo(
                    userId = "2",
                    name = "Roberto Méndez",
                    relationship = "Padre • Monitoreo de Signos",
                    isActive = true
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
