package com.telvique.phone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.telvique.phone.data.SettingsRepository
import com.telvique.phone.telecom.CallNotificationHelper

class TelviqueApp : Application() {

    companion object {
        lateinit var instance: TelviqueApp
            private set
    }

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val incomingChannel = NotificationChannel(
            CallNotificationHelper.CHANNEL_INCOMING,
            getString(R.string.channel_incoming_call),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming phone call alerts"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val ongoingChannel = NotificationChannel(
            CallNotificationHelper.CHANNEL_ONGOING,
            getString(R.string.channel_ongoing_call),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Active and ongoing call status"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val missedChannel = NotificationChannel(
            CallNotificationHelper.CHANNEL_MISSED,
            getString(R.string.channel_missed_call),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Missed phone call alerts"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(incomingChannel)
        manager.createNotificationChannel(ongoingChannel)
        manager.createNotificationChannel(missedChannel)
    }
}
