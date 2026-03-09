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

/**
 * Centralized cache manager for all signage slide data.
 *
 * Features:
 * - Fetches ALL data from API every [REFRESH_INTERVAL_MS] (5 minutes) in **parallel**.
 * - Uses hash comparison to only emit new data when it actually changes.
 * - Slides read from [StateFlow] — always get the latest cached value instantly.
 * - On cold start, fetches immediately, then starts the 5-minute cycle.
 * - On API failure, retains previous cached data (critical for 24/7 signage).
 * - **Staleness indicator**: exposes [isDataStale] when data hasn't refreshed in 15+ minutes.
 * - **Exponential backoff**: retries faster on failure (30s → 60s → 120s → cap at 5 min).
 * - **Duplicate-safe**: uses [AtomicBoolean] + job cancellation to prevent multiple loops.
 */
@Singleton
class CachedDataManager @Inject constructor(
    private val repository: MonitoringRepository,
    private val networkMonitor: NetworkMonitor,
    private val apiClock: ApiClock
) {

    companion object {
        private const val TAG = "CachedDataManager"
        private const val REFRESH_INTERVAL_MS = 60 * 1000L       // 1 menit (60 detik x 1000)
        private const val RETRY_BASE_MS = 30 * 1000L              // 30 seconds base retry
        private const val STALE_THRESHOLD_MS = 5 * 60 * 1000L    // 5 menit → data dianggap usang
    }

    // --- StateFlows for each slide data ---

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

    // --- Staleness indicator (#3) ---

    private val _isDataStale = MutableStateFlow(false)
    val isDataStale: StateFlow<Boolean> = _isDataStale.asStateFlow()

    private val _consecutiveFailures = MutableStateFlow(0)
    val consecutiveFailures: StateFlow<Int> = _consecutiveFailures.asStateFlow()

    // --- Last successful update timestamp for UI display ---

    private val _lastUpdatedText = MutableStateFlow("")
    val lastUpdatedText: StateFlow<String> = _lastUpdatedText.asStateFlow()

    // --- Hash storage for change detection ---

    private var hashKehadiran: Int = 0
    private var hashMeeting: Int = 0
    private var hashProgressDivisi: Int = 0
    private var hashKeberangkatanPMI: Int = 0
    private var hashMitra: Int = 0

    // --- Refresh tracking (#2: AtomicBoolean + Job cancellation) ---

    private val isRunning = AtomicBoolean(false)
    private var refreshJob: Job? = null
    private var lastSuccessTimestamp: Long = 0L
    private var failureCount: Int = 0

    /**
     * Starts the auto-refresh loop.
     * Safe to call multiple times — cancels previous loop before starting new one (#2 fix).
     * Fetches immediately on first call, then every 5 minutes.
     */
    fun startAutoRefresh(scope: CoroutineScope) {
        // Cancel previous job to prevent duplicate loops (#2)
        refreshJob?.cancel()
        isRunning.set(false)

        if (!isRunning.compareAndSet(false, true)) {
            Log.d(TAG, "Auto-refresh already running, skipping")
            return
        }

        Log.d(TAG, "Starting auto-refresh cycle (interval: ${REFRESH_INTERVAL_MS / 1000}s)")

        refreshJob = scope.launch {
            while (isActive) {
                // HAPUS BLOK INI:
                // if (!networkMonitor.isOnline.value) { ... }

                // Langsung hajar fetch data (Try Anyway)
                val success = refreshAllParallel()

                if (success) {
                    failureCount = 0
                    _consecutiveFailures.value = 0
                    lastSuccessTimestamp = System.currentTimeMillis()
                    _isDataStale.value = false
                    _lastUpdatedText.value = "Diperbarui ${apiClock.getCurrentTimeFormatted()}"
                    delay(REFRESH_INTERVAL_MS)
                } else {
                    failureCount++
                    _consecutiveFailures.value = failureCount
                    checkStaleness()

                    // Jika GAGAL BENERAN (karena timeout/no route to host dari Retrofit)
                    // Sistem akan pakai Exponential backoff: 30s → 60s → 120s → cap at 5 min
                    val backoffMs = (RETRY_BASE_MS * (1L shl failureCount.coerceAtMost(4)))
                        .coerceAtMost(REFRESH_INTERVAL_MS)
                    Log.w(TAG, "Refresh failed ($failureCount consecutive), retry in ${backoffMs / 1000}s")
                    delay(backoffMs)
                }
            }
            isRunning.set(false)
        }
    }

    /**
     * Fetches all data from API **in parallel** and updates StateFlows only if data changed.
     * Returns true if at least one endpoint succeeded (#6 fix).
     */
    private suspend fun refreshAllParallel(): Boolean {
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

    /**
     * Check if data is stale: last successful refresh > 15 minutes ago (#3 fix).
     */
    private fun checkStaleness() {
        if (lastSuccessTimestamp > 0) {
            val elapsed = System.currentTimeMillis() - lastSuccessTimestamp
            if (elapsed > STALE_THRESHOLD_MS) {
                _isDataStale.value = true
                Log.w(TAG, "Data is STALE — last success ${elapsed / 1000}s ago")
            }
        }
    }

    // --- Individual refresh methods with hash check ---
    // Each returns true on success, false on failure.

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
            Log.e(TAG, "Failed to refresh kehadiran", e)
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
            Log.e(TAG, "Failed to refresh meeting", e)
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
            Log.e(TAG, "Failed to refresh progressDivisi", e)
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
            Log.e(TAG, "Failed to refresh keberangkatanPMI", e)
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
            Log.e(TAG, "Failed to refresh mitra", e)
            false
        }
    }
}

