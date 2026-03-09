package com.karirjepang.dailymonitoringkj.ui.main

import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.karirjepang.dailymonitoringkj.R
import com.karirjepang.dailymonitoringkj.ui.main.slide1.SlideSatu
import com.karirjepang.dailymonitoringkj.ui.main.slide2.SlideDua
import com.karirjepang.dailymonitoringkj.ui.main.slide3.SlideTiga
import com.karirjepang.dailymonitoringkj.ui.main.slide4.SlideEmpat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val TAG = "MainActivity"

    private val viewModel: MainViewModel by viewModels()
    private var currentSlideIndex = 0
    private var isSliding = false
    private var hasEverLoggedIn = false

    // Wake lock to prevent TV Doze mode from pausing background refresh
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Keep screen on & prevent TV Doze mode
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        acquireWakeLock()

        // Start clock immediately — ticks locally, syncs from API when possible
        viewModel.apiClock.start()

        // Observe clock in the permanent header
        val tvDate = findViewById<TextView>(R.id.currentDateTime)
        val tvTime = findViewById<TextView>(R.id.currentDayTime)
        viewModel.apiClock.currentDate.observe(this) { tvDate.text = it }
        viewModel.apiClock.currentTime.observe(this) { tvTime.text = it }

        // Start network monitoring (includes immediate ping check)
        viewModel.networkMonitor.start()

        // Observe network status → update indicator instantly
        observeNetworkStatus()

//        // 1. TETAP PERTAHANKAN: Kalau tiba-tiba koneksi nyambung, pancing login & sync
//        viewModel.networkMonitor.addOnOnlineListener {
//            Log.d(TAG, "Internet confirmed — triggering login + clock sync")
//            viewModel.apiClock.forceResync()
//            viewModel.autoLoginBackground()
//        }

        // 2. TAMBAHAN BARU: Paksa login di awal (Try Anyway) tidak peduli status internet!
        viewModel.autoLoginBackground()

        // Observe login result
        val tvLoading = findViewById<TextView>(R.id.tvLoadingStatus)
        val tvStatus = findViewById<TextView>(R.id.tvStatusIndicator)

        viewModel.isLoginSuccessful.observe(this) { success ->
            if (success) {
                hasEverLoggedIn = true
                tvLoading.visibility = View.GONE
                tvStatus.visibility = View.GONE
                viewModel.startCacheRefresh()
                startAutoSlide()
            } else {
                // Tampilan error merah tetap dimunculkan sebagai info
                if (!viewModel.networkMonitor.isOnline.value) {
                    tvStatus.text = "⚠ Tidak ada koneksi internet"
                } else {
                    tvStatus.text = "⚠ Server tidak dapat dijangkau"
                }
                tvStatus.setBackgroundColor(0xFFD32F2F.toInt())
                tvStatus.visibility = View.VISIBLE

                if (!hasEverLoggedIn) {
                    tvLoading.text = tvStatus.text
                }

                // 3. UBAH BAGIAN INI: Hapus syarat isOnline.
                // Biarkan dia nekat mencoba login lagi setiap 10 detik sampai berhasil.
                lifecycleScope.launch {
                    delay(10_000)
                    if (!hasEverLoggedIn) {
                        viewModel.autoLoginBackground()
                    }
                }
            }
        }
    }

    /**
     * Observe real-time network status.
     *
     * - Online → hide indicator (if already logged in)
     * - Offline → show red indicator + update loading text if not yet logged in
     *
     * Note: StateFlow emits the initial value immediately, so even cold-start
     * with no internet will trigger the offline branch right away.
     */
    private fun observeNetworkStatus() {
        val tvStatus = findViewById<TextView>(R.id.tvStatusIndicator)
        val tvLoading = findViewById<TextView>(R.id.tvLoadingStatus)

        lifecycleScope.launch(Dispatchers.Main.immediate) {
            viewModel.networkMonitor.isOnline.collect { online ->
                Log.d(TAG, "Network state received: online=$online, hasEverLoggedIn=$hasEverLoggedIn")
                if (online) {
                    if (hasEverLoggedIn) {
                        tvStatus.visibility = View.GONE
                    }
                } else {
                    tvStatus.text = "⚠ Tidak ada koneksi internet"
                    tvStatus.setBackgroundColor(0xFFD32F2F.toInt())
                    tvStatus.visibility = View.VISIBLE

                    if (!hasEverLoggedIn) {
                        tvLoading.text = "Tidak ada koneksi internet"
                    }
                }
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "DailyMonitoringKJ::SignageWakeLock"
            ).apply {
                acquire()
            }
            Log.d(TAG, "Wake lock acquired for signage mode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun startAutoSlide() {
        if (isSliding) return
        isSliding = true

        val container = findViewById<View>(R.id.fragmentContainer)

        lifecycleScope.launch {
            while (isActive) {
//                val fragment: Fragment = when (currentSlideIndex) {
//                    0 -> SlideSatu()
//                    1 -> SlideDua()
//                    2 -> SlideTiga()
//                    else -> SlideEmpat()
//                }
                val fragment: Fragment = when (currentSlideIndex) {
                    0 -> SlideSatu()
                    else -> SlideDua()
                }


                val anim = SlideAnimationConfig.current

                if (anim.sequential) {
                    container.awaitAnimate(alpha = 0f, duration = anim.fadeOutMs)
                    delay(anim.pauseMs)

                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainer, fragment)
                    }
                    delay(50)

                    container.awaitAnimate(alpha = 1f, duration = anim.fadeInMs)
                } else {
                    supportFragmentManager.commit {
                        setCustomAnimations(anim.enter, anim.exit)
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainer, fragment)
                    }
                }

//                val nextSlideIndex = (currentSlideIndex + 1) % 4
                val nextSlideIndex = (currentSlideIndex + 1) % 2

                val startTime = System.currentTimeMillis()
                val minDisplayMs = 10_000L

                when (fragment) {
                    is SlideSatu -> {
                        fragment.awaitScrollFinished()
                        delay(3000)
                    }
                    is SlideDua -> {
                        fragment.awaitScrollFinished()
                        delay(3000)
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                val remaining = minDisplayMs - elapsed
                if (remaining > 0) {
                    delay(remaining)
                }

                currentSlideIndex = nextSlideIndex
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.networkMonitor.stop()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Wake lock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }
}

private suspend fun View.awaitAnimate(alpha: Float, duration: Long) {
    suspendCancellableCoroutine { cont ->
        animate()
            .alpha(alpha)
            .setDuration(duration)
            .withEndAction { if (cont.isActive) cont.resume(Unit) }
            .start()

        cont.invokeOnCancellation { animate().cancel() }
    }
}
