package com.karirjepang.dailymonitoringkj

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val LAUNCH_DELAY_MS = 10_000L // 10 seconds after boot
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "BootReceiver triggered with action: $action")

        val bootActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.REBOOT",
            Intent.ACTION_LOCKED_BOOT_COMPLETED
        )

        if (action in bootActions) {
            Log.d(TAG, "Boot action matched: $action — scheduling app launch in ${LAUNCH_DELAY_MS}ms")

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val launchIntent = Intent(context, com.karirjepang.dailymonitoringkj.ui.main.MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(launchIntent)
                    Log.d(TAG, "App launched successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch app: ${e.message}", e)
                }
            }, LAUNCH_DELAY_MS)
        } else {
            Log.w(TAG, "Unknown action received: $action")
        }
    }
}