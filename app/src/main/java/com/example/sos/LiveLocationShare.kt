package com.example.sos

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sos.location.SafetyModeViewModel
import com.example.sos.location.data.CrimeIncident
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LocationScreen(
    onBack: () -> Unit,
    safetyViewModel: SafetyModeViewModel = viewModel()
) {
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ── State ────────────────────────────────────────────────────────────
    val currentLocation by safetyViewModel.location.collectAsState()
    val isLoading       by safetyViewModel.isLoadingIncidents.collectAsState()
    val isSosActive     by safetyViewModel.isSosActive.collectAsState()
    val incidents: List<CrimeIncident> by safetyViewModel.incidents
        .observeAsState(initial = emptyList())

    // ── Timer — resets when SOS becomes active ───────────────────────────
    var elapsed      by remember { mutableStateOf(0L) }
    var sosStartTime by remember { mutableStateOf(0L) }
    LaunchedEffect(isSosActive) {
        if (isSosActive) {
            sosStartTime = System.currentTimeMillis()
            while (isSosActive) {
                elapsed = System.currentTimeMillis() - sosStartTime
                if (!isSosActive) break
                delay(1000)
            }
        } else {
            elapsed = 0L
        }
    }

    // ── Camera ───────────────────────────────────────────────────────────
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(22.5, 80.0), 5f)
    }

    var centeredOnce by remember { mutableStateOf(false) }
    LaunchedEffect(currentLocation) {
        if (currentLocation != null && !centeredOnce) {
            centeredOnce = true
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentLocation!!.latitude, currentLocation!!.longitude), 15f
                ), 1200
            )
        }
    }

    // ── Permission launcher ───────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            safetyViewModel.startLocationTracking()
            safetyViewModel.loadIncidentsFromFirestore()
        }
    }

    // ── Start tracking on screen open ─────────────────────────────────────
    DisposableEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            safetyViewModel.startLocationTracking()
            safetyViewModel.loadIncidentsFromFirestore()
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        onDispose {
            safetyViewModel.stopSOS()
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Google Map ────────────────────────────────────────────────────
        GoogleMap(
            modifier            = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType             = MapType.NORMAL,
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled     = false,
                myLocationButtonEnabled = false,
                compassEnabled          = true
            )
        ) {
            // ── Blue dot — always visible on map screen ───────────────────
            currentLocation?.let { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = "You are here",
                    icon  = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)

                )
            }

            // ── Crime circles ─────────────────────────────────────────────
            incidents.forEach { incident: CrimeIncident ->
                val pos = LatLng(incident.latitude, incident.longitude)

                val (fillCol, strokeCol) = when (incident.category) {
                    "Murder"            -> Pair(Color(0x55FF0000), Color(0xCCFF0000))
                    "Rape", "Assault"   -> Pair(Color(0x55FF8800), Color(0xCCFF8800))
                    "Missing", "Kidnap" -> Pair(Color(0x550055FF), Color(0xCC0055FF))
                    else                -> Pair(Color(0x55888888), Color(0xCC888888))
                }

                val baseRadius  = 8_000.0
                val outerRadius = baseRadius * incident.severity
                val innerRadius = baseRadius * incident.severity * 0.4

                Circle(
                    center      = pos,
                    radius      = outerRadius,
                    fillColor   = fillCol,
                    strokeColor = strokeCol,
                    strokeWidth = 1.5f
                )

                Circle(
                    center      = pos,
                    radius      = innerRadius,
                    fillColor   = strokeCol,
                    strokeColor = strokeCol,
                    strokeWidth = 2f
                )
            }
        }

        // ── Loading spinner ───────────────────────────────────────────────
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 200.dp),
                color       = Color.White,
                strokeWidth = 3.dp
            )
        }

        // ── SOS timer bar — only when active ─────────────────────────────
        if (isSosActive) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape  = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.85f)
                    ),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Text(
                            text       = "SOS ACTIVE  •  ${formatElapsed(elapsed)}",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                currentLocation?.let { loc ->
                    Card(
                        shape  = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.70f)
                        )
                    ) {
                        Text(
                            text = "📍 ${"%.5f".format(loc.latitude)}, " +
                                    "${"%.5f".format(loc.longitude)}  " +
                                    "±${loc.accuracy.toInt()}m",
                            color    = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // ── Right side FABs ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    currentLocation?.let { loc ->
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(loc.latitude, loc.longitude), 16f
                                ), 800
                            )
                        }
                    }
                },
                modifier       = Modifier.size(48.dp),
                shape          = CircleShape,
                containerColor = Color.White,
                contentColor   = Color(0xFF1A73E8)
            ) {
                Icon(
                    imageVector        = Icons.Default.LocationOn,
                    contentDescription = "Go to my location",
                    modifier           = Modifier.size(22.dp)
                )
            }

            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn(), 300)
                    }
                },
                modifier       = Modifier.size(48.dp),
                shape          = CircleShape,
                containerColor = Color.White,
                contentColor   = Color.Black
            ) {
                Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut(), 300)
                    }
                },
                modifier       = Modifier.size(48.dp),
                shape          = CircleShape,
                containerColor = Color.White,
                contentColor   = Color.Black
            ) {
                Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ── Legend ────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 90.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.80f)
            ),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                LegendItem(Color.Red,           "Murder")
                LegendItem(Color(0xFFFF8800),   "Rape / Assault")
                LegendItem(Color(0xFF1A73E8),   "Missing Person")
                LegendItem(Color(0xFF888888),   "Other Cases")
                LegendItem(Color(0xFF1A73E8),   "Your Location", isPin = true)
            }
        }

        // ── STOP SOS — only when active ───────────────────────────────────
        if (isSosActive) {
            Button(
                onClick = {
                    safetyViewModel.stopSOS()
                    onBack()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 90.dp)
                    .shadow(8.dp, RoundedCornerShape(50)),
                colors         = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape          = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    "STOP SOS",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isPin: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.padding(vertical = 3.dp)
    ) {
        if (isPin) {
            Text("📍", fontSize = 12.sp)
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

private fun formatElapsed(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}