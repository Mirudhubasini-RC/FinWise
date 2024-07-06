package com.finwise.finwise_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.finwise.finwise_app.R
import android.content.Context
import android.media.MediaPlayer

class AlarmService : Service() {

    private lateinit var ringtone: Ringtone


    override fun onCreate() {
        super.onCreate()
        // Create a ringtone from default alarm sound
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(applicationContext, defaultSoundUri)
        ringtone.isLooping = true
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            // Stop the alarm and stop the service
            stopAlarm()
            stopSelf()
        } else {
            val label = intent?.getStringExtra("label") ?: "Default Label"
            val description = intent?.getStringExtra("description") ?: "Default Description"

            // Start the alarm and show foreground notification
            createNotificationChannel()
            val notification = buildNotification(label,description)
            startForeground(NOTIFICATION_ID, notification)
            ringtone.play()
        }
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        // Stop the ringtone when service is destroyed
        stopAlarm()
    }
    private fun stopAlarm() {
        if (ringtone.isPlaying) {
            ringtone.stop()
        }
        // Remove the foreground notification and stop the service
        stopForeground(true)
        stopSelf()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun buildNotification(label :String, description: String): Notification {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "com.example.finwise_app.STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(label)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_alarm)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .build()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val CHANNEL_ID = "AlarmChannel"
        private const val NOTIFICATION_ID = 12345
        private const val ACTION_STOP_ALARM = "com.example.finwise_app.STOP_ALARM"
        private lateinit var mediaPlayer: MediaPlayer
    }

}
