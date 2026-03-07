package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.karirjepang.dailymonitoringkj.core.model.Mitra
import com.karirjepang.dailymonitoringkj.databinding.ItemMitraBinding

class MitraAdapter(
    private var listMitra: List<Mitra>,
    private val spanCount: Int = 6
) : RecyclerView.Adapter<MitraAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMitraBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMitraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listMitra[position]
        holder.binding.tvNamaMitra.text = item.nama

        // Hide image until it's fully loaded to avoid the ic_launcher placeholder flash
        holder.binding.ivLogoMitra.visibility = View.INVISIBLE

        holder.binding.ivLogoMitra.load(item.logoUrl) {
            crossfade(true)
            listener(
                onSuccess = { _, _ ->
                    holder.binding.ivLogoMitra.visibility = View.VISIBLE
                },
                onError = { _, _ ->
                    // Show placeholder icon on error
                    holder.binding.ivLogoMitra.visibility = View.VISIBLE
                }
            )
        }
    }

    override fun getItemCount(): Int = listMitra.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Mitra>) {
        listMitra = reorderCenterOutZigZag(newData)
        notifyDataSetChanged()
    }

    /**
     * Reorder items so that within each row, the first items in the
     * original list are placed at the center columns, then expand outward.
     *
     * Even rows (0,2,4…): center → left → right → left → right …
     * Odd  rows (1,3,5…): center → right → left → right → left …
     *
     * Example with spanCount=6, original row data [A,B,C,D,E,F]:
     *   Row 0 (even): grid positions → [ E, C, A, B, D, F ]
     *                  (center cols 2,3 get A,B first, then expand L,R)
     *   Row 1 (odd):  grid positions → [ F, D, B, A, C, E ]
     *                  (center cols 2,3 get A,B first, then expand R,L)
     */
    private fun reorderCenterOutZigZag(data: List<Mitra>): List<Mitra> {
        if (data.isEmpty()) return data

        val result = mutableListOf<Mitra>()
        val totalRows = (data.size + spanCount - 1) / spanCount

        for (row in 0 until totalRows) {
            val startIdx = row * spanCount
            val endIdx = minOf(startIdx + spanCount, data.size)
            val rowItems = data.subList(startIdx, endIdx) // items in original order
            val cols = rowItems.size

            // Build the column placement order: center-out with zig-zag direction
            val placementOrder = buildCenterOutOrder(cols, leftFirst = row % 2 == 0)

            // placementOrder[i] = the grid column where the i-th item of this row should go
            // We need the inverse: for each grid column (0..cols-1), which item index fills it
            val reordered = Array<Mitra?>(cols) { null }
            for ((itemIdx, gridCol) in placementOrder.withIndex()) {
                reordered[gridCol] = rowItems[itemIdx]
            }

            reordered.forEach { it?.let { mitra -> result.add(mitra) } }
        }

        return result
    }

    /**
     * Returns a list where index = item order (0=first item, 1=second, …),
     * value = grid column position where that item is placed.
     *
     * Items fill from center outward.
     * @param leftFirst if true, after center go left first; if false, right first.
     */
    private fun buildCenterOutOrder(count: Int, leftFirst: Boolean): List<Int> {
        if (count == 0) return emptyList()

        val order = mutableListOf<Int>() // each entry = grid column for the next item

        // Start with center column(s)
        val midLeft = (count - 1) / 2
        order.add(midLeft)

        if (count % 2 == 0) {
            order.add(midLeft + 1)
        }

        var left = midLeft - 1
        var right = if (count % 2 == 0) midLeft + 2 else midLeft + 1
        var goLeft = leftFirst

        while (left >= 0 || right < count) {
            if (goLeft) {
                if (left >= 0) order.add(left--)
                if (right < count) order.add(right++)
            } else {
                if (right < count) order.add(right++)
                if (left >= 0) order.add(left--)
            }
            goLeft = !goLeft
        }

        return order
    }
}

