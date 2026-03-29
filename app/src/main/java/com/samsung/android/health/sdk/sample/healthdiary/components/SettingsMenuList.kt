package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*

/**
 * Settings menu item data.
 */
data class SettingMenuItem(
    val title: String,
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color
)

/**
 * Settings menu list with account, notifications, and security options.
 */
@Composable
fun SettingsMenuList(
    onAccountClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSecurityClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val menuItems = listOf(
        SettingMenuItem(
            title = "Configuración de Cuenta",
            icon = Icons.Filled.ManageAccounts,
            iconBackground = Color(0xFFEFF6FF), // blue-50
            iconTint = Color(0xFF2563EB) // blue-600
        ),
        SettingMenuItem(
            title = "Notificaciones",
            icon = Icons.Filled.Notifications,
            iconBackground = Color(0xFFFFF7ED), // orange-50
            iconTint = Color(0xFFEA580C) // orange-600
        ),
        SettingMenuItem(
            title = "Seguridad",
            icon = Icons.Filled.Shield,
            iconBackground = Color(0xFFF0FDF4), // green-50
            iconTint = Color(0xFF16A34A) // green-600
        )
    )
    
    val clickHandlers = listOf(onAccountClick, onNotificationsClick, onSecurityClick)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SandboxSurfaceContainerLowest
        ),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            menuItems.forEachIndexed { index, item ->
                SettingsMenuItem(
                    item = item,
                    onClick = clickHandlers[index]
                )
                
                // Divider (except for last item)
                if (index < menuItems.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 1.dp,
                        color = SandboxOutlineVariant.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    item: SettingMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with colored background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = SandboxOnSurface,
                modifier = Modifier.weight(1f)
            )
            
            // Chevron
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Navegar",
                tint = SandboxOutlineVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsMenuListPreview() {
    SandboxTheme {
        SettingsMenuList(
            modifier = Modifier.padding(16.dp)
        )
    }
}
