package com.example.sos.location

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun LiveLocationMap(
    latitude: Double?,
    longitude: Double?,
    history: List<LocationData>
) {

    val cameraPositionState = rememberCameraPositionState()

    val userLocation =
        if (latitude != null && longitude != null)
            LatLng(latitude, longitude)
        else null

    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(it, 16f)
            )
        }
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        cameraPositionState = cameraPositionState
    ) {

        userLocation?.let {

            Marker(
                state = MarkerState(position = it),
                title = "Emergency Location",
                icon = BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_RED
                )
            )
        }

        if (history.isNotEmpty()) {
            Polyline(
                points = history.map {
                    LatLng(it.latitude, it.longitude)
                }
            )
        }
    }
}
