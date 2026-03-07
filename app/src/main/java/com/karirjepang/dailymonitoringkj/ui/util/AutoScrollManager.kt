package com.karirjepang.dailymonitoringkj.ui.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AutoScrollManager(
    private val recyclerView: RecyclerView,
    private val delayMillis: Long = 2000L,
    private val actualDataCount: Int = 0
) {

    private val handler = Handler(Looper.getMainLooper())
    private var started = false
    private var waitingForIdle = false
    private var currentTarget = -1
    private val TAG = "AutoScrollManager"

    // listener untuk men-trigger scroll berikutnya segera setelah RecyclerView menjadi idle
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
            if (!started) return
            Log.d(TAG, "onScrollStateChanged: newState=$newState, waitingForIdle=$waitingForIdle, currentTarget=$currentTarget")
            if (waitingForIdle && newState == RecyclerView.SCROLL_STATE_IDLE) {
                waitingForIdle = false
                Log.d(TAG, "Scroll IDLE, scheduling next scroll. currentTarget=$currentTarget")
                // Langsung trigger scroll berikutnya tanpa update currentTarget
                scheduleNextScroll()
            }
        }

        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            // Jika user menggulir manual saja (bukan otomatis), update currentTarget
            // Tapi karena ini otomatis scroll, jangan update
            if (!started) return
            // Kosongkan: jangan update currentTarget saat otomatis scroll
        }
    }

    private val initialRunnable = Runnable {
        if (!started) return@Runnable
        Log.d(TAG, "initialRunnable executing. actualDataCount=$actualDataCount")
        // jika data kurang dari 1, tidak perlu scroll
        if (actualDataCount <= 0) {
            Log.d(TAG, "actualDataCount <= 0, returning")
            return@Runnable
        }

        // ensure RecyclerView finished layout
        recyclerView.post {
            Log.d(TAG, "Post runnable executing on main thread")
            // set currentTarget ke posisi pertama yang terlihat
            val lm = recyclerView.layoutManager as? LinearLayoutManager
            currentTarget = lm?.findFirstVisibleItemPosition() ?: 0
            if (currentTarget < 0) currentTarget = 0
            Log.d(TAG, "Starting chain scroll. currentTarget=$currentTarget")

            // langsung mulai chain scrolling
            scheduleNextScroll()
        }
    }

    private fun scheduleNextScroll() {
        if (!started) {
            Log.d(TAG, "scheduleNextScroll: not started, returning")
            return
        }
        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return

        // Hitung next position SEBELUM scroll
        val nextTarget = if (currentTarget >= actualDataCount - 1) 0 else currentTarget + 1
        currentTarget = nextTarget
        Log.d(TAG, "scheduleNextScroll: scrolling to position $currentTarget (actualDataCount=$actualDataCount)")

        // mulai smooth scroll ke next position
        val scroller = SmoothLinearScroller(recyclerView.context)
        scroller.targetPosition = nextTarget
        // set flag agar OnScrollListener memicu scheduleNextScroll setelah scroller selesai
        waitingForIdle = true
        lm.startSmoothScroll(scroller)
    }

    fun startAutoScroll() {
        if (started) {
            Log.d(TAG, "startAutoScroll: already started, ignoring")
            return
        }
        Log.d(TAG, "startAutoScroll: starting. delayMillis=$delayMillis, actualDataCount=$actualDataCount")
        started = true
        // pasang listener
        recyclerView.addOnScrollListener(scrollListener)
        // tunggu delay awal sekali saja lalu mulai scrolling continuous (post to ensure layout)
        handler.postDelayed({
            Log.d(TAG, "Delay completed, posting initialRunnable")
            recyclerView.post(initialRunnable)
        }, delayMillis)
    }

    fun stopAutoScroll() {
        if (!started) return
        Log.d(TAG, "stopAutoScroll: stopping")
        started = false
        waitingForIdle = false
        handler.removeCallbacks(initialRunnable)
        recyclerView.removeOnScrollListener(scrollListener)
    }

    fun cleanup() {
        Log.d(TAG, "cleanup: cleaning up")
        stopAutoScroll()
    }
}
