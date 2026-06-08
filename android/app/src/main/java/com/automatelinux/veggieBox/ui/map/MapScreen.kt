package com.automatelinux.veggieBox.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.automatelinux.veggieBox.R
import com.automatelinux.veggieBox.ui.MainViewModel
import com.automatelinux.veggieBox.util.Intents
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val stops = state.route?.stops.orEmpty()
    val biz = state.route?.business
    val geocoded = stops.filter { it.lat != null && it.lon != null }
    val visible = if (state.hideDelivered) geocoded.filter { it.status != "delivered" } else geocoded

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.5)
            controller.setCenter(GeoPoint(biz?.mapCenterLat ?: 30.8536, biz?.mapCenterLon ?: 34.7820))
        }
    }
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onDetach() }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    "${visible.size} מתוך ${stops.size} עם מיקום על המפה",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("הסתר שנמסרו", style = MaterialTheme.typography.bodySmall)
                Switch(checked = state.hideDelivered, onCheckedChange = { vm.toggleHideDelivered() })
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { mv ->
                mv.overlays.clear()
                visible.forEach { s ->
                    val m = Marker(mv)
                    m.position = GeoPoint(s.lat!!, s.lon!!)
                    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    m.icon = ContextCompat.getDrawable(
                        ctx,
                        if (s.status == "delivered") R.drawable.pin_done else R.drawable.pin_pending,
                    )
                    m.title = s.name
                    m.subDescription = s.address ?: ""
                    m.setOnMarkerClickListener { _, _ ->
                        Intents.waze(ctx, s.lat, s.lon, s.address)
                        true
                    }
                    mv.overlays.add(m)
                }
                mv.invalidate()
            },
        )
    }
}
