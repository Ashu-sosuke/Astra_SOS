package com.example.sos.modelCread

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*

class SosViewModel(private val context: Context) : ViewModel() {

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val api = RetrofitClient.api
    private val recorder = AudioRecorder(context)

    var activeIncidentId: String? = null
        private set

    fun triggerSos(
        incidentId: String,
        latitude: Double,
        longitude: Double,
        onComplete: () -> Unit
    ) {

        val user = auth.currentUser ?: return
        val userId = user.uid
        val phone = user.phoneNumber ?: "Unknown"

        viewModelScope.launch {

            try {

                activeIncidentId = incidentId

                Log.d("SOS", "Recording audio...")

                val audioFile = recorder.startRecording()

                delay(15000)

                recorder.stopRecording()

                val audioUrl = uploadAudioFile(audioFile, userId, incidentId)

                val request = IncidentRequest(
                    incidentId = incidentId,
                    userId = userId,
                    phone = phone,
                    audioUrl = audioUrl,
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = System.currentTimeMillis() / 1000
                )

                val response = api.processIncident(request)

                if (response.isSuccessful && response.body() != null) {

                    val result = response.body()!!

                    saveIncidentAIResult(
                        userId,
                        incidentId,
                        result
                    )
                }

            } catch (e: Exception) {

                Log.e("SOS", "SOS Flow failed", e)

            } finally {
                onComplete()
            }
        }
    }

    // ===============================
    // Upload Audio
    // ===============================

    private suspend fun uploadAudioFile(
        file: File,
        userId: String,
        incidentId: String
    ): String {

        val fileName = "audio_${System.currentTimeMillis()}.m4a"

        val storageRef = storage.reference
            .child("users/$userId/audio/$incidentId/$fileName")

        storageRef.putFile(file.toUri()).await()

        val downloadUrl = storageRef.downloadUrl.await().toString()

        saveAudioMetadata(userId, incidentId, downloadUrl)

        return downloadUrl
    }

    private suspend fun saveAudioMetadata(
        userId: String,
        incidentId: String,
        url: String
    ) {

        val data = mapOf(
            "url" to url,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(userId)
            .collection("incidents")
            .document(incidentId)
            .collection("media")
            .document("audio")
            .collection("files")
            .add(data)
            .await()
    }

    private suspend fun saveIncidentAIResult(
        userId: String,
        incidentId: String,
        response: IncidentResponse
    ) {

        val data = mapOf(
            "transcript" to response.transcript,
            "severityScore" to response.severityScore,
            "threatType" to response.threatType,
            "finalSeverity" to response.finalSeverity,
            "confidence" to response.confidence,
            "recommendedAction" to response.recommendedAction
        )

        firestore.collection("users")
            .document(userId)
            .collection("incidents")
            .document(incidentId)
            .update(data)
            .await()
    }
}