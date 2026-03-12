package com.example.sos.loginCred

data class UserData(
    val name: String = "User01",
    val phone: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val isGuest: Boolean = false
)