package com.example.sos

sealed class Screen(val route: String) {

    object Login : Screen("login")
    object Main : Screen("main")

    object HomeScreen : Screen("home")
    object Contacts : Screen("contacts")
    object SafetyModeScreen : Screen("map")
    object SafetySettingsScreen : Screen("settings")
}


object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PERMISSION = "permission"
    const val HOME = "home"
}
