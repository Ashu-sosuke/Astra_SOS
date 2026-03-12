package com.example.sos.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.sos.R
import com.example.sos.Screen

@Composable
fun BottomNavBar(
    navController: NavController
) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {

        BottomNavItem(
            icon = painterResource(R.drawable.baseline_home_24),
            label = "Home",
            selected = currentRoute == Screen.HomeScreen.route
        ) {
            navigateTo(navController, Screen.HomeScreen.route)
        }

        BottomNavItem(
            icon = painterResource(R.drawable.baseline_location_pin_24),
            label = "Map",
            selected = currentRoute == Screen.SafetyModeScreen.route
        ) {
            navigateTo(navController, Screen.SafetyModeScreen.route)
        }

        BottomNavItem(
            icon = painterResource(R.drawable.outline_groups_2_24),
            label = "Contacts",
            selected = currentRoute == Screen.Contacts.route
        ) {
            navigateTo(navController, Screen.Contacts.route)
        }

        BottomNavItem(
            icon = painterResource(R.drawable.baseline_settings_24),
            label = "Settings",
            selected = currentRoute == Screen.SafetySettingsScreen.route
        ) {
            navigateTo(navController, Screen.SafetySettingsScreen.route)
        }
    }
}

/**
 * Centralized safe navigation for BottomNav
 */
private fun navigateTo(
    navController: NavController,
    route: String
) {
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun BottomNavItem(
    icon: Painter,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    val tint = if (selected) Color(0xFF3B82F6) else Color(0xFF6B7280)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {

        Icon(
            painter = icon,
            contentDescription = label,
            tint = tint
        )

        Text(
            text = label,
            fontSize = 11.sp,
            color = tint
        )
    }
}
