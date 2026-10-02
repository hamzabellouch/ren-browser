package com.tkno.ren.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tkno.ren.MainActivity
import com.tkno.ren.R

object IncognitoNotificationManager {

    const val CHANNEL_ID = "ren_incognito_channel"
    const val NOTIFICATION_ID = 4201
    const val ACTION_CLOSE_INCOGNITO = "com.tkno.ren.action.CLOSE_INCOGNITO"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Incognito Mode"
            val channelDescription = "Ongoing notification shown while browsing in Incognito mode"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, incognitoTabCount: Int) {
        if (incognitoTabCount <= 0) {
            cancelNotification(context)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                return
            }
        }

        initChannel(context)

        val closeIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_CLOSE_INCOGNITO
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val closePendingIntent = PendingIntent.getActivity(context, 101, closeIntent, flags)

        val tabText = if (incognitoTabCount == 1) {
            "1 incognito tab open"
        } else {
            "$incognitoTabCount incognito tabs open"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_incognito_glasses)
            .setContentTitle("Ren Browser")
            .setContentText(tabText)
            .setSubText("Incognito")
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(closePendingIntent)
            .addAction(
                R.drawable.ic_incognito_glasses,
                "Close all incognito tabs",
                closePendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted or notification error
        } catch (e: Exception) {
            // Safeguard against any system notification error
        }
    }

    fun cancelNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
