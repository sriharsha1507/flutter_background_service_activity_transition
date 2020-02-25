package com.developergunda.android_activity_transition.notification.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
object NotificationChannelService {
    fun createChannel(context: Context) {
        val notificationChannel =
                NotificationChannel("sriharsha","sriharsha", NotificationManager.IMPORTANCE_DEFAULT)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(notificationChannel)
    }
}

