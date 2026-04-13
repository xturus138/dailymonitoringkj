package com.karirjepang.dailymonitoringkj.ui.util

import android.os.Handler
import android.os.Looper

/**
 * Timer-based paging coordinator.
 *
 * - Keeps each page visible for [delayMillis] (or [pageDelayProvider])
 * - Asks the UI layer to run fade-out/data-swap/fade-in via [onPageTransition]
 * - Calls [onScrollComplete] after the last page has also finished its dwell time
 */
class AutoScrollManager(
    private val delayMillis: Long = 10_000L,
    private val totalPages: Int,
    private val onPageTransition: (nextPageIndex: Int, onTransitionFinished: () -> Unit) -> Unit,
    private val pageDelayProvider: ((currentPageIndex: Int) -> Long)? = null
) {

    private val handler = Handler(Looper.getMainLooper())
    private var started = false
    private var currentPage = 0
    private var hasCompletedOneCycle = false
    private var sessionToken = 0
    private var completionExpectedToken = 0

    private var onPageShown: ((Int) -> Unit)? = null
    private var onScrollComplete: (() -> Unit)? = null

    private val pageRunnable = object : Runnable {
        override fun run() {
            if (!started || totalPages <= 1) return

            val expectedToken = sessionToken
            val nextPage = currentPage + 1

            onPageTransition(nextPage) {
                if (!started || expectedToken != sessionToken) return@onPageTransition

                currentPage = nextPage
                onPageShown?.invoke(currentPage)

                if (currentPage >= totalPages - 1) {
                    scheduleCompletionForCurrentPage()
                } else {
                    scheduleNextPage()
                }
            }
        }
    }

    private val completionRunnable = Runnable {
        if (!started || completionExpectedToken != sessionToken || hasCompletedOneCycle) return@Runnable

        hasCompletedOneCycle = true
        started = false
        onScrollComplete?.invoke()
    }

    fun setOnPageShownListener(listener: ((Int) -> Unit)?) {
        onPageShown = listener
    }

    fun setOnScrollCompleteListener(listener: (() -> Unit)?) {
        onScrollComplete = listener
    }

    fun startAutoScroll() {
        if (started) return
        started = true
        currentPage = 0
        hasCompletedOneCycle = false
        sessionToken++

        onPageShown?.invoke(currentPage)

        if (totalPages <= 1) {
            scheduleCompletionForCurrentPage()
            return
        }

        scheduleNextPage()
    }

    private fun scheduleNextPage() {
        handler.removeCallbacks(pageRunnable)
        if (!started) return

        val delayForCurrentPage = pageDelayProvider?.invoke(currentPage) ?: delayMillis
        handler.postDelayed(pageRunnable, delayForCurrentPage.coerceAtLeast(0L))
    }

    private fun scheduleCompletionForCurrentPage() {
        handler.removeCallbacks(completionRunnable)
        if (!started) return

        completionExpectedToken = sessionToken
        val delayForCurrentPage = pageDelayProvider?.invoke(currentPage) ?: delayMillis
        handler.postDelayed(completionRunnable, delayForCurrentPage.coerceAtLeast(0L))
    }

    fun stopAutoScroll() {
        started = false
        sessionToken++
        handler.removeCallbacks(pageRunnable)
        handler.removeCallbacks(completionRunnable)
    }

    fun cleanup() {
        stopAutoScroll()
    }
}
