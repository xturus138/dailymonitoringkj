package com.karirjepang.dailymonitoringkj.ui.main

import android.os.Bundle
import android.widget.Toast
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var currentSlideIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start clock immediately — it doesn't need auth
        viewModel.apiClock.start()

        viewModel.autoLoginBackground()

        viewModel.isLoginSuccessful.observe(this) { success ->
            if (success) {
                startAutoSlide()
            } else {
                Toast.makeText(this, "Gagal terhubung ke API, mencoba ulang...", Toast.LENGTH_SHORT).show()
                // Retry login after 5 seconds
                lifecycleScope.launch {
                    delay(5000)
                    viewModel.autoLoginBackground()
                }
            }
        }
    }

    private fun startAutoSlide() {
        lifecycleScope.launch {
            while (isActive) {
                val fragment: Fragment = when (currentSlideIndex) {
                    0 -> SlideSatu()
                    1 -> SlideDua()
                    2 -> SlideTiga()
                    else -> SlideEmpat()
                }

                supportFragmentManager.commit {
                    setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    setReorderingAllowed(true)
                    replace(R.id.fragmentContainer, fragment)
                }

                // Calculate next slide index and start prefetching its data
                val nextSlideIndex = (currentSlideIndex + 1) % 4
                viewModel.prefetchNextSlideData(nextSlideIndex)

                // All slides must be visible for at least 10 seconds.
                // For scroll slides: wait for scroll + extra pause, but never less than 10s total.
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

                // Ensure minimum display time for ALL slides
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = minDisplayMs - elapsed
                if (remaining > 0) {
                    delay(remaining)
                }

                currentSlideIndex = nextSlideIndex
            }
        }
    }
}