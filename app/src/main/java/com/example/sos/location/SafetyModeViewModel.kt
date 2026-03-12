package com.example.sos.location

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)

class SafetyModeViewModel(application: Application) : AndroidViewModel(application) {


    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var currentIncidentId: String = ""

    private var lastSavedLocation: LocationData? = null

    private val MIN_DISTANCE_METERS = 10
    private val MAX_ACCURACY_METERS = 30

    // -------------------------------
    // Compose State
    // -------------------------------

    private val _location = MutableStateFlow<LocationData?>(null)
    val location: StateFlow<LocationData?> = _location

    private val _locationHistory = MutableStateFlow<List<LocationData>>(emptyList())
    val locationHistory: StateFlow<List<LocationData>> = _locationHistory

    // ===============================
    // Start SOS
    // ===============================

    fun startSOS(): String {

        val incidentId = System.currentTimeMillis().toString()

        currentIncidentId = incidentId
        lastSavedLocation = null

        val context = getApplication<Application>().applicationContext


        Log.d("SOS", "Incident started: $incidentId")

        val intent = Intent(context, ForegroundLocationService::class.java)
        ContextCompat.startForegroundService(context, intent)

        return incidentId
    }

    // ===============================
    // Process new GPS update
    // ===============================

    fun processLocationUpdate(location: LocationData) {

        if (currentIncidentId.isEmpty()) return

        // Ignore poor GPS
        if (location.accuracy > MAX_ACCURACY_METERS) {
            Log.d("SOS", "Ignoring poor accuracy: ${location.accuracy}")
            return
        }

        // Ignore small movement
        if (!shouldSaveLocation(location)) {
            Log.d("SOS", "Movement < 10m")
            return
        }

        lastSavedLocation = location

        _location.value = location

        _locationHistory.value = _locationHistory.value + location

        uploadLocationToFirebase(location)
    }

    // ===============================
    // Movement Filter
    // ===============================

    private fun shouldSaveLocation(newLocation: LocationData): Boolean {

        val last = lastSavedLocation ?: return true

        val results = FloatArray(1)

        Location.distanceBetween(
            last.latitude,
            last.longitude,
            newLocation.latitude,
            newLocation.longitude,
            results
        )

        val distance = results[0]

        Log.d("SOS", "Distance moved: $distance")

        return distance > MIN_DISTANCE_METERS
    }

    // ===============================
    // Firestore Upload
    // ===============================

    private fun uploadLocationToFirebase(location: LocationData) {

        val userId = auth.currentUser?.uid ?: return

        val incidentRef =
            firestore.collection("users")
                .document(userId)
                .collection("incidents")
                .document(currentIncidentId)

        val locationData = hashMapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "accuracy" to location.accuracy,
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Live location
        incidentRef.set(
            mapOf("lastLocation" to locationData),
            SetOptions.merge()
        )

        // History
        incidentRef
            .collection("locationHistory")
            .add(locationData)

        Log.d("SOS", "Location saved to Firebase")

        FirebaseFirestore.getInstance()
            .collection("live_locations")
            .document(currentIncidentId)
            .set(
                mapOf(
                    "lat" to location.latitude,
                    "lng" to location.longitude,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            )
    }

    // ===============================
    // Stop Tracking
    // ===============================

    fun stopSOS() {

        Log.d("SOS", "SOS stopped by user")

        val context = getApplication<Application>()

        // Stop foreground location service
        val intent = Intent(context, ForegroundLocationService::class.java)
        context.stopService(intent)

        // Reset incident tracking
        currentIncidentId = ""
        lastSavedLocation = null

        // Reset UI state
        _location.value = null
        _locationHistory.value = emptyList()

        Log.d("SOS", "State cleared, ready for next SOS")
    }
}