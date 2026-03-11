package com.karirjepang.dailymonitoringkj.core.cache

import android.util.Log
import com.karirjepang.dailymonitoringkj.core.model.*
import com.karirjepang.dailymonitoringkj.core.network.NetworkMonitor
import com.karirjepang.dailymonitoringkj.core.repository.MonitoringRepository
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachedDataManager @Inject constructor(
    private val repository: MonitoringRepository,
    private val networkMonitor: NetworkMonitor,
    private val apiClock: ApiClock
) {

    companion object {
        private const val TAG = "CachedDataManager"
        private const val REFRESH_INTERVAL_MS = 60 * 1000L       // 1 menit
        private const val RETRY_BASE_MS = 30 * 1000L              // 30 seconds base retry
        private const val STALE_THRESHOLD_MS = 5 * 60 * 1000L    // 5 menit → data usang
    }

    // --- StateFlows ---
    private val _kehadiranList = MutableStateFlow<List<Kehadiran>>(emptyList())
    val kehadiranList: StateFlow<List<Kehadiran>> = _kehadiranList.asStateFlow()

    private val _meetingList = MutableStateFlow<List<Meeting>>(emptyList())
    val meetingList: StateFlow<List<Meeting>> = _meetingList.asStateFlow()

    private val _progressDivisiList = MutableStateFlow<List<ProgressDivisi>>(emptyList())
    val progressDivisiList: StateFlow<List<ProgressDivisi>> = _progressDivisiList.asStateFlow()

    private val _keberangkatanPMIList = MutableStateFlow<List<KeberangkatanPMI>>(emptyList())
    val keberangkatanPMIList: StateFlow<List<KeberangkatanPMI>> = _keberangkatanPMIList.asStateFlow()

    private val _mitraList = MutableStateFlow<List<Mitra>>(emptyList())
    val mitraList: StateFlow<List<Mitra>> = _mitraList.asStateFlow()

    // --- Staleness indicator ---
    private val _isDataStale = MutableStateFlow(false)
    val isDataStale: StateFlow<Boolean> = _isDataStale.asStateFlow()

    private val _consecutiveFailures = MutableStateFlow(0)
    val consecutiveFailures: StateFlow<Int> = _consecutiveFailures.asStateFlow()

    private val _lastUpdatedText = MutableStateFlow("")
    val lastUpdatedText: StateFlow<String> = _lastUpdatedText.asStateFlow()

    // --- Hash storage ---
    private var hashKehadiran: Int = 0
    private var hashMeeting: Int = 0
    private var hashProgressDivisi: Int = 0
    private var hashKeberangkatanPMI: Int = 0
    private var hashMitra: Int = 0

    // --- Refresh tracking ---
    private val isRunning = AtomicBoolean(false)
    private var refreshJob: Job? = null
    private var delayJob: Job? = null // NEW: Dedicated job for the waiting period
    private var lastSuccessTimestamp: Long = 0L
    private var failureCount: Int = 0
    private var lastAttemptWasFailure = false // NEW: Track if we need an instant retry

    init {
        // NEW: Listen for the exact moment the internet returns
        networkMonitor.addOnOnlineListener {
            Log.d(TAG, "Internet Restored Event Received!")

            // Only interrupt if the previous attempt failed and we are currently waiting
            if (lastAttemptWasFailure && delayJob?.isActive == true) {
                Log.d(TAG, "Interrupting backoff delay to fetch immediately.")
                delayJob?.cancel() // This breaks the delay() in the while loop
            }
        }
    }

    fun startAutoRefresh(scope: CoroutineScope) {
        refreshJob?.cancel()
        delayJob?.cancel()
        isRunning.set(false)

        if (!isRunning.compareAndSet(false, true)) {
            Log.d(TAG, "Auto-refresh already running, skipping")
            return
        }

        Log.d(TAG, "Starting auto-refresh cycle")

        refreshJob = scope.launch {
            while (isActive) {
                // KITA HAPUS PRASYARAT (PRE-CHECK) PENGECEKAN PING DI SINI!
                // Biarkan Retrofit yang membuktikan apakah internet benar-benar mati atau tidak.

                Log.d(TAG, "Mencoba memanggil API (Try Anyway)...")
                val success = refreshAllParallel()

                if (success) {
                    // API BERHASIL! (Artinya internet nyata-nyata ada, masa bodoh dengan hasil ping)
                    failureCount = 0
                    lastAttemptWasFailure = false
                    _consecutiveFailures.value = 0
                    lastSuccessTimestamp = System.currentTimeMillis()
                    _isDataStale.value = false
                    _lastUpdatedText.value = "Diperbarui ${apiClock.getCurrentTimeFormatted()}"

                    waitForNextCycle(REFRESH_INTERVAL_MS) // Tunggu 1 menit
                } else {
                    // API GAGAL (Memang tidak ada internet, atau server down)
                    failureCount++
                    lastAttemptWasFailure = true
                    _consecutiveFailures.value = failureCount
                    checkStaleness()

                    val backoffMs = (RETRY_BASE_MS * (1L shl failureCount.coerceAtMost(4)))
                        .coerceAtMost(REFRESH_INTERVAL_MS)
                    Log.w(TAG, "Refresh gagal ($failureCount), istirahat selama ${backoffMs / 1000} detik")

                    // Sistem beristirahat. Jika tiba-tiba NetworkMonitor mendeteksi koneksi,
                    // listener di blok "init" akan membatalkan istirahat ini dan loop langsung berulang!
                    waitForNextCycle(backoffMs)
                }
            }
            isRunning.set(false)
        }
    }

    /**
     * Extracts the delay into a cancellable sub-job.
     */
    private suspend fun waitForNextCycle(timeMs: Long) {
        coroutineScope {
            delayJob = launch {
                delay(timeMs)
            }
            delayJob?.join() // Wait until the delay finishes normally OR gets cancelled
        }
    }

    private suspend fun refreshAllParallel(): Boolean {
        // (This function remains exactly the same as your original code)
        Log.d(TAG, "Refreshing all data from API (parallel)...")
        val startTime = System.currentTimeMillis()
        var successCount = 0

        try {
            coroutineScope {
                val jobs = listOf(
                    async { refreshKehadiran() },
                    async { refreshMeeting() },
                    async { refreshProgressDivisi() },
                    async { refreshKeberangkatanPMI() },
                    async { refreshMitra() }
                )
                jobs.forEach { if (it.await()) successCount++ }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parallel refresh error", e)
        }

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Refresh complete in ${elapsed}ms ($successCount/5 succeeded)")
        return successCount > 0
    }

    private fun checkStaleness() {
        // (This function remains exactly the same as your original code)
        if (lastSuccessTimestamp > 0) {
            val elapsed = System.currentTimeMillis() - lastSuccessTimestamp
            if (elapsed > STALE_THRESHOLD_MS) {
                _isDataStale.value = true
                Log.w(TAG, "Data is STALE — last success ${elapsed / 1000}s ago")
            }
        }
    }

    // (refreshKehadiran, refreshMeeting, etc. remain exactly the same as your original code)
    private suspend fun refreshKehadiran(): Boolean {
        return try {
            val data = repository.getKehadiran()
            val newHash = data.hashCode()
            if (newHash != hashKehadiran) {
                hashKehadiran = newHash
                _kehadiranList.value = data
                Log.d(TAG, "Kehadiran updated: ${data.size} items")
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun refreshMeeting(): Boolean {
        return try {
            val data = repository.getMeeting()
            val newHash = data.hashCode()
            if (newHash != hashMeeting) {
                hashMeeting = newHash
                _meetingList.value = data
                Log.d(TAG, "Meeting updated: ${data.size} items")
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun refreshProgressDivisi(): Boolean {
        return try {
            val data = repository.getProgressDivisi()
            val newHash = data.hashCode()
            if (newHash != hashProgressDivisi) {
                hashProgressDivisi = newHash
                _progressDivisiList.value = data
                Log.d(TAG, "ProgressDivisi updated: ${data.size} items")
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun refreshKeberangkatanPMI(): Boolean {
        return try {
            val data = repository.getKeberangkatanPMI()
            val newHash = data.hashCode()
            if (newHash != hashKeberangkatanPMI) {
                hashKeberangkatanPMI = newHash
                _keberangkatanPMIList.value = data
                Log.d(TAG, "KeberangkatanPMI updated: ${data.size} items")
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun refreshMitra(): Boolean {
        return try {
            val data = repository.getDaftarMitra()
            val newHash = data.hashCode()
            if (newHash != hashMitra) {
                hashMitra = newHash
                _mitraList.value = data
                Log.d(TAG, "Mitra updated: ${data.size} items")
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}