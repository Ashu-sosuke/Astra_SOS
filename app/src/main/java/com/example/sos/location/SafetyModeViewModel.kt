package com.example.sos.location

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sos.location.data.CrimeDatabase
import com.example.sos.location.data.CrimeIncident
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)

class SafetyModeViewModel(application: Application) : AndroidViewModel(application) {

    // ── Crime incidents ──────────────────────────────────────────────────
    private val _incidents = MutableLiveData<List<CrimeIncident>>()
    val incidents: LiveData<List<CrimeIncident>> = _incidents

    private val _isLoadingIncidents = MutableStateFlow(false)
    val isLoadingIncidents: StateFlow<Boolean> = _isLoadingIncidents

    // ── Room DB ──────────────────────────────────────────────────────────
    private val db = CrimeDatabase.getInstance(application)

    // ── Firebase ─────────────────────────────────────────────────────────
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── SOS state ────────────────────────────────────────────────────────
    private var currentIncidentId: String = ""
    private var lastSavedLocation: LocationData? = null
    private var receiverRegistered = false

    private val MIN_DISTANCE_METERS = 10
    private val MAX_ACCURACY_METERS = 30

    private val _location = MutableStateFlow<LocationData?>(null)
    val location: StateFlow<LocationData?> = _location

    private val _locationHistory = MutableStateFlow<List<LocationData>>(emptyList())
    val locationHistory: StateFlow<List<LocationData>> = _locationHistory

    private val _isSosActive = MutableStateFlow(false)
    val isSosActive: StateFlow<Boolean> = _isSosActive

