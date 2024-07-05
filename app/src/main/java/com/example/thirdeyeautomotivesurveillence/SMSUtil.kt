package com.example.thirdeyeautomotivesurveillence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import java.io.File

object SMSUtil {
    fun sendTextMessage(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("SMSUtil", "SMS sent successfully to $phoneNumber")
        } catch (e: Exception) {
            Log.e("SMSUtil", "SMS failed to send", e)
        }
    }

    fun sendMMSMessage(context: Context, phoneNumber: String, message: String, imagePath: String) {
        try {
            val file = File(imagePath)
            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra("address", phoneNumber)
                putExtra("sms_body", message)
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            context.startActivity(intent)
            Log.d("SMSUtil", "MMS sent successfully to $phoneNumber")
        } catch (e: Exception) {
            Log.e("SMSUtil", "MMS failed to send", e)
        }
    }
}