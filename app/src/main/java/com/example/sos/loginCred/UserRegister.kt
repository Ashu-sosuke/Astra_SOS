package com.example.sos.loginCred

import android.graphics.drawable.Icon
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sos.Routes

@Composable
fun RegisterScreen(
    phone: String,
    navController: NavController,
    viewModel: AuthViewModel
) {

    val isLoading = viewModel.isLoading

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable {
                    if (!isLoading) imagePicker.launch("image/*")
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Complete Profile")

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            enabled = !isLoading
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = {},
            enabled = false,
            label = { Text("Phone") }
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (Optional)") },
            enabled = !isLoading
        )

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color.Red)
        }

        if (isLoading) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
        }

        Spacer(Modifier.height(24.dp))

        Button(
            enabled = name.isNotBlank() && !isLoading,
            onClick = {
                viewModel.saveNewUser(
                    name = name,
                    phone = phone,
                    email = email,
                    imageUri = imageUri,
                    onSuccess = {
                        navController.navigate(Routes.PERMISSION) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    },
                    onError = {
                        errorMessage = it
                    }
                )
            }
        ) {
            Text("Continue")
        }
    }
}