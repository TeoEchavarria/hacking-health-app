package com.samsung.android.health.sdk.sample.healthdiary.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.samsung.android.health.sdk.sample.healthdiary.R

/**
 * Data class representing a location marker on the map.
 */
data class MapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String? = null,
    val iconResId: Int? = null,
    val isCurrentUser: Boolean = false
)

/**
 * Composable wrapper for OpenStreetMap's MapView.
 *
 * Provides a full-featured map with:
 * - Multiple markers for different users
 * - Zoom controls
 * - Tile caching for offline use
 *
 * @param markers List of markers to display on the map
 * @param centerOnMarker ID of marker to center on (optional)
 * @param zoomLevel Initial zoom level (default: 15.0)
 * @param onMapReady Callback when map is ready
 * @param modifier Compose modifier
 */
@Composable
fun OsmMapView(
    markers: List<MapMarker>,
    centerOnMarker: String? = null,
    zoomLevel: Double = 15.0,
    onMapReady: ((MapView) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Initialize osmdroid configuration
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            // Cache tiles for offline use
            osmdroidTileCache = context.cacheDir
        }
    }
    
    // Create MapView instance
    val mapView = remember { 
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoomLevel)
            
            // Enable zoom buttons
            zoomController.setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
            )
        }
    }
    
    // Handle lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    
    // Update markers when they change
    DisposableEffect(markers, centerOnMarker) {
        mapView.overlays.clear()
        
        markers.forEach { markerData ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(markerData.latitude, markerData.longitude)
                title = markerData.title
                snippet = markerData.snippet
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                
                // Set custom icon if provided
                markerData.iconResId?.let { resId ->
                    icon = ContextCompat.getDrawable(context, resId)
                }
            }
            mapView.overlays.add(marker)
            
            // Center on specific marker if requested
            if (markerData.id == centerOnMarker) {
                mapView.controller.animateTo(
                    GeoPoint(markerData.latitude, markerData.longitude)
                )
            }
        }
        
        // If no specific center, center on first marker
        if (centerOnMarker == null && markers.isNotEmpty()) {
            val firstMarker = markers.first()
            mapView.controller.setCenter(
                GeoPoint(firstMarker.latitude, firstMarker.longitude)
            )
        }
        
        mapView.invalidate()
        
        onDispose { }
    }
    
    // Notify when map is ready
    DisposableEffect(Unit) {
        onMapReady?.invoke(mapView)
        onDispose { }
    }
    
    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

/**
 * Extension function to center the map on a specific location with animation.
 */
fun MapView.centerOnLocation(latitude: Double, longitude: Double, animate: Boolean = true) {
    val point = GeoPoint(latitude, longitude)
    if (animate) {
        controller.animateTo(point)
    } else {
        controller.setCenter(point)
    }
}

/**
 * Extension function to zoom to show all markers within bounds.
 */
fun MapView.zoomToFitMarkers(markers: List<MapMarker>, padding: Double = 0.01) {
    if (markers.isEmpty()) return
    
    if (markers.size == 1) {
        val marker = markers.first()
        controller.setCenter(GeoPoint(marker.latitude, marker.longitude))
        controller.setZoom(16.0)
        return
    }
    
    val minLat = markers.minOf { it.latitude } - padding
    val maxLat = markers.maxOf { it.latitude } + padding
    val minLon = markers.minOf { it.longitude } - padding
    val maxLon = markers.maxOf { it.longitude } + padding
    
    val boundingBox = org.osmdroid.util.BoundingBox(maxLat, maxLon, minLat, minLon)
    zoomToBoundingBox(boundingBox, true)
}
