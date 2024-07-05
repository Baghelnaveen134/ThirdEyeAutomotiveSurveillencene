package com.example.thirdeyeautomotivesurveillence


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class CameraService : Service() {
    private val CHANNEL_ID = "CameraServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Camera Service")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.ic_camera)
            .build()
        startForeground(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Camera Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle service start
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}