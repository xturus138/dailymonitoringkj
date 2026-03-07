package com.karirjepang.dailymonitoringkj.ui.util

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SmoothLinearLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        val smoothScroller = SmoothLinearScroller(recyclerView.context)
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }
}

