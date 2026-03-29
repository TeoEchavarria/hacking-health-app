package com.samsung.android.health.sdk.sample.healthdiary.views

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.*
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.TrackingViewModel

/**
 * Tracking Screen - GPS Location Timeline
 * 
 * Real-time location tracking with:
 * - Background map view with pulse indicator
 * - Floating status card showing current state
 * - 24-hour location history timeline
 * - Call and verify action buttons
 */
@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { TrackingViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background Map
        MapMockView(
            userAvatarUrl = null, // Could pass user profile image URL
            modifier = Modifier.fillMaxSize()
        )
        
        // Foreground Content (scrollable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card (floating over map)
            LocationStatusCard(
                userName = uiState.userName,
                activityStatus = "En movimiento (${uiState.currentActivity})",
                batteryLevel = uiState.batteryLevel,
                onCallClick = {
                    Toast.makeText(context, "Llamar próximamente", Toast.LENGTH_SHORT).show()
                    viewModel.callEmergency()
                },
                onVerifyClick = {
                    Toast.makeText(context, "Verificar ubicación próximamente", Toast.LENGTH_SHORT).show()
                    viewModel.verifyLocation()
                }
            )
            
            // Timeline Section
            if (uiState.locationHistory.isNotEmpty()) {
                LocationTimelineCard(
                    locations = uiState.locationHistory,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * Location Timeline Card
 * 
 * Scrollable card displaying the 24-hour location history
 */
@Composable
private fun LocationTimelineCard(
    locations: List<com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.LocationPointEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recorrido 24h",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SandboxOnSurface
                )
                Text(
                    text = "${locations.size} puntos",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = SandboxPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Timeline items
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(locations) { index, location ->
                    LocationTimelineItem(
                        address = location.address,
                        timestamp = location.timestamp,
                        isCurrent = index == 0,
                        isLast = index == locations.lastIndex
                    )
                }
            }
        }
    }
}
