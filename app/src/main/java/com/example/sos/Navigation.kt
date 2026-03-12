package com.example.sos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sos.loginCred.AuthViewModel
import com.example.sos.loginCred.LogInScreen
import com.example.sos.loginCred.RegisterScreen
import com.example.sos.utils.BottomNavBar

@Composable
fun Navigation(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {


    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        composable(Routes.SPLASH) {
            SplashScreen(navController, authViewModel)
        }

        composable(Routes.LOGIN) {
            LogInScreen(navController, authViewModel)
        }

        composable("${Routes.REGISTER}/{phone}") { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            RegisterScreen(phone, navController, authViewModel)
        }

        composable(Routes.PERMISSION) {
            PermissionScreen(navController)
        }


        composable(Routes.HOME) {
            MainScreen(rootNavController = navController, authViewModel)
        }

    }
}