    // ── BroadcastReceiver ────────────────────────────────────────────────
    // ACTION_SOS_LOCATION_UPDATE comes from your existing constants file
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_SOS_LOCATION_UPDATE) return
            val lat      = intent.getDoubleExtra("latitude", 0.0)
            val lng      = intent.getDoubleExtra("longitude", 0.0)
            val accuracy = intent.getFloatExtra("accuracy", 0f)
            Log.d("SOS_VM", "Broadcast received: $lat, $lng")
            processLocationUpdate(LocationData(lat, lng, accuracy))
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SOS Controls
    // ════════════════════════════════════════════════════════════════════

    fun startSOS(): String {
        _isSosActive.value = true
        val incidentId = System.currentTimeMillis().toString()
        currentIncidentId = incidentId
        lastSavedLocation = null

        val context = getApplication<Application>().applicationContext

        // Guard against double registration
        if (!receiverRegistered) {
            val filter = IntentFilter(ACTION_SOS_LOCATION_UPDATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                ContextCompat.registerReceiver(
                    context,
                    locationReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
            receiverRegistered = true
        }

        ContextCompat.startForegroundService(
            context,
            Intent(context, ForegroundLocationService::class.java)
        )

        Log.d("SOS", "Incident started: $incidentId")
        return incidentId
    }

    fun stopSOS() {
        _isSosActive.value = false
        val context = getApplication<Application>()

        if (receiverRegistered) {
            try {
                context.unregisterReceiver(locationReceiver)
            } catch (e: IllegalArgumentException) {
                Log.d("SOS", "Receiver already unregistered")
            }
            receiverRegistered = false
        }

        context.stopService(Intent(context, ForegroundLocationService::class.java))
        currentIncidentId = ""
        lastSavedLocation = null
        _location.value = null
        _locationHistory.value = emptyList()

        Log.d("SOS", "SOS stopped, state cleared")
    }

    // ════════════════════════════════════════════════════════════════════
    // Location Processing
    // ════════════════════════════════════════════════════════════════════

    fun processLocationUpdate(location: LocationData) {
        // Always update the UI dot, even without an active incident
        _location.value = location

        // Only persist/upload during an active SOS
        if (currentIncidentId.isEmpty()) return
        if (location.accuracy > MAX_ACCURACY_METERS) return
        if (!shouldSaveLocation(location)) return

        lastSavedLocation = location
        _locationHistory.value = _locationHistory.value + location
        uploadLocationToFirebase(location)
    }

    private fun shouldSaveLocation(newLocation: LocationData): Boolean {
        val last = lastSavedLocation ?: return true
        val results = FloatArray(1)
        Location.distanceBetween(
            last.latitude, last.longitude,
            newLocation.latitude, newLocation.longitude,
            results
        )
        Log.d("SOS", "Distance moved: ${results[0]}m")
        return results[0] > MIN_DISTANCE_METERS
    }

    // ════════════════════════════════════════════════════════════════════
    // Firebase Upload (live SOS location)
    // ════════════════════════════════════════════════════════════════════

    private fun uploadLocationToFirebase(location: LocationData) {
        val userId = auth.currentUser?.uid ?: return

        val incidentRef = firestore
            .collection("users").document(userId)
            .collection("incidents").document(currentIncidentId)

        val locationData = hashMapOf(
            "lat"       to location.latitude,
            "lng"       to location.longitude,
            "accuracy"  to location.accuracy,
            "timestamp" to FieldValue.serverTimestamp()
        )

        incidentRef.set(mapOf("lastLocation" to locationData), SetOptions.merge())
        incidentRef.collection("locationHistory").add(locationData)

        firestore.collection("live_locations")
            .document(currentIncidentId)
            .set(mapOf(
                "lat"       to location.latitude,
                "lng"       to location.longitude,
                "timestamp" to FieldValue.serverTimestamp()
            ))

        Log.d("SOS", "Location saved to Firestore")
    }

    // ════════════════════════════════════════════════════════════════════
    // Crime Incidents — Firestore (primary)
    // ════════════════════════════════════════════════════════════════════

    @Suppress("DEPRECATION")
    suspend fun getLatLngFromCity(context: Context, city: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context)
                val query = "$city, India"

                val addresses = geocoder.getFromLocationName(query, 1)

                if (!addresses.isNullOrEmpty()) {
                    val loc = addresses[0]
                    Log.d("GEOCODER", "$city -> ${loc.latitude}, ${loc.longitude}")
                    Pair(loc.latitude, loc.longitude)
                } else {
                    Log.e("GEOCODER", "No result for $city")
                    null
                }
            } catch (e: Exception) {
                Log.e("GEOCODER", "Failed for $city: ${e.message}")
                null
            }
        }
    }

    fun loadIncidentsFromFirestore() {
        _isLoadingIncidents.value = true

        viewModelScope.launch {
            try {
                val snapshot = firestore
                    .collection("crime_incidents")
                    .get()
                    .await()

                val result = mutableListOf<CrimeIncident>()

                for (doc in snapshot.documents) {
                    try {
                        val city = doc.getString("city") ?: ""

                        val lat = doc.getDouble("latitude")
                        val lng = doc.getDouble("longitude")

                        if (lat == null || lng == null) {
                            Log.e("MAP", "Skipping $city — no coords in doc")
                            continue
                        }

                        val incident = CrimeIncident(
                            title       = doc.getString("summary") ?: "",
                            description = doc.getString("summary") ?: "",
                            category    = doc.getString("category") ?: "Other",
                            date        = doc.getString("date") ?: "",
                            city        = city,
                            state       = doc.getString("state") ?: "",
                            severity    = (doc.getLong("severity") ?: 5L).toInt(),
                            victimCount = (doc.getLong("victimCount") ?: 1L).toInt(),
                            latitude    = lat,
                            longitude   = lng,
                            source      = "Firestore"
                        )

                        result.add(incident)

                    } catch (e: Exception) {
                        Log.e("FIRESTORE", "Parse error: ${e.message}")
                    }
                }

                _incidents.value = result
                _isLoadingIncidents.value = false
                db.crimeDao().insertAll(result)

            } catch (e: Exception) {
                Log.e("FIRESTORE", "Fetch failed: ${e.message}")
                _isLoadingIncidents.value = false
                loadIncidentsFromRoom()
            }
        }
    }


    // Crime Incidents — Room (offline fallback)
    fun loadIncidentsFromRoom() {
        viewModelScope.launch {
            val data = db.crimeDao().getAllIncidents()   // ✅ correct method name
            Log.d("SOS_VM", "Loaded ${data.size} incidents from Room")
            _incidents.value = data
        }
    }

    // ════════════════════════════════════════════════════════════════════
// Passive Location Tracking (map screen — no SOS incident)
// ════════════════════════════════════════════════════════════════════

    fun startLocationTracking() {
        val context = getApplication<Application>().applicationContext

        if (!receiverRegistered) {
            val filter = IntentFilter(ACTION_SOS_LOCATION_UPDATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                ContextCompat.registerReceiver(
                    context,
                    locationReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
            receiverRegistered = true
        }

        ContextCompat.startForegroundService(
            context,
            Intent(context, ForegroundLocationService::class.java)
        )

        Log.d("SOS_VM", "Passive location tracking started")
    }



    // ════════════════════════════════════════════════════════════════════
    // Cleanup
    // ════════════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        if (_isSosActive.value) stopSOS()
    }
}