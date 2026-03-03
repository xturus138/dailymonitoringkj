package com.karirjepang.dailymonitoringkj.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
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
                val fragment = when (currentSlideIndex) {
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
                currentSlideIndex = (currentSlideIndex + 1) % 4 

                delay(10000)
            }
        }
    }
}