package com.karirjepang.dailymonitoringkj.ui.util

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import kotlin.math.abs

class SmoothLinearScroller(context: Context) : LinearSmoothScroller(context) {

    override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
        // fallback speed, but we will control time via calculateTimeForScrolling
        return 100f / displayMetrics.densityDpi
    }

    override fun calculateTimeForScrolling(dx: Int): Int {
        // calculate time proportional to distance but constrained to [800ms, 2000ms]
        // use ~10 ms per pixel so small items (~100px) take ~1000ms
        val base = (abs(dx) * 10f).toInt()
        return base.coerceIn(800, 2000)
    }

    override fun calculateDtToFit(
        viewStart: Int,
        viewEnd: Int,
        boxStart: Int,
        boxEnd: Int,
        snapPreference: Int
    ): Int {
        // Pastikan scroll ke tengah item
        return boxStart + (boxEnd - boxStart) / 2 - (viewStart + (viewEnd - viewStart) / 2)
    }
}
