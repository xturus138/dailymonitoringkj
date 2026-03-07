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

        viewModel.autoLoginBackground()

        viewModel.isLoginSuccessful.observe(this) { success ->
            if (success) {
                startAutoSlide()
            } else {
                Toast.makeText(this, "Gagal terhubung ke API (Login Failed)", Toast.LENGTH_LONG).show()
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

                // For slides with scrolling (1 & 2): wait for scroll to finish, THEN 2s end-pause already built-in
                // For all slides: always wait at least 10 seconds total
                when (fragment) {
                    is SlideSatu -> {
                        // Wait for scroll to complete (no hard timeout — let it finish)
                        fragment.awaitScrollFinished()
                        // After scroll done + 2s keepAtEnd already elapsed,
                        // add extra delay so slide stays visible
                        delay(3000)
                    }
                    is SlideDua -> {
                        fragment.awaitScrollFinished()
                        delay(3000)
                    }
                    else -> {
                        // Slide 3 (chart) & Slide 4 (mitra): just show for 10 seconds
                        delay(10000)
                    }
                }

                currentSlideIndex = (currentSlideIndex + 1) % 4
            }
        }
    }
}