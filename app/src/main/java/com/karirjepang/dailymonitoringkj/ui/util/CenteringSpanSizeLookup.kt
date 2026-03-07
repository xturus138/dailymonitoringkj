package com.karirjepang.dailymonitoringkj.ui.util

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A [GridLayoutManager.SpanSizeLookup] that centers partial (incomplete) last rows
 * by giving each item in those rows a larger span, so the group appears centered
 * within the grid.
 *
 * For example, with spanCount = 6 and 4 items in the last row, each item gets
 * floor(6/4)=1 span, and the first 6%4=2 items get an extra span → spans: 2,2,1,1 = 6 total.
 *
 * @param spanCount number of columns in the grid
 * @param adapter the RecyclerView adapter (used to get total item count)
 */
class CenteringSpanSizeLookup(
    private val spanCount: Int,
    private val adapter: RecyclerView.Adapter<*>
) : GridLayoutManager.SpanSizeLookup() {

    init {
        isSpanIndexCacheEnabled = true
    }

    override fun getSpanSize(position: Int): Int {
        val totalItems = adapter.itemCount
        if (totalItems == 0) return 1

        val row = position / spanCount
        val rowStart = row * spanCount
        val itemsInRow = minOf(spanCount, totalItems - rowStart)

        // Full row → each item = 1 span
        if (itemsInRow == spanCount) return 1

        // Partial row → distribute spanCount across the items
        val colInRow = position - rowStart
        val baseSpan = spanCount / itemsInRow
        val remainder = spanCount % itemsInRow

        // First 'remainder' items get 1 extra span for even distribution
        return if (colInRow < remainder) baseSpan + 1 else baseSpan
    }
}

