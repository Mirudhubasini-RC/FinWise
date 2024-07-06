package com.finwise.finwise_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("label")
        val description = intent.getStringExtra("description")
        Log.d("AlarmReceiver", "Received Label: $label, Description: $description")

        val serviceIntent = Intent(context, AlarmService::class.java).apply{
            putExtra("label", label)
            putExtra("description", description)
        }
        context.startForegroundService(serviceIntent)
    }

}