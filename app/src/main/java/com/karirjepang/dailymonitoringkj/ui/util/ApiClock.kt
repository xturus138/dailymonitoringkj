package com.karirjepang.dailymonitoringkj.ui.util

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.karirjepang.dailymonitoringkj.core.network.TimeApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton clock that fetches the current time from TimeAPI (Asia/Jakarta) once,
 * then ticks locally every second. Re-syncs with the API every [RESYNC_INTERVAL_MS].
 *
 * Because it's a singleton, all slides share the same LiveData —
 * no more "flash of local time" when switching slides.
 */
@Singleton
class ApiClock @Inject constructor(
    private val timeApiService: TimeApiService
) {
    private val TAG = "ApiClock"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String> get() = _currentDate

    private val _currentTime = MutableLiveData<String>()
    val currentTime: LiveData<String> get() = _currentTime

    private val calendar = Calendar.getInstance(
        TimeZone.getTimeZone("Asia/Jakarta"), Locale("id", "ID")
    )

    @Volatile
    private var started = false

    private val dayNames = mapOf(
        "Monday" to "SENIN",
        "Tuesday" to "SELASA",
        "Wednesday" to "RABU",
        "Thursday" to "KAMIS",
        "Friday" to "JUMAT",
        "Saturday" to "SABTU",
        "Sunday" to "MINGGU"
    )

    companion object {
        private const val RESYNC_INTERVAL_MS = 60_000L
    }

    /**
     * Start the clock. Safe to call multiple times — only the first call takes effect.
     */
    fun start() {
        if (started) return
        started = true

        scope.launch {
            syncFromApi()
            updateDisplay()

            // Tick every second
            launch {
                while (true) {
                    delay(1000)
                    calendar.add(Calendar.SECOND, 1)
                    updateDisplay()
                }
            }

            // Re-sync periodically
            launch {
                while (true) {
                    delay(RESYNC_INTERVAL_MS)
                    syncFromApi()
                }
            }
        }
    }

    private suspend fun syncFromApi() {
        try {
            val response = timeApiService.getCurrentTime()
            val timeResponse = if (response.isSuccessful) response.body() else null
            if (timeResponse != null) {
                calendar.set(Calendar.YEAR, timeResponse.year)
                calendar.set(Calendar.MONTH, timeResponse.month - 1)
                calendar.set(Calendar.DAY_OF_MONTH, timeResponse.day)
                calendar.set(Calendar.HOUR_OF_DAY, timeResponse.hour)
                calendar.set(Calendar.MINUTE, timeResponse.minute)
                calendar.set(Calendar.SECOND, timeResponse.seconds)
                calendar.set(Calendar.MILLISECOND, timeResponse.milliSeconds)
                Log.d(TAG, "Synced time from API: ${timeResponse.dateTime}")
            } else {
                Log.w(TAG, "API time null, using local tick")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync time from API", e)
        }
    }

    private fun updateDisplay() {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekEn = when (dayOfWeekIndex) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> ""
        }
        val dayOfWeekId = dayNames[dayOfWeekEn] ?: dayOfWeekEn.uppercase()

        val dateStr = String.format(
            Locale.US,
            "%s, %02d/%02d/%04d",
            dayOfWeekId, day, month, year
        )
        val timeStr = String.format(Locale.US, "%02d:%02d:%02d", hour, minute, second)

        _currentDate.postValue(dateStr)
        _currentTime.postValue(timeStr)
    }

    /**
     * Returns today's date as "yyyy-MM-dd" (e.g. "2026-03-07") based on the
     * API-synced clock. Used to filter data that should only show today's entries.
     */
    fun getTodayDateString(): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }
}

