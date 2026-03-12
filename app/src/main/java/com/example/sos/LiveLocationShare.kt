package com.example.sos


import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sos.location.LiveLocationMap
import com.example.sos.location.SafetyModeViewModel
import kotlinx.coroutines.delay


@Composable
fun SafetyModeScreen(
    onBack: () -> Unit,
    viewModel: SafetyModeViewModel = viewModel()
) {

    val startTime = remember { System.currentTimeMillis() }
    var elapsed by remember { mutableStateOf(0L) }

    LaunchedEffect(true) {
        while (true) {
            elapsed = System.currentTimeMillis() - startTime
            delay(1000)
        }
    }

    val background = Color(0xFF0B1220)
    val card = Color(0xFF151E30)
    val primaryBlue = Color(0xFF1F5EFF)
    val location by viewModel.location.collectAsState()
    val context = LocalContext.current
    val history by viewModel.locationHistory.collectAsState()


    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            if (fineGranted) {
                viewModel.startSOS()
            }
        }
    val seconds = (elapsed / 1000) % 60
    val minutes = (elapsed / 60000) % 60
    val hours = elapsed / 3600000

    DisposableEffect(Unit) {

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted) {
            viewModel.startSOS()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        onDispose {
            viewModel.stopSOS()
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .background(background)
            .padding(16.dp)
    ) {

        // Top Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .clickable{onBack()}
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Location Sharing",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(16.dp))

        // Emergency Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.Red, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "EMERGENCY ACTIVE",
                color = Color.Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Safety Mode Active",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Sharing real-time location tracking",
            color = Color.LightGray,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(16.dp))

        // Timer
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimerBox(hours.toString().padStart(2,'0'), "HRS")
            TimerBox(minutes.toString().padStart(2,'0'), "MIN")
            TimerBox(seconds.toString().padStart(2,'0'), "SEC")
        }

        Spacer(Modifier.height(16.dp))

        // Map Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                LiveLocationMap(
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    history = history
                )
            }

            // Live Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "● Live GPS",
                    color = Color.Green,
                    fontSize = 12.sp
                )
            }
        }



        Spacer(Modifier.height(24.dp))

        // Trusted Contacts
        SectionHeader("Trusted Contacts", "Manage")

        ContactRow("Mom", true)
        ContactRow("Partner", true)

        Spacer(Modifier.height(24.dp))

        // Community & Authorities
        SectionHeader("Community & Authorities")

        AuthorityRow(
            title = "Community Shield",
            subtitle = "Alert sent to 12 nearby volunteers",
            enabled = true
        )

        AuthorityRow(
            title = "Local Police",
            subtitle = "Dispatch Notified",
            enabled = true
        )

        AuthorityRow(
            title = "Nearest Hospital",
            subtitle = "Share if injured",
            enabled = false
        )

        Spacer(Modifier.weight(1f))

        // Buttons
        Button(
            onClick = {
                viewModel.stopSOS()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
        ) {
            Text("I am Safe", color = Color.White, fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Stop Sharing Only", color = Color.White)
        }
    }
}

@Composable
fun TimerBox(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFF1E2A44), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        action?.let {
            Text(it, color = Color(0xFF3D6BFF), fontSize = 14.sp)
        }
    }
}

@Composable
fun ContactRow(name: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF151E30), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Gray, CircleShape)
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(name, color = Color.White, fontWeight = FontWeight.Medium)
            Text("Notified", color = Color(0xFF4CAF50), fontSize = 12.sp)
        }

        Switch(checked = enabled, onCheckedChange = {})
    }
}

@Composable
fun AuthorityRow(title: String, subtitle: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF151E30), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF1E2A44), CircleShape)
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }

        Switch(checked = enabled, onCheckedChange = {})
    }
}

