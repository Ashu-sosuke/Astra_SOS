package com.example.sos.modelCread

data class IncidentRequest(
    val incidentId: String,
    val userId: String,
    val phone: String,
    val audioUrl: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)


data class IncidentResponse(
    val incidentId: String,
    val transcript: String,
    val stressScore: Double,
    val threatType: String,
    val severityScore: Double,
    val finalSeverity: String,
    val confidence: Double,
    val recommendedAction: String
)