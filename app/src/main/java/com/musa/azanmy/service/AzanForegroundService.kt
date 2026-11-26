package com.musa.azanmy.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.VolumeProviderCompat
import com.musa.azanmy.MainActivity
import com.musa.azanmy.R
import com.musa.azanmy.alarm.AzanScheduler
import com.musa.azanmy.data.AzanAudioCatalog
import com.musa.azanmy.data.JakimZoneData
import com.musa.azanmy.data.PrayerName
import com.musa.azanmy.data.PrayerPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AzanForegroundService : Service() {

    companion object {
        const val CHANNEL_ID_ALARM = "azan_alarm_channel"
        const val NOTIF_ID_ALARM = 2001

        const val ACTION_START_AZAN = "com.musa.azanmy.action.START_AZAN"
        const val ACTION_STOP_AZAN = "com.musa.azanmy.action.STOP_AZAN"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"

        fun startAzan(context: Context, prayer: PrayerName) {
            val intent = Intent(context, AzanForegroundService::class.java).apply {
                action = ACTION_START_AZAN
                putExtra(EXTRA_PRAYER_NAME, prayer.name)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopAzan(context: Context) {
            val intent = Intent(context, AzanForegroundService::class.java).apply {
                action = ACTION_STOP_AZAN
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaSession: MediaSessionCompat? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        setupMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_AZAN) {
            stopAzanAndSelf()
            return START_NOT_STICKY
        }

        val prayer = PrayerName.fromName(intent?.getStringExtra(EXTRA_PRAYER_NAME))
            ?: PrayerName.ZOHOR

        val initialNotif = buildAlarmNotification(
            prayer = prayer,
            subtitle = "Sedang melaungkan azan waktu ${prayer.displayName}..."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID_ALARM,
                initialNotif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIF_ID_ALARM, initialNotif)
        }

        serviceScope.launch {
            val settings = PrayerPrefs(applicationContext).getSettings()
            val zone = JakimZoneData.findByCode(settings.zoneCode)
            val rawName = AzanAudioCatalog.getRawNameForPrayer(prayer, settings.selectedDailyAzanId)

            updateMediaMetadata(prayer, zone.shortTitle)

            val updatedNotif = buildAlarmNotification(
                prayer = prayer,
                subtitle = "Masuk waktu ${prayer.displayName} • ${zone.shortTitle}"
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIF_ID_ALARM,
                    updatedNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIF_ID_ALARM, updatedNotif)
            }

            val started = AzanAudioPlayer.playPrayerAzan(
                context = applicationContext,
                rawName = rawName,
                onFinished = {
                    stopSelf()
                }
            )

            if (!started) {
                PrayerStatusNotification.showPrayerAlertBanner(applicationContext, prayer)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun setupMediaSession() {
        val session = MediaSessionCompat(this, "AzanMYMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onStop() {
                    stopAzanAndSelf()
                }

                override fun onPause() {
                    stopAzanAndSelf()
                }
            })

            val volumeProvider = object : VolumeProviderCompat(
                VOLUME_CONTROL_RELATIVE,
                100,
                50
            ) {
                override fun onAdjustVolume(direction: Int) {
                    if (direction != 0) {
                        stopAzanAndSelf()
                    }
                }
            }
            setPlaybackToRemote(volumeProvider)

            val state = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
                .setState(PlaybackStateCompat.STATE_PLAYING, 0L, 1.0f)
                .build()
            setPlaybackState(state)
            isActive = true
        }
        mediaSession = session
    }

    private fun updateMediaMetadata(prayer: PrayerName, zoneTitle: String) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Azan ${prayer.displayName}")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "AzanMY • $zoneTitle")
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun buildAlarmNotification(
        prayer: PrayerName,
        subtitle: String
    ) = NotificationCompat.Builder(this, CHANNEL_ID_ALARM)
        .setSmallIcon(R.drawable.ic_notif_mosque)
        .setContentTitle("Azan ${prayer.displayName}")
        .setContentText(subtitle)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setContentIntent(createOpenAppPendingIntent())
        .addAction(
            android.R.drawable.ic_media_pause,
            "Hentikan Azan",
            createStopPendingIntent()
        )
        .build()

    private fun createOpenAppPendingIntent(): PendingIntent {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            2010,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createStopPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, AzanForegroundService::class.java).apply {
            action = ACTION_STOP_AZAN
        }
        return PendingIntent.getService(
            this,
            2011,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopAzanAndSelf() {
        AzanAudioPlayer.stopAll(invokeCallback = false)
        stopSelf()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AzanMY::AzanPlaybackWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(6 * 60 * 1000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        } finally {
            wakeLock = null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        serviceScope.launch {
            AzanScheduler.scheduleNext(applicationContext)
        }
    }

    override fun onDestroy() {
        AzanAudioPlayer.stopAll(invokeCallback = false)
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
        } catch (_: Exception) {
        } finally {
            mediaSession = null
        }
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }
}
