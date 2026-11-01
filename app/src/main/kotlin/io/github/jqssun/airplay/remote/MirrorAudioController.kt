package io.github.jqssun.airplay.remote

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import io.github.jqssun.airplay.renderer.AudioRenderer
import org.json.JSONObject

/** Main-thread audio policy. Never mutes or changes the shared TV volume. */
class MirrorAudioController(
    context: Context,
    private val renderer: AudioRenderer,
    private val beforeOutput: () -> Unit = {},
) {
    private val prefs = context.getSharedPreferences("mirror_audio", Context.MODE_PRIVATE)
    private var volume = prefs.getInt("volume", 100).coerceIn(0, 100)
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var requested = "tv"
    private var mirrorActive = false
    private var format: Pair<Int, Int>? = null
    private var focusHeld = false
    private var focusRequested = false
    private var outputActive = false
    private var message = "正在播放电视原声"
    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        // Ignore queued callbacks from a focus request that a mode change has abandoned.
        if (requested != "mirror") return@OnAudioFocusChangeListener
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                focusHeld = true
                if (requested == "mirror" && mirrorActive && format != null) startOutput()
                else releaseFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS -> select("tv")
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                focusHeld = false
                stopOutput()
                message = "声音被其他应用暂时占用"
            }
        }
    }
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setAcceptsDelayedFocusGain(false)
        .setOnAudioFocusChangeListener(listener, Handler(Looper.getMainLooper()))
        .build()

    init { renderer.setClientGain(volume / 100f) }

    fun setVolume(value: Int) {
        require(value in 0..100)
        volume = value
        // Per-receiver PCM gain; never writes AudioManager's shared music-stream volume.
        renderer.setClientGain(volume / 100f)
        prefs.edit().putInt("volume", volume).apply()
    }

    fun select(source: String) {
        require(source in setOf("tv", "mirror", "mixed"))
        requested = source
        refresh()
    }

    fun onMirrorActive() {
        mirrorActive = true
        refresh()
    }

    fun onFormat(ct: Int, spf: Int) {
        // Some senders omit usingScreen during negotiation. Only a real video session may play.
        if (ct !in setOf(2, 4, 8) || spf <= 0) return
        format = ct to spf
        renderer.setFormat(ct, spf)
        refresh()
    }

    fun endSession() {
        mirrorActive = false
        format = null
        requested = "tv"
        refresh()
    }

    private fun refresh() {
        if (requested == "tv" || !mirrorActive || format == null) {
            stopOutput()
            releaseFocus()
            message = when {
                requested == "tv" -> "正在播放电视原声"
                !mirrorActive -> if (requested == "mixed") "连接投屏后混合播放" else "连接投屏后切换声音"
                else -> "等待手机发送音频"
            }
            return
        }
        if (requested == "mixed") {
            // Explicit simultaneous-playback mode on this TV: preserve the HDMI player's
            // focus and allow the HAL/mixer to combine its path with our shared AAudio stream.
            releaseFocus()
            startOutput()
            return
        }
        if (focusRequested && !focusHeld) {
            message = "声音被其他应用暂时占用"
            return
        }
        if (!focusHeld) {
            focusRequested = manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            focusHeld = focusRequested
        }
        if (!focusHeld) {
            requested = "tv"
            message = "电视暂未允许切换声音，请重试"
            return
        }
        startOutput()
    }

    private fun startOutput() {
        if (!outputActive) {
            beforeOutput()
            format?.let { renderer.setFormat(it.first, it.second) }
            renderer.setClientGain(volume / 100f)
            outputActive = renderer.start()
        }
        if (outputActive) {
            message = if (requested == "mixed") "电视原声与投屏声音同时播放" else "正在播放投屏声音"
        } else {
            requested = "tv"
            renderer.stop() // Clear the native requested-running flag even after open failure.
            releaseFocus()
            message = "投屏音频启动失败，已保留电视原声"
        }
    }

    private fun stopOutput() {
        renderer.stop()
        outputActive = false
    }

    private fun releaseFocus() {
        val wasRequested = focusRequested
        focusRequested = false
        focusHeld = false
        if (wasRequested) manager.abandonAudioFocusRequest(focus)
    }

    fun state(): JSONObject = JSONObject().apply {
        put("source", requested)
        put("volume", volume)
        put("gain", renderer.clientGain.toDouble())
        put("outputActive", outputActive)
        put("focusHeld", focusHeld)
        put("formatReady", format != null)
        put("message", message)
    }
}
