package com.example.sos

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.sos.loginCred.AuthViewModel
import com.example.sos.loginCred.EditProfileDialog
import coil.compose.rememberAsyncImagePainter


@Composable
fun SafetySettingsScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    authViewModel: AuthViewModel,
) {
    val background = Color(0xFF0B1220)
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }

    var stressDetection by remember { mutableStateOf(true) }
    var autoSos by remember { mutableStateOf(true) }
    var backgroundTracking by remember { mutableStateOf(true) }
    var micSensitivity by remember { mutableFloatStateOf(0.75f) }

    // ✅ SHOW CONFIRMATION
    LaunchedEffect(Unit) {
        authViewModel.loadUserProfile()
    }

    if (showEditProfile) {
        EditProfileDialog(
            authViewModel = authViewModel,
            onDismiss = { showEditProfile = false }
        )
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                showLogoutDialog = false
                authViewModel.logout {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0)
                    }
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    // 🔥 REQUIRED FOR SNACKBAR
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(16.dp)
        ) {

            // Top Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.clickable { onBack() }
                )

                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Safety Settings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(24.dp))

            ProfileCard(
                userName = authViewModel.profile.name,
                image = authViewModel.profile.photoUrl,
                onEditProfile = { showEditProfile = true }
            )

            Spacer(Modifier.height(24.dp))

            SectionTitle("AI PROTECTION")

            SettingCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Voice Stress Detection", color = Color.White)
                        Text("Analyzes audio for screams", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = stressDetection,
                        onCheckedChange = { stressDetection = it }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text("Microphone Sensitivity", color = Color.White)

                Slider(
                    value = micSensitivity,
                    onValueChange = { micSensitivity = it },
                    valueRange = 0f..1f
                )
            }

            Spacer(Modifier.height(24.dp))

            SectionTitle("EMERGENCY RESPONSE")

            SettingToggle(
                title = "Auto-SOS Trigger",
                subtitle = "Alert contacts when threat verified",
                checked = autoSos,
                onCheckedChange = { autoSos = it }
            )

            SettingToggle(
                title = "Background Tracking",
                subtitle = "Share location during SOS events",
                checked = backgroundTracking,
                onCheckedChange = { backgroundTracking = it }
            )

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Log Out", color = Color.White)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun LogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Logout") },
        text = { Text("Are you sure you want to logout?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Logout", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151E30), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF151E30), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun NavigationRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFF151E30), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@Composable
fun ProfileCard(
    userName: String,
    image: Any?,
    onEditProfile: () -> Unit,
) {
    SettingCard {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Edit info",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onEditProfile() }
                )
            }

            AsyncImage(
                model = image,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                placeholder = painterResource(R.drawable.baseline_person_2_24),
                error = painterResource(R.drawable.baseline_person_2_24)
            )

        }

        Spacer(Modifier.height(24.dp))
    }
}
