package com.example.sos.modelCread

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AiApiService {

    @POST("process-incident")
    suspend fun processIncident(
        @Body request: IncidentRequest
    ): Response<IncidentResponse>
}

