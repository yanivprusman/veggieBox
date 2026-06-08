package com.automatelinux.veggieBox.ui.route

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.automatelinux.veggieBox.data.model.Stop
import com.automatelinux.veggieBox.ui.MainViewModel
import com.automatelinux.veggieBox.util.Intents
import com.automatelinux.veggieBox.util.LocationUtil
import kotlinx.coroutines.launch

val Green = Color(0xFF2E7D32)
val Orange = Color(0xFFF57C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    vm: MainViewModel,
    onOpenStop: (Int) -> Unit,
    onOpenMap: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    // When a reorder lands (the ViewModel bumps reorderTick only after the route has
    // reloaded), snap the list to the top so the change — always at #1 — is visible.
    val reorderTick by vm.reorderTick.collectAsState()
    LaunchedEffect(reorderTick) {
        if (reorderTick > 0) {
            listState.animateScrollToItem(0)
            snackbar.showSnackbar("המסלול סודר מחדש")
        }
    }

    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) LocationUtil.requestOneShot(ctx) { lat, lon -> vm.optimize(lat, lon) }
        else vm.optimize(null, null)
    }
    var showStartChooser by remember { mutableStateOf(false) }

    val biz = state.route?.business
    val stops = state.route?.stops.orEmpty()
    val visible = if (state.hideDelivered) stops.filter { it.status != "delivered" } else stops

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green, titleContentColor = Color.White, actionIconContentColor = Color.White),
                title = { Text("🥬 " + (biz?.name ?: "VeggieBox"), maxLines = 1) },
                actions = {
                    IconButton(onClick = { vm.toggleHideDelivered() }) {
                        Icon(
                            if (state.hideDelivered) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "הסתר/הצג שנמסרו",
                        )
                    }
                    IconButton(onClick = onOpenMap) { Icon(Icons.Filled.Map, contentDescription = "מפה") }
                    IconButton(onClick = { showStartChooser = true }) {
                        Icon(Icons.Filled.Route, contentDescription = "סדר מסלול")
                    }
                    IconButton(onClick = { vm.load() }) { Icon(Icons.Filled.Refresh, contentDescription = "רענן") }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            StatsHeader(state)
            HorizontalDivider()
            HideToggleRow(state.hideDelivered) { vm.toggleHideDelivered() }
            if (state.loading && stops.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else if (state.error != null && stops.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Text("שגיאת חיבור לשרת:\n${state.error}", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(visible, key = { _, it -> it.stopId }) { index, stop ->
                        StopRow(
                            position = index + 1,
                            stop = stop,
                            onOpen = { onOpenStop(stop.stopId) },
                            onDeliver = { vm.deliver(stop.stopId) },
                            onUndo = { vm.undeliver(stop.stopId) },
                            onOnMyWay = {
                                scope.launch {
                                    val url = vm.onMyWay(stop.stopId)
                                    if (url != null) Intents.whatsapp(ctx, url)
                                    else snackbar.showSnackbar("אין מספר טלפון ללקוח")
                                }
                            },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (showStartChooser) {
        StartChooserDialog(
            centralLabel = biz?.defaultCentralDrop,
            onCentral = { showStartChooser = false; vm.optimize(null, null) },
            onMyGps = {
                showStartChooser = false
                locPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = { showStartChooser = false },
        )
    }
}

// "Order route by distance — start from where?" The central drop (pallet point) is
// the default; the worker's live GPS is the alternative for re-ordering mid-route.
@Composable
private fun StartChooserDialog(
    centralLabel: String?,
    onCentral: () -> Unit,
    onMyGps: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("סדר מסלול לפי מרחק") },
        text = {
            Text(
                "מאיפה להתחיל את המסלול?" +
                    (if (!centralLabel.isNullOrBlank()) "\n\nנקודה מרכזית: $centralLabel" else ""),
            )
        },
        confirmButton = { TextButton(onClick = onCentral) { Text("📦 נקודה מרכזית") } },
        dismissButton = { TextButton(onClick = onMyGps) { Text("📍 המיקום שלי") } },
    )
}

@Composable
private fun StatsHeader(state: com.automatelinux.veggieBox.ui.UiState) {
    val p = state.route?.progress
    val e = state.earnings
    val cur = state.route?.business?.currency ?: "₪"
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatChip("נמסרו", "${p?.delivered ?: 0}/${p?.total ?: 0}", Green, Modifier.weight(1f))
        StatChip("נותרו", "${p?.remaining ?: 0}", Orange, Modifier.weight(1f))
        StatChip("רווח היום", "${(e?.today?.amount ?: 0.0).toInt()}$cur", Color(0xFF1565C0), Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.10f)) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HideToggleRow(hide: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("הסתר משלוחים שנמסרו", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = hide, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun StopRow(
    position: Int,
    stop: Stop,
    onOpen: () -> Unit,
    onDeliver: () -> Unit,
    onUndo: () -> Unit,
    onOnMyWay: () -> Unit,
) {
    val delivered = stop.status == "delivered"
    Card(
        Modifier.fillMaxWidth().clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = if (delivered) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(12.dp).clip(CircleShape)
                        .background(if (delivered) Green else Orange),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "$position. ${stop.name}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (delivered) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AssistChip(onClick = onOpen, label = { Text("×${stop.cartons}") })
            }
            Text(
                stop.address ?: "— אין כתובת —",
                style = MaterialTheme.typography.bodyMedium,
                color = if (stop.address == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, top = 2.dp),
            )
            if (!stop.houseInstructions.isNullOrBlank()) {
                Text(
                    "📍 ${stop.houseInstructions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8D6E00),
                    modifier = Modifier.padding(start = 22.dp, top = 2.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!delivered) {
                    OutlinedButton(onClick = onOnMyWay, modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp)) {
                        Icon(Icons.Filled.Send, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("בדרך")
                    }
                    Button(onClick = onDeliver, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Green), contentPadding = PaddingValues(8.dp)) {
                        Icon(Icons.Filled.Check, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("נמסר")
                    }
                } else {
                    TextButton(onClick = onUndo) { Text("בטל מסירה") }
                    val drop = when (stop.dropUsed) { "central" -> "נקודה מרכזית"; "beside" -> "ליד הבית"; else -> "נמסר" }
                    Text(drop, Modifier.align(Alignment.CenterVertically), color = Green, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
