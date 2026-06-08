package com.automatelinux.veggieBox.ui.map

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.automatelinux.veggieBox.R
import com.automatelinux.veggieBox.data.model.Stop
import com.automatelinux.veggieBox.ui.MainViewModel
import com.automatelinux.veggieBox.util.Intents
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen(vm: MainViewModel, onOpenInRoute: (Int) -> Unit = {}) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    // A tapped pin opens a small action sheet instead of jumping straight to Waze.
    var selected by remember { mutableStateOf<Stop?>(null) }
    val stops = state.route?.stops.orEmpty()
    val biz = state.route?.business
    // Number stops by their position in the same ordered list the Route screen shows,
    // so a pin's number always matches its row number there (non-geocoded stops still
    // count toward the order, but only geocoded ones get a marker).
    val ordered = if (state.hideDelivered) stops.filter { it.status != "delivered" } else stops
    val numberOf = ordered.withIndex().associate { (i, s) -> s.stopId to i + 1 }
    val visible = ordered.filter { it.lat != null && it.lon != null }

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.5)
            controller.setCenter(GeoPoint(biz?.mapCenterLat ?: 30.8536, biz?.mapCenterLon ?: 34.7820))
        }
    }

    // Live "you are here" overlay — shows the worker's moving position + heading.
    val myLocation = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(ctx), mapView).apply { enableMyLocation() }
    }
    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) myLocation.enableMyLocation()
    }
    LaunchedEffect(Unit) { locPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { myLocation.disableMyLocation(); mapView.onDetach() }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
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
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = { mv ->
                    mv.overlays.clear()
                    visible.forEach { s ->
                        val pos = numberOf[s.stopId] ?: 0
                        val m = Marker(mv)
                        m.position = GeoPoint(s.lat!!, s.lon!!)
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        m.icon = numberedPin(ctx, pos, s.status == "delivered")
                        m.title = "$pos. ${s.name}"
                        m.subDescription = s.address ?: ""
                        m.setOnMarkerClickListener { _, _ ->
                            selected = s
                            true
                        }
                        mv.overlays.add(m)
                    }
                    // Keep the live-location overlay on top of the markers.
                    mv.overlays.add(myLocation)
                    mv.invalidate()
                },
            )
            // Center-on-me button (bottom-start, clear of the feedback FAB at bottom-end).
            FloatingActionButton(
                onClick = {
                    val me = myLocation.myLocation
                    if (me != null) {
                        mapView.controller.animateTo(me)
                        mapView.controller.setZoom(17.0)
                    }
                },
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "מרכז על המיקום שלי")
            }
        }

        // Tapping a pin opens this action sheet: jump to the stop in the Route (מסלול)
        // tab, or hand off to Waze for turn-by-turn navigation.
        selected?.let { s ->
            val pos = numberOf[s.stopId] ?: 0
            AlertDialog(
                onDismissRequest = { selected = null },
                title = { Text("$pos. ${s.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val addr = s.address
                        if (!addr.isNullOrBlank()) {
                            Text(addr, style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = { selected = null; onOpenInRoute(s.stopId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("📋 פתח במסלול") }
                        FilledTonalButton(
                            onClick = { selected = null; Intents.waze(ctx, s.lat, s.lon, s.address) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("🧭 נווט עם Waze") }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { selected = null }) { Text("ביטול") } },
            )
        }
    }
}

// Build a teardrop pin (orange = pending, green = delivered) with the stop's route
// number drawn in a white disc in the pin head, so the worker can read the delivery
// order straight off the map without opening each marker.
private fun numberedPin(ctx: Context, position: Int, delivered: Boolean): BitmapDrawable {
    val base = ContextCompat.getDrawable(
        ctx,
        if (delivered) R.drawable.pin_done else R.drawable.pin_pending,
    )!!.mutate()
    val w = base.intrinsicWidth
    val h = base.intrinsicHeight
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    base.setBounds(0, 0, w, h)
    base.draw(canvas)

    // Head circle of the teardrop sits at (12, 9) in the 24-unit viewport.
    val cx = w / 2f
    val cy = h * (9f / 24f)
    val r = w * (5.4f / 24f)
    val statusColor = if (delivered) 0xFF2E7D32.toInt() else 0xFFF57C00.toInt()

    val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, r, disc)

    val label = position.toString()
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = statusColor
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = r * 1.7f
    }
    // Shrink the text so multi-digit numbers still fit inside the disc.
    val maxWidth = r * 1.7f
    val measured = text.measureText(label)
    if (measured > maxWidth) text.textSize *= maxWidth / measured

    val fm = text.fontMetrics
    val baseline = cy - (fm.ascent + fm.descent) / 2f
    canvas.drawText(label, cx, baseline, text)

    return BitmapDrawable(ctx.resources, bmp)
}
