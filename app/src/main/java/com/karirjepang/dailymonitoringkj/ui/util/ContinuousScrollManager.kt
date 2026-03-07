package com.karirjepang.dailymonitoringkj.ui.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ContinuousScrollManager(
    private val recyclerView: RecyclerView,
    private val delayMillis: Long = 2000L,
    private val actualDataCount: Int = 0,
    /**
     * Duration of a full scroll from top to bottom in milliseconds.
     */
    private val scrollDurationMs: Long = 10000L,
    /**
     * How long to stay at the end of the scroll before signalling completion (ms).
     */
    private val keepAtEndDelayMs: Long = 2000L
) {

    private val handler = Handler(Looper.getMainLooper())
    private var started = false
    private var finished = false
    private val TAG = "ContinuousScrollManager"

    // We compute the REAL max scroll in pixels once after full layout
    private var cachedMaxScroll: Int = -1

    private var onScrollComplete: (() -> Unit)? = null

    fun setOnScrollCompleteListener(listener: (() -> Unit)?) {
        onScrollComplete = listener
    }

    fun isScrollFinished(): Boolean = finished

    fun startAutoScroll() {
        if (started) return
        if (actualDataCount <= 0) {
            handler.postDelayed({
                finished = true
                onScrollComplete?.invoke()
            }, keepAtEndDelayMs)
            return
        }

        Log.d(TAG, "startAutoScroll: delayMillis=$delayMillis, actualDataCount=$actualDataCount, scrollDurationMs=$scrollDurationMs")
        started = true
        finished = false

        handler.postDelayed({
            if (!started) return@postDelayed
            measureAndStartScroll()
        }, delayMillis)
    }

    private fun measureAndStartScroll() {
        if (!started) return

        if (recyclerView.width == 0 || recyclerView.height == 0) {
            handler.postDelayed({ measureAndStartScroll() }, 100)
            return
        }

        recyclerView.post {
            if (!started) return@post

            val adapter = recyclerView.adapter ?: return@post
            if (actualDataCount <= 0) {
                notifyDone()
                return@post
            }

            val lm = recyclerView.layoutManager as? LinearLayoutManager
            val firstChild = lm?.findViewByPosition(0)

            if (firstChild == null) {
                // Children not laid out yet — wait one more frame
                recyclerView.post { measureAndStartScroll() }
                return@post
            }

            val rect = android.graphics.Rect()
            recyclerView.getDecoratedBoundsWithMargins(firstChild, rect)
            val itemHeight = rect.height()

            val totalContentHeight = itemHeight * actualDataCount
            val rvHeight = recyclerView.height

            cachedMaxScroll = (totalContentHeight - rvHeight).coerceAtLeast(0)

            Log.d(TAG, "Measured: itemHeight=$itemHeight, actualDataCount=$actualDataCount, " +
                    "totalContentHeight=$totalContentHeight, rvHeight=$rvHeight, cachedMaxScroll=$cachedMaxScroll")

            if (cachedMaxScroll <= 0) {
                Log.d(TAG, "Nothing to scroll, notifying completion")
                notifyDone()
                return@post
            }

            startLinearScroll()
        }
    }

    private fun notifyDone() {
        handler.postDelayed({
            started = false
            finished = true
            onScrollComplete?.invoke()
        }, keepAtEndDelayMs)
    }

    private fun startLinearScroll() {
        if (!started) return

        val maxScroll = cachedMaxScroll

        // 1. Tentukan kecepatan konstan (misalnya 50 pixel per detik)
        val speedPxPerSecond = 50f

        // 2. Hitung durasi secara dinamis berdasarkan sisa jarak
        val calculatedDurationMs = ((maxScroll / speedPxPerSecond) * 1000).toLong()

        // 3. Batasi durasi agar tidak terlalu cepat (minimal 2 detik) atau terlalu lama (maksimal 15 detik)
        val finalDurationMs = calculatedDurationMs.coerceIn(2000L, 15000L)

        Log.d(TAG, "startLinearScroll: maxScroll=$maxScroll, durationMs=$finalDurationMs")

        // Eksekusi native scroll dengan durasi yang sudah disesuaikan
        recyclerView.smoothScrollBy(0, maxScroll, LinearInterpolator(), finalDurationMs.toInt())

        // Timer untuk menandakan scroll selesai
        handler.postDelayed({
            if (!started) return@postDelayed

            Log.d(TAG, "Scroll complete. Staying at end, notifying in ${keepAtEndDelayMs}ms")
            started = false
            finished = true

            handler.postDelayed({
                onScrollComplete?.invoke()
            }, keepAtEndDelayMs)

        }, finalDurationMs)
    }

    fun stopAutoScroll() {
        if (!started) return
        Log.d(TAG, "stopAutoScroll")
        started = false
        // MENGHENTIKAN NATIVE SCROLL ANIMATION
        recyclerView.stopScroll()
        handler.removeCallbacksAndMessages(null)
    }

    fun cleanup() {
        Log.d(TAG, "cleanup")
        stopAutoScroll()
    }
}