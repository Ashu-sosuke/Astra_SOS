package com.example.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.sos.loginCred.AuthViewModel
import com.example.sos.ui.theme.SOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SOSTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()

                Navigation(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
