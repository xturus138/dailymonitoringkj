package com.karirjepang.dailymonitoringkj.ui.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * ItemDecoration that adds a half-column horizontal offset to odd rows
 * for a zig-zag / honeycomb effect.
 *
 * Works together with [CenteringSpanSizeLookup] which handles centering
 * of partial (incomplete) rows via span sizes.
 *
 * Note: The zig-zag offset is only applied to full rows (rows with
 * [spanCount] items). Partial rows are already centered by the span lookup
 * and don't need the offset.
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

        val totalItems = state.itemCount
        val row = position / spanCount
        val colInRow = position % spanCount
        val rowStart = row * spanCount
        val itemsInRow = minOf(spanCount, totalItems - rowStart)

        // Only apply zig-zag offset to full rows on odd row indices
        if (row % 2 == 1 && itemsInRow == spanCount) {
            val columnWidth = parent.width / spanCount
            val halfColumn = columnWidth / 2

            // Only the first item gets the offset — this pushes the entire
            // row to the right because GridLayoutManager lays out items
            // sequentially and the first item's extra left inset shifts
            // subsequent cells.
            if (colInRow == 0) {
                outRect.left = halfColumn
            } else {
                outRect.left = 0
            }
        } else {
            outRect.left = 0
        }
    }
}

