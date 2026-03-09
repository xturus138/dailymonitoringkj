package com.karirjepang.dailymonitoringkj

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.system.exitProcess

/**
 * Custom UncaughtExceptionHandler that automatically restarts the app
 * when an unhandled crash occurs. Essential for 24/7 signage displays
 * where no one is available to manually restart the app.
 */
class CrashRecoveryHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val TAG = "CrashRecovery"

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e(TAG, "Uncaught exception — scheduling restart", throwable)

        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                // Restart after 3 seconds
                alarmManager.set(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + 3000,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule restart", e)
        }

        // Kill the process so AlarmManager can restart it cleanly
        exitProcess(1)
    }
}

