package io.github.jqssun.airplay.floating

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.github.jqssun.airplay.service.AirPlayService

/** TV-friendly controls stay separate from the non-focusable floating window. */
class FloatControlActivity : Activity() {
    private var service: AirPlayService? = null
    private var bound = false
    private var editing = false
    private lateinit var status: TextView
    private lateinit var edit: Button
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            val svc = service
            val permission = if (Settings.canDrawOverlays(this@FloatControlActivity)) "已允许" else "未允许"
            val receiving = when (svc?.serverState?.value) {
                AirPlayService.ServerState.RUNNING -> "运行中"
                AirPlayService.ServerState.ERROR -> "启动失败"
                else -> "未启动"
            }
            status.text = "悬浮窗：$permission　接收：$receiving\n" +
                "镜像名称：电视投屏小窗\n" +
                (if ((svc?.connectionCount?.value ?: 0) > 0) "手机已连接 · ${svc?.videoResolution?.value}" else "等待同一局域网内的 iPhone") +
                "\n${if (editing) "调整中：方向键移动；返回键完成调整" else "第一版固定静音，综艺继续使用原来的声音"}"
            handler.postDelayed(this, 500)
        }
    }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as? AirPlayService.LocalBinder)?.service
        }
        override fun onServiceDisconnected(name: ComponentName?) { service = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            setBackgroundColor(Color.rgb(20, 25, 36))
        }
        layout.addView(TextView(this).apply {
            text = "AirPlay 投屏小窗"; textSize = 27f; setTextColor(Color.WHITE)
        })
        status = TextView(this).apply { textSize = 17f; setTextColor(Color.LTGRAY); setPadding(0, 12, 0, 12) }
        layout.addView(status)
        fun button(text: String, action: () -> Unit): Button = Button(this).apply {
            this.text = text
            setOnClickListener { action() }
            layout.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        button("1. 允许显示在其他应用上层") {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }
        val start = button("2. 开启投屏小窗") {
            if (!Settings.canDrawOverlays(this)) {
                android.widget.Toast.makeText(this, "请先允许悬浮窗权限", android.widget.Toast.LENGTH_LONG).show()
            } else {
                ContextCompat.startForegroundService(this,
                    Intent(this, AirPlayService::class.java).setAction(AirPlayService.ACTION_START_SERVER))
            }
        }
        edit = button("调整小窗位置（方向键移动）") { editing = true }
        button("缩小小窗") { service?.resizeFloating(0.85f) }
        button("放大小窗") { service?.resizeFloating(1.18f) }
        button("小窗恢复角落大小") { service?.compactFloating() }
        button("返回电视节目，保持接收") { finish() }
        button("停止接收并关闭小窗") { service?.stopServer() }
        setContentView(ScrollView(this).apply { addView(layout) })
        start.requestFocus()
        bound = bindService(Intent(this, AirPlayService::class.java), connection, BIND_AUTO_CREATE)
    }

    override fun onResume() { super.onResume(); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (editing) {
            val direction = when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> -24 to 0
                KeyEvent.KEYCODE_DPAD_RIGHT -> 24 to 0
                KeyEvent.KEYCODE_DPAD_UP -> 0 to -24
                KeyEvent.KEYCODE_DPAD_DOWN -> 0 to 24
                else -> null
            }
            if (direction != null) {
                if (event.action == KeyEvent.ACTION_DOWN) service?.moveFloating(direction.first, direction.second)
                return true
            }
            if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (event.action == KeyEvent.ACTION_UP) { editing = false; edit.requestFocus() }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        if (bound) unbindService(connection)
        service = null
        super.onDestroy()
    }
}
