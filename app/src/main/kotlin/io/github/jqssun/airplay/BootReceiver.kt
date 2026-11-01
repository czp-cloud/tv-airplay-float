package io.github.jqssun.airplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.jqssun.airplay.service.AirPlayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        if (!prefs[Prefs.BOOT_AUTO_START] || !Settings.canDrawOverlays(context)) return

        val serviceIntent = Intent(context, AirPlayService::class.java)
            .setAction(AirPlayService.ACTION_START_SERVER)
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.i("BootReceiver", "background_receiver_start_requested action=${intent.action}")
        } catch (e: RuntimeException) {
            Log.e("BootReceiver", "background_receiver_start_denied", e)
        }
    }
}
