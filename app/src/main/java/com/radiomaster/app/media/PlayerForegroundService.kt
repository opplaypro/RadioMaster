package com.radiomaster.app.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.presentation.App
import com.radiomaster.app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class PlayerForegroundService : Service() {

    const val ACTION_TOGGLE_PLAYBACK = "com.radiomaster.app.action.TOGGLE_PLAYBACK"
    const val ACTION_STOP = "com.radiomaster.app.action.STOP"

    private const val CHANNEL_ID = "radiomaster_playback"
    private const val NOTIFICATION_ID = 1

    @Inject lateinit var playerManager: RadioPlayerManager

    private lateinit var notificationManager: NotificationManagerCompat

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_TOGGLE_PLAYBACK -> playerManager.toggle()
            ACTION_STOP -> playerManager.stop()
        }
        updateNotification()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        playerManager.stop()
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    fun updateNotification() {
        val station = playerManager.currentStation.value
        val context = applicationContext
        val channelId = CHANNEL_ID
        val res = context.resources

        val contentIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = Intent(context, PlayerForegroundService::class.java).apply {
            action = ACTION_TOGGLE_PLAYBACK
        }
        val togglePending = PendingIntent.getService(
            context, 1, toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(context, PlayerForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            context, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_radio)
            .setColor(context.getColor(R.color.radio_primary))
            .setContentIntent(contentIntent)
            .setOngoing(playerManager.isPlaying)
            .setOnlyAlertOnce(true)
            .setWhen(System.currentTimeMillis())

        if (station != null) {
            builder.setContentTitle(station.name)
            builder.setContentText(station.country ?: res.getString(R.string.app_name))
            val toggleLabel = if (playerManager.isPlaying) res.getString(R.string.station_pause) else res.getString(R.string.station_play)
            val toggleIcon = if (playerManager.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            builder.addAction(toggleIcon, toggleLabel, togglePending)
        } else {
            builder.setContentTitle(res.getString(R.string.app_name))
            builder.setContentText(res.getString(R.string.player_ready))
        }

        builder.addAction(android.R.drawable.ic_media_stop, res.getString(R.string.notification_cancel), stopPending)

        val notification = builder.build()
        if (playerManager.isPlaying || station != null) {
            startForeground(NOTIFICATION_ID, notification)
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
