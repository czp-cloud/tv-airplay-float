package io.github.jqssun.airplay.floating

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.FrameLayout
import org.json.JSONObject
import kotlin.math.roundToInt

/** Display surface only; listening and decoding do not require a visible window. Main thread only. */
class MirrorOverlay(
    private val context: Context,
    private val attach: (Surface) -> Unit,
    private val detach: (Surface) -> Unit,
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = context.getSharedPreferences("floating_window", Context.MODE_PRIVATE)
    private var root: FrameLayout? = null
    private var surface: Surface? = null
    private var videoSize = ""
    private var surfaceUpdates = 0L
    private val density = context.resources.displayMetrics.density
    private fun dp(n: Int) = (n * density).roundToInt()
    private fun screen() = Point().also { wm.defaultDisplay.getRealSize(it) }
    private var aspect = prefs.getString("aspect_ratio", null)?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 } ?: (16.0 / 9.0)
    // Migrate the old landscape width without overwriting it when portrait must fit the screen.
    private var preferredLongEdge = prefs.getInt("long_edge", prefs.getInt("width", screen().x / 6))
    private fun geometry(): OverlayGeometry {
        val s = screen()
        return OverlayGeometry(s.x, s.y, aspect, dp(120))
    }

    // Migrate the former translucent debug window once, preserving its size and position.
    init {
        if (!prefs.getBoolean("frameless_v1", false)) {
            prefs.edit().putFloat("opacity", 1f).putBoolean("frameless_v1", true).apply()
        }
    }
    private val params: WindowManager.LayoutParams by lazy {
        val s = screen()
        val (width, height) = geometry().size(preferredLongEdge)
        WindowManager.LayoutParams(
            width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = prefs.getInt("x", s.x - width - dp(8))
            y = prefs.getInt("y", dp(8))
            title = "AirPlayMirrorOverlay"
            alpha = prefs.getFloat("opacity", 1f).coerceIn(0.12f, 1f)
        }
    }

    fun show(): Boolean {
        if (root != null) return true
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "overlay_permission_denied")
            return false
        }
        clamp()
        params.x = screen().x - params.width
        val panel = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        val texture = TextureView(context)
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        texture.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    moveTo(startX + (event.rawX - downX).toInt(), startY + (event.rawY - downY).toInt())
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { save(); true }
                else -> false
            }
        }
        texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                surface = Surface(st).also(attach)
                Log.i(TAG, "surface_attached ${width}x$height")
            }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                surface?.let(attach)
            }
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                releaseSurface()
                return true
            }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) { surfaceUpdates++ }
        }
        panel.addView(texture, FrameLayout.LayoutParams(-1, -1))
        root = panel
        return try {
            wm.addView(panel, params)
            save()
            Log.i(TAG, "overlay_shown ${state()}")
            true
        } catch (e: Exception) {
            root = null
            releaseSurface()
            Log.e(TAG, "overlay_add_failed", e)
            false
        }
    }

    fun receiving(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        videoSize = "${width}x$height"
        val newAspect = width.toDouble() / height
        if (aspect != newAspect) {
            aspect = newAspect
            update(alignRight = true)
        }
        Log.i(TAG, "mirror_video $videoSize")
    }

    fun move(dx: Int, dy: Int) = moveTo(params.x + dx, params.y + dy)

    fun moveTo(x: Int, y: Int) {
        params.x = x; params.y = y
        update()
    }

    fun resize(scale: Float) {
        if (!scale.isFinite()) return
        setLongEdge((maxOf(params.width, params.height) * scale.coerceIn(0.5f, 2f)).roundToInt())
    }

    fun setWidth(width: Int) {
        setLongEdge(geometry().longEdgeForWidth(width))
    }

    fun setLongEdge(longEdge: Int) {
        val wasAtRightEdge = params.x + params.width == screen().x
        val g = geometry()
        val s = screen()
        // A restored landscape preference can exceed the current portrait limit.
        val preferenceLimit = maxOf(s.x * 4 / 5, s.y * 4 / 5, g.minLongEdge)
        preferredLongEdge = longEdge.coerceIn(g.minLongEdge, preferenceLimit)
        update(alignRight = wasAtRightEdge)
    }

    fun opacity(value: Float) {
        if (!value.isFinite()) return
        params.alpha = value.coerceIn(0.12f, 1f)
        update()
    }

    fun currentOpacity(): Float = params.alpha

    fun compact() {
        val s = screen()
        preferredLongEdge = minOf(320, s.x / 4)
        clamp()
        params.x = s.x - params.width
        params.y = dp(8)
        params.alpha = 1f
        update()
    }

    private fun clamp() {
        val s = screen()
        val (width, height) = geometry().size(preferredLongEdge)
        params.width = width
        params.height = height
        params.x = params.x.coerceIn(0, (s.x - params.width).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (s.y - params.height).coerceAtLeast(0))
    }

    private fun update(alignRight: Boolean = false) {
        clamp()
        if (alignRight) params.x = screen().x - params.width
        root?.let { wm.updateViewLayout(it, params) }
        save()
        Log.i(TAG, "overlay_changed ${state()}")
    }

    private fun save() {
        prefs.edit().putInt("x", params.x).putInt("y", params.y).putInt("width", params.width)
            .putInt("long_edge", preferredLongEdge).putString("aspect_ratio", aspect.toString())
            .putFloat("opacity", params.alpha).apply()
    }

    fun state(): JSONObject = JSONObject().apply {
        clamp()
        val s = screen()
        put("screenWidth", s.x); put("screenHeight", s.y)
        val g = geometry()
        put("maxWidth", g.size(g.maxLongEdge).first)
        put("minWidth", g.size(g.minLongEdge).first)
        put("aspectRatio", aspect)
        put("longEdge", maxOf(params.width, params.height))
        put("preferredLongEdge", preferredLongEdge)
        put("minLongEdge", g.minLongEdge); put("maxLongEdge", g.maxLongEdge)
        put("visible", root != null)
        put("videoSize", videoSize)
        put("surfaceUpdates", surfaceUpdates)
        put("opacity", currentOpacity())
        put("chrome", false)
        put("x", params.x); put("y", params.y)
        put("width", params.width); put("height", params.height)
    }

    fun hide() {
        videoSize = ""
        val view = root ?: return
        save()
        root = null
        releaseSurface()
        try { wm.removeViewImmediate(view) } catch (e: IllegalArgumentException) { Log.w(TAG, "already_removed", e) }
        Log.i(TAG, "overlay_removed")
    }

    private fun releaseSurface() {
        surface?.let { detach(it); it.release() }
        surface = null
    }

    companion object { private const val TAG = "MirrorOverlay" }
}
