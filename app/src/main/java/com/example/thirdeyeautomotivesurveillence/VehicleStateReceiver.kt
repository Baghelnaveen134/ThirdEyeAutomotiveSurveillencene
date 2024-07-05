package com.example.thirdeyeautomotivesurveillence


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

class VehicleStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.ACTION_VEHICLE_LOCKED" &&
            intent.getBooleanExtra("ENGINE_OFF", false)
        ) {
            val serviceIntent = Intent(context, SurveillanceService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } else if (intent.action == "com.example.ACTION_VEHICLE_UNLOCKED") {
            val serviceIntent = Intent(context, SurveillanceService::class.java)
            context.stopService(serviceIntent)
        }
    }
}