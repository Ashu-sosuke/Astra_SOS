package com.example.sos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sos.loginCred.AuthViewModel
import com.example.sos.utils.BottomNavBar

@Composable
fun MainScreen(rootNavController: NavHostController, authViewModel: AuthViewModel) {

    val bottomNavController = rememberNavController()
    Scaffold(
        bottomBar = {
            BottomNavBar(navController = bottomNavController)
        }
    ) { paddingValues ->

        NavHost(
            navController = bottomNavController,
            startDestination = Screen.HomeScreen.route,
            modifier = Modifier.padding(paddingValues)
        ) {

            composable(Screen.HomeScreen.route) {
                HomeScreen()
            }

            composable(Screen.Contacts.route) {
                TrustedContactsScreen(onBack = {
                    bottomNavController.popBackStack()
                })
            }

            composable(Screen.SafetyModeScreen.route) {
                SafetyModeScreen(onBack = {
                    bottomNavController.popBackStack()
                })
            }

            composable(Screen.SafetySettingsScreen.route) {
                SafetySettingsScreen(onBack = {
                    bottomNavController.popBackStack()
                },
                    navController = rootNavController,
                    authViewModel
                )
            }
        }
    }
}
