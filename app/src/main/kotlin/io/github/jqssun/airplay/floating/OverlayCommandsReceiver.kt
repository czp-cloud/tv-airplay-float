package io.github.jqssun.airplay.floating

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.jqssun.airplay.service.AirPlayService
import org.json.JSONObject

/** Diagnostic controls restricted by the system DUMP permission in the manifest. */
class OverlayCommandsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = AirPlayService.localInstance
        when (intent.getStringExtra("command")) {
            "start" -> ContextCompat.startForegroundService(context,
                Intent(context, AirPlayService::class.java).setAction(AirPlayService.ACTION_START_SERVER))
            "stop" -> service?.stopServer()
            "move" -> service?.moveFloating(intent.getIntExtra("dx", 0), intent.getIntExtra("dy", 0))
            "resize" -> {
                val scale = intent.getFloatExtra("scale", 1f)
                if (scale.isFinite()) service?.resizeFloating(scale)
            }
            "opacity" -> service?.setFloatingOpacity(intent.getFloatExtra("opacity", 1f))
            "compact" -> service?.compactFloating()
            "audio-devices" -> {
                setResult(Activity.RESULT_OK, service?.audioDevices()?.toString() ?: "{}", null)
                return
            }
            "web" -> {
                setResult(Activity.RESULT_OK, service?.remoteInfo()?.toString() ?: "{}", null)
                return
            }
            "state" -> Unit
            else -> { setResultCode(Activity.RESULT_CANCELED); return }
        }
        val state = JSONObject().apply {
            put("server", service?.serverState?.value?.name ?: "STOPPED")
            put("overlay", service?.floatingState() ?: JSONObject().put("visible", false))
            put("connections", service?.connectionCount?.value ?: 0)
            put("mirroring", service?.mirroringActive?.value ?: false)
            put("inputFrames", service?.videoRenderer?.frameCount ?: 0)
            put("codec", service?.videoRenderer?.codecName ?: "")
            put("audio", service?.remoteState()?.optJSONObject("audio"))
            put("audioOutput", service?.remoteState()?.optJSONObject("audio")?.optBoolean("outputActive") ?: false)
        }
        setResult(Activity.RESULT_OK, state.toString(), null)
    }
}
