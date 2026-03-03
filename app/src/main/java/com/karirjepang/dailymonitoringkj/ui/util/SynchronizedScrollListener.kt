package com.karirjepang.dailymonitoringkj.ui.util

import androidx.recyclerview.widget.RecyclerView

class SynchronizedScrollListener(private val targetRecyclerView: RecyclerView) : RecyclerView.OnScrollListener() {

    private var isScrolling = false

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (isScrolling) return

        isScrolling = true
        targetRecyclerView.scrollBy(dx, dy)
        isScrolling = false
    }
}
