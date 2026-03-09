package com.karirjepang.dailymonitoringkj.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time network connectivity monitor for signage.
 *
 * Uses **dual detection**:
 * 1. [ConnectivityManager.NetworkCallback] for instant events (when they work)
 * 2. **Periodic ping every 5 seconds** as fallback (Android TV often doesn't fire onLost)
 *
 * Ping target: Google DNS 8.8.8.8:53 (TCP, 2s timeout)
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "NetworkMonitor"
        private const val PING_TIMEOUT_MS = 2000
        private const val PING_INTERVAL_MS = 5000L // Check every 5 seconds
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var isRegistered = false
    private var scope: CoroutineScope? = null
    private var pingJob: Job? = null

    // Mutex prevents race condition when multiple coroutines call updateOnlineState concurrently
    // (e.g., onAvailable callback + periodic ping firing at the same time)
    private val stateMutex = Mutex()

    // Thread-safe copy-on-write list for listeners
    @Volatile
    private var onOnlineListeners = listOf<() -> Unit>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Log.d(TAG, "onAvailable — verifying...")
            verifyAndUpdate()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "onLost — verifying...")
            verifyAndUpdate()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val validated = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
            if (validated && !_isOnline.value) {
                Log.d(TAG, "onCapabilitiesChanged validated — verifying...")
                verifyAndUpdate()
            }
        }
    }

    fun addOnOnlineListener(listener: () -> Unit) {
        onOnlineListeners = onOnlineListeners + listener
    }

    fun start() {
        if (isRegistered) return
        scope = CoroutineScope(Dispatchers.IO)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isRegistered = true
            Log.d(TAG, "Network monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }

        // Immediate check
        verifyAndUpdate()

        // Start continuous ping loop — the real safety net for Android TV
        startPeriodicPing()
    }

    fun stop() {
        pingJob?.cancel()
        pingJob = null

        if (isRegistered) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                isRegistered = false
                onOnlineListeners = emptyList()
                Log.d(TAG, "Network monitoring stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback", e)
            }
        }
    }

    /**
     * Ping every 5 seconds. This catches WiFi-off scenarios that
     * Android TV's ConnectivityManager fails to report via onLost.
     */
    private fun startPeriodicPing() {
        pingJob?.cancel()
        pingJob = scope?.launch {
            while (isActive) {
                delay(PING_INTERVAL_MS)
                val reachable = pingTest()
                updateOnlineState(reachable)
            }
        }
    }

    private fun verifyAndUpdate() {
        scope?.launch {
            val reachable = pingTest()
            Log.d(TAG, "Ping result: $reachable")
            updateOnlineState(reachable)
        }
    }

    private suspend fun updateOnlineState(online: Boolean) {
        stateMutex.withLock {
            val was = _isOnline.value
            if (was == online) return // No change, skip

            _isOnline.value = online
            Log.d(TAG, "State changed: $was → $online")

            if (online && !was) {
                Log.d(TAG, "OFFLINE → ONLINE, notifying ${onOnlineListeners.size} listeners")
                // Read the volatile snapshot — safe even if another thread modifies
                val listeners = onOnlineListeners
                listeners.forEach { it.invoke() }
            }
        }
    }

    private fun pingTest(): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("dailymonitoringkj.id", 443), PING_TIMEOUT_MS)
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }
}

