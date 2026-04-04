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

    companion object {
        const val EXTRA_SOS_MODE = "sos_mode"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_TRACKING = "sos_tracking_channel"
        private const val CHANNEL_SOS      = "sos_active_channel"
    }

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SOS:LocationWakeLock"
        )
        wakeLock.acquire()

        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isSosMode = intent?.getBooleanExtra(EXTRA_SOS_MODE, false) ?: false

        // Re-calling startForeground updates the notification if already running
        startForeground(
            NOTIFICATION_ID,
            if (isSosMode) buildSosNotification() else buildTrackingNotification()
        )

        // Only start location updates once — if callback already exists, skip
        if (locationCallback == null) {
            startLocationUpdates()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        if (wakeLock.isHeld) wakeLock.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification Channels ─────────────────────────────────────────────

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_TRACKING,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW  // silent, no sound
                ).apply {
                    description = "Background location access for map screen"
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SOS,
                    "SOS Active",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Active SOS emergency alert"
                }
            )
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────

    private fun buildTrackingNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_TRACKING)
            .setContentTitle("Location Active")
            .setContentText("AstraSOS is monitoring your location")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()

    private fun buildSosNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_SOS)
            .setContentTitle("🚨 SOS Active")
            .setContentText("Sharing live location with emergency contacts")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

    // ── Location Updates ──────────────────────────────────────────────────

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
                sendBroadcast(
                    Intent(ACTION_SOS_LOCATION_UPDATE).apply {
                        setPackage(packageName)
                        putExtra("latitude",  location.latitude)
                        putExtra("longitude", location.longitude)
                        putExtra("accuracy",  location.accuracy)
                    }
                )
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            mainLooper
        )
    }

    // ── Restart on task removal ───────────────────────────────────────────

    override fun onTaskRemoved(rootIntent: Intent?) {
        val pendingIntent = PendingIntent.getService(
            this, 1,
            Intent(applicationContext, ForegroundLocationService::class.java)
                .setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        (getSystemService(ALARM_SERVICE) as AlarmManager).set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 2000,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }
}