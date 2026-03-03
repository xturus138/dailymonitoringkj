package com.karirjepang.dailymonitoringkj

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Handler(Looper.getMainLooper()).postDelayed({
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.karirjepang.dailymonitoringkj")

                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            }, 15000)
        }
    }
}