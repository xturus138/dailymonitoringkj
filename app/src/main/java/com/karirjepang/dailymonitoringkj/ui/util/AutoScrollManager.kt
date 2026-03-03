package com.karirjepang.dailymonitoringkj.ui.util

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AutoScrollManager(
    private val recyclerView: RecyclerView,
    private val delayMillis: Long = 2000L,
    private val actualDataCount: Int = 0
) {

    private val handler = Handler(Looper.getMainLooper())
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            // Only scroll if data exists and scroll is needed
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            if (layoutManager != null && recyclerView.adapter != null && actualDataCount > 0) {
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

                // If at the end of actual data, scroll back to top
                if (lastVisiblePosition >= actualDataCount - 1) {
                    recyclerView.smoothScrollToPosition(0)
                } else {
                    // Scroll to next item
                    recyclerView.smoothScrollToPosition(lastVisiblePosition + 1)
                }
            }
            handler.postDelayed(this, intervalMillis)
        }
    }

    private var intervalMillis: Long = 3000L

    fun startAutoScroll(intervalMillis: Long = 3000L) {
        this.intervalMillis = intervalMillis
        handler.postDelayed(autoScrollRunnable, delayMillis)
    }

    fun stopAutoScroll() {
        handler.removeCallbacks(autoScrollRunnable)
    }

    fun cleanup() {
        stopAutoScroll()
    }
}
