package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.karirjepang.dailymonitoringkj.core.model.Mitra
import com.karirjepang.dailymonitoringkj.databinding.ItemMitraBinding

/**
 * Adapter that reorders Mitra items center-out per row with zig-zag direction.
 *
 * Row 0 (even): center → left  → right → left  → right …
 * Row 1 (odd):  center → right → left  → right → left  …
 *
 * Centering of partial rows is handled externally by ItemDecoration.
 */
class MitraAdapter(
    private var listMitra: List<Mitra> = emptyList(),
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

        holder.binding.ivLogoMitra.visibility = View.INVISIBLE

        holder.binding.ivLogoMitra.load(item.logoUrl) {
            crossfade(true)
            listener(
                onSuccess = { _, _ ->
                    holder.binding.ivLogoMitra.visibility = View.VISIBLE
                },
                onError = { _, _ ->
                    holder.binding.ivLogoMitra.visibility = View.VISIBLE
                }
            )
        }
    }

    override fun getItemCount(): Int = listMitra.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Mitra>) {
        listMitra = reorderCenterOut(newData)
        notifyDataSetChanged()
    }

    /**
     * Reorder data so within each row, items are placed center-out.
     * Even rows zig left-first, odd rows zig right-first.
     */
    private fun reorderCenterOut(data: List<Mitra>): List<Mitra> {
        if (data.isEmpty()) return data

        val result = mutableListOf<Mitra>()
        val totalRows = (data.size + spanCount - 1) / spanCount

        for (row in 0 until totalRows) {
            val startIdx = row * spanCount
            val endIdx = minOf(startIdx + spanCount, data.size)
            val rowItems = data.subList(startIdx, endIdx)
            val cols = rowItems.size

            // Get center-out column order for this row's item count
            val placementOrder = buildCenterOutOrder(cols, leftFirst = row % 2 == 0)

            // placementOrder[i] = grid column for the i-th original item
            // We need: for each grid column 0..cols-1, which item index goes there
            val reordered = arrayOfNulls<Mitra>(cols)
            for ((itemIdx, gridCol) in placementOrder.withIndex()) {
                reordered[gridCol] = rowItems[itemIdx]
            }

            reordered.filterNotNull().forEach { result.add(it) }
        }

        return result
    }

    /**
     * For [count] items in a row, return column assignments center-out.
     * Index = item order (0=first), value = grid column.
     */
    private fun buildCenterOutOrder(count: Int, leftFirst: Boolean): List<Int> {
        if (count == 0) return emptyList()

        val order = mutableListOf<Int>()
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

