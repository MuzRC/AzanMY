package com.musa.azanmy.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.musa.azanmy.data.AzanAudioCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AzanAudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var activeAudioManager: AudioManager? = null
    private var onFinishedCallback: (() -> Unit)? = null
    private var isActualPrayerActive: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var fallbackStopRunnable: Runnable? = null

    private val _playingPreviewRawName = MutableStateFlow<String?>(null)
    val playingPreviewRawName: StateFlow<String?> = _playingPreviewRawName.asStateFlow()

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                stopAll(invokeCallback = true)
            }
        }
    }

    fun isCallInProgress(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun playPrayerAzan(
        context: Context,
        rawName: String,
        onFinished: () -> Unit
    ): Boolean {
        if (isCallInProgress(context)) {
            stopAll(invokeCallback = false)
            return false
        }

        stopAll(invokeCallback = false)
        isActualPrayerActive = true
        onFinishedCallback = onFinished

        return startPlaybackInternal(
            context = context.applicationContext,
            rawName = rawName,
            isPreview = false
        )
    }

    @Synchronized
    fun togglePreview(context: Context, rawName: String) {
        if (isActualPrayerActive) return

        val currentlyPlaying = _playingPreviewRawName.value
        stopAll(invokeCallback = false)

        if (currentlyPlaying == rawName) {
            return
        }

        _playingPreviewRawName.value = rawName
        startPlaybackInternal(
            context = context.applicationContext,
            rawName = rawName,
            isPreview = true
        )
    }

    private fun startPlaybackInternal(
        context: Context,
        rawName: String,
        isPreview: Boolean
    ): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        activeAudioManager = am

        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (am != null) {
            val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(alarmAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            audioFocusRequest = focusReq
            val res = am.requestAudioFocus(focusReq)
            if (res != AudioManager.AUDIOFOCUS_REQUEST_GRANTED && !isPreview) {
                if (isCallInProgress(context)) {
                    stopAll(invokeCallback = false)
                    return false
                }
            }
        }

        return try {
            val resId = AzanAudioCatalog.resolveRawResId(context, rawName)
            val player = MediaPlayer()
            player.setAudioAttributes(alarmAttributes)

            if (resId != 0) {
                val afd = context.resources.openRawResourceFd(resId)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                player.isLooping = false
            } else {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                player.setDataSource(context, fallbackUri)
                player.isLooping = true
                scheduleFallbackAutoStop(if (isPreview) 6_000L else 18_000L)
            }

            player.setOnCompletionListener {
                stopAll(invokeCallback = true)
            }

            player.setOnErrorListener { _, _, _ ->
                stopAll(invokeCallback = true)
                true
            }

            player.prepare()
            player.start()
            mediaPlayer = player
            true
        } catch (_: Exception) {
            stopAll(invokeCallback = true)
            false
        }
    }

    private fun scheduleFallbackAutoStop(delayMs: Long) {
        cancelFallbackAutoStop()
        val runnable = Runnable {
            stopAll(invokeCallback = true)
        }
        fallbackStopRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun cancelFallbackAutoStop() {
        fallbackStopRunnable?.let { mainHandler.removeCallbacks(it) }
        fallbackStopRunnable = null
    }

    @Synchronized
    fun stopAll(invokeCallback: Boolean = false) {
        cancelFallbackAutoStop()

        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }

        try {
            val am = activeAudioManager
            val req = audioFocusRequest
            if (am != null && req != null) {
                am.abandonAudioFocusRequest(req)
            }
        } catch (_: Exception) {
        } finally {
            audioFocusRequest = null
            activeAudioManager = null
        }

        _playingPreviewRawName.value = null
        isActualPrayerActive = false

        val callback = onFinishedCallback
        onFinishedCallback = null
        if (invokeCallback) {
            callback?.invoke()
        }
    }

    fun vibrateThreeTimes(context: Context) {
        try {
            val pattern = longArrayOf(0L, 450L, 250L, 450L, 250L, 650L)
            val effect = VibrationEffect.createWaveform(pattern, -1)

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val vibAttrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build()
                vibrator?.vibrate(effect, vibAttrs)
            } else {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vibrator?.vibrate(effect, audioAttrs)
            }
        } catch (_: Exception) {
        }
    }
}
