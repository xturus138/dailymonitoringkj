package com.karirjepang.dailymonitoringkj.ui.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Adds a horizontal offset to every odd row in a GridLayoutManager,
 * producing a zig-zag / brick-wall / honeycomb layout.
 *
 * The offset is half a column width, so odd-row items sit between
 * the columns of even rows.
 *
 * @param spanCount number of columns in the grid
 */
class ZigZagItemDecoration(private val spanCount: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val row = position / spanCount

        if (row % 2 == 1) {
            // Odd row: shift right by half a column width
            val columnWidth = parent.width / spanCount
            val offset = columnWidth / 2
            outRect.left = offset
        }
    }
}

