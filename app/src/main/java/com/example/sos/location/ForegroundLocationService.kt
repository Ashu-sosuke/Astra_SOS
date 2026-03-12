package com.example.sos.location

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class ForegroundLocationService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager

        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SOS:LocationWakeLock"
        )

        wakeLock.acquire()
    }

    override fun onDestroy() {
        super.onDestroy()

        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(1, createNotification())

        startLocationUpdates()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {

        val channelId = "sos_location_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "SOS Live Location",
                NotificationManager.IMPORTANCE_HIGH
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SOS Active")
            .setContentText("Sharing live location")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {

        if (checkSelfPermission(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        )
            .setMinUpdateIntervalMillis(3000)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation ?: return

                val broadcastIntent =
                    Intent(ACTION_SOS_LOCATION_UPDATE).apply {

                        setPackage(packageName)

                        putExtra("latitude", location.latitude)
                        putExtra("longitude", location.longitude)
                        putExtra("accuracy", location.accuracy)
                    }

                sendBroadcast(broadcastIntent)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            mainLooper
        )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {

        val restartIntent =
            Intent(applicationContext, ForegroundLocationService::class.java)

        restartIntent.setPackage(packageName)

        val pendingIntent = PendingIntent.getService(
            this,
            1,
            restartIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 2000,
            pendingIntent
        )

        super.onTaskRemoved(rootIntent)
    }
}