package com.automatelinux.veggieBox.ui.detail

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.automatelinux.veggieBox.data.model.Stop
import com.automatelinux.veggieBox.ui.MainViewModel
import com.automatelinux.veggieBox.ui.theme.BrandGreen
import com.automatelinux.veggieBox.util.Intents
import com.automatelinux.veggieBox.util.LocationUtil
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailScreen(vm: MainViewModel, stopId: Int, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val stop = state.route?.stops?.firstOrNull { it.stopId == stopId }
    val baseUrl = remember { com.automatelinux.veggieBox.BuildConfig.API_BASE_URL.trimEnd('/') }

    var showDrop by remember { mutableStateOf(false) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    val snackbar = remember { SnackbarHostState() }

    // Failed writes (status/cartons/upload/pin) surface here — never silently.
    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) captureFile?.let { vm.uploadMedia(stopId, it) }
    }
    val takeVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        if (ok) captureFile?.let { vm.uploadMedia(stopId, it) }
    }
    fun capture(video: Boolean) {
        val ext = if (video) "mp4" else "jpg"
        val f = File(ctx.cacheDir, "cap-$stopId-${System.currentTimeMillis()}.$ext")
        captureFile = f
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        if (video) takeVideo.launch(uri) else takePhoto.launch(uri)
    }

    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && stop != null) {
            LocationUtil.requestOneShot(ctx) { lat, lon -> vm.setCustomerLocation(stop.customerId, lat, lon) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandGreen, titleContentColor = Color.White, navigationIconContentColor = Color.White),
                title = { Text(stop?.name ?: "פרטי משלוח", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "חזור") }
                },
            )
        },
    ) { pad ->
        if (stop == null) {
            Box(Modifier.padding(pad).fillMaxSize(), Alignment.Center) { Text("המשלוח לא נמצא") }
            return@Scaffold
        }
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Info card
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoRow(Icons.Filled.Person, stop.name, bold = true)
                    InfoRow(Icons.Filled.Place, stop.address ?: "אין כתובת — בקש מהלקוח (מסך 'חסרי פרטים')")
                    val houseInstr = stop.houseInstructions
                    if (!houseInstr.isNullOrBlank()) {
                        InfoRow(Icons.Filled.Info, houseInstr, color = MaterialTheme.colorScheme.tertiary)
                    }
                    stop.dropPreference?.let {
                        val txt = if (it == "central") "אם לא בבית: נקודה מרכזית" else "אם לא בבית: ליד הבית"
                        InfoRow(Icons.Filled.MoveToInbox, txt, color = MaterialTheme.colorScheme.secondary)
                    }
                    InfoRow(Icons.Filled.Inventory2, "${stop.cartons} ארגזים")
                }
            }

            // Cartons stepper
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ארגזים:", Modifier.weight(1f))
                IconButton(onClick = { if (stop.cartons > 1) vm.setCartons(stopId, stop.cartons - 1) }) { Icon(Icons.Filled.Remove, "פחות") }
                Text("${stop.cartons}", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { vm.setCartons(stopId, stop.cartons + 1) }) { Icon(Icons.Filled.Add, "עוד") }
            }

            // Contact / navigation
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn("בדרך 💬", Icons.AutoMirrored.Filled.Send, Modifier.weight(1f)) {
                    scope.launch { vm.onMyWay(stopId)?.let { Intents.whatsapp(ctx, it) } }
                }
                ActionBtn("Waze 🧭", Icons.Filled.Navigation, Modifier.weight(1f)) {
                    Intents.waze(ctx, stop.lat, stop.lon, stop.address)
                }
                stop.phone?.let { ph ->
                    ActionBtn("חייג", Icons.Filled.Call, Modifier.weight(1f)) { Intents.dial(ctx, ph) }
                }
            }

            HorizontalDivider()

            // Delivery status
            if (stop.status == "delivered") {
                val drop = when (stop.dropUsed) { "central" -> "בנקודה המרכזית"; "beside" -> "ליד הבית"; else -> "ביד/בדלת" }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("נמסר ($drop)", Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { vm.undeliver(stopId) }) { Text("בטל") }
                    }
                }
            } else {
                Button(
                    onClick = { vm.deliver(stopId, "home") },
                    Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("נמסר ביד / בדלת", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = { showDrop = true }, Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Filled.MoveToInbox, null); Spacer(Modifier.width(8.dp)); Text("השארתי — לא היו בבית")
                }
            }

            // Photo / video documentation
            Text("תיעוד (כשמשאירים בחוץ):", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn("צלם תמונה", Icons.Filled.PhotoCamera, Modifier.weight(1f)) { capture(false) }
                ActionBtn("צלם סרטון", Icons.Filled.Videocam, Modifier.weight(1f)) { capture(true) }
            }
            stop.mediaPath?.let { mp ->
                val url = if (mp.startsWith("http")) mp else "$baseUrl$mp"
                val isVideo = mp.substringAfterLast('.', "").lowercase() in listOf("mp4", "3gp", "mov", "webm")
                if (isVideo) {
                    // Coil's AsyncImage can't play video — hand the clip to a player.
                    OutlinedButton(onClick = { Intents.view(ctx, url) }, Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PlayCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("צפה בסרטון התיעוד 🎬")
                    }
                } else {
                    AsyncImage(
                        model = url,
                        contentDescription = "תיעוד",
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                }
            }

            HorizontalDivider()

            // Pin location
            OutlinedButton(
                onClick = { locPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.MyLocation, null)
                Spacer(Modifier.width(8.dp))
                Text(if (stop.lat != null) "עדכן מיקום הבית (אני כאן)" else "שמור מיקום הבית (אני כאן) 📍")
            }
        }
    }

    if (showDrop && stop != null) {
        DropDialog(
            preselect = stop.dropPreference,
            centralLabel = state.route?.business?.defaultCentralDrop ?: "נקודה מרכזית",
            onPick = { drop -> showDrop = false; vm.deliver(stopId, drop) },
            onDismiss = { showDrop = false },
        )
    }
}

@Composable
private fun DropDialog(preselect: String?, centralLabel: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
        title = { Text("איפה השארת את הארגז?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onPick("beside") },
                    Modifier.fillMaxWidth(),
                    colors = if (preselect == "beside") ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                ) { Text("ליד הבית / על יד הדלת") }
                Button(
                    onClick = { onPick("central") },
                    Modifier.fillMaxWidth(),
                    colors = if (preselect == "central") ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                ) { Text(centralLabel) }
            }
        },
    )
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, bold: Boolean = false, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Text(text, color = color, fontWeight = if (bold) FontWeight.Bold else null, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(vertical = 10.dp, horizontal = 6.dp)) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
    }
}
