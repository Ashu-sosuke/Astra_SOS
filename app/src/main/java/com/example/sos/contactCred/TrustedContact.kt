package com.example.sos.contactCred


import java.util.UUID

data class TrustedContact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val phoneHash: String = "",
    val relation: String = "",
    val relationColor: Int = 0xFF38BDF8.toInt() // default visible color
)

