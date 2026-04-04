// location/data/CrimeIncident.kt
package com.example.sos.location.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crimes")
data class CrimeIncident(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String       = "",
    val description: String = "",
    val category: String    = "",   // "Murder" | "Rape" | "Missing" | "Assault"
    val date: String        = "",
    val latitude: Double    = 0.0,
    val longitude: Double   = 0.0,
    val city: String        = "",
    val state: String       = "",
    val severity: Int       = 5,    // 1-10 from LLM
    val victimCount: Int    = 1,
    val source: String      = ""
)