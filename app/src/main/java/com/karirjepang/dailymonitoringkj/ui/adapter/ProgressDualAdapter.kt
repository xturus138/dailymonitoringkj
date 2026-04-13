package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.karirjepang.dailymonitoringkj.core.model.ProgressDivisi
import com.karirjepang.dailymonitoringkj.databinding.ItemProgressDualBinding

/**
 * Single-RecyclerView adapter that displays two columns side-by-side.
 *
 * Each adapter row renders:
 *   - Left column  →  data[position]
 *   - Right column →  data[position + rowCount]
 *
 * where rowCount = ceil(totalData / 2).
 *
 * This gives one smooth scroll because there is only ONE RecyclerView.
 */
class ProgressDualAdapter(
    private var listProgress: List<ProgressDivisi> = emptyList()
) : RecyclerView.Adapter<ProgressDualAdapter.ViewHolder>() {

    /** How many rows are visible on screen — used to pad with empty placeholders */
    private var visibleItemCount: Int = 0

    /** Number of rows in each column (= ceil(totalData / 2)) */
    private var rowCount: Int = 0

    fun setVisibleItemCount(count: Int) {
        if (visibleItemCount != count) {
            visibleItemCount = count
            notifyDataSetChanged()
        }
    }

    class ViewHolder(val binding: ItemProgressDualBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProgressDualBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Left column data index
        val leftIndex = position
        if (leftIndex < rowCount && leftIndex < listProgress.size) {
            val item = listProgress[leftIndex]
            holder.binding.tvNamaDivisiLeft.text = item.namaDivisi
            holder.binding.tvProjectLeft.text = item.projectProgress
            holder.binding.tvPersentaseLeft.text = item.persentase
            bindMarqueeWithDelay(holder.binding.tvNamaDivisiLeft, item.namaDivisi, true)
            bindMarqueeWithDelay(holder.binding.tvProjectLeft, item.projectProgress, true)
            holder.binding.tvSeparatorLeft1.visibility = android.view.View.VISIBLE
            holder.binding.tvSeparatorLeft2.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.tvNamaDivisiLeft.text = ""
            holder.binding.tvProjectLeft.text = ""
            holder.binding.tvPersentaseLeft.text = ""
            bindMarqueeWithDelay(holder.binding.tvNamaDivisiLeft, null, false)
            bindMarqueeWithDelay(holder.binding.tvProjectLeft, null, false)
            holder.binding.tvNamaDivisiLeft.isSelected = false
            holder.binding.tvProjectLeft.isSelected = false
            holder.binding.tvSeparatorLeft1.visibility = android.view.View.INVISIBLE
            holder.binding.tvSeparatorLeft2.visibility = android.view.View.INVISIBLE
        }

        // Right column data index
        val rightIndex = position + rowCount
        if (rightIndex < listProgress.size) {
            val item = listProgress[rightIndex]
            holder.binding.tvNamaDivisiRight.text = item.namaDivisi
            holder.binding.tvProjectRight.text = item.projectProgress
            holder.binding.tvPersentaseRight.text = item.persentase
            bindMarqueeWithDelay(holder.binding.tvNamaDivisiRight, item.namaDivisi, true)
            bindMarqueeWithDelay(holder.binding.tvProjectRight, item.projectProgress, true)
            holder.binding.tvSeparatorRight1.visibility = android.view.View.VISIBLE
            holder.binding.tvSeparatorRight2.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.tvNamaDivisiRight.text = ""
            holder.binding.tvProjectRight.text = ""
            holder.binding.tvPersentaseRight.text = ""
            bindMarqueeWithDelay(holder.binding.tvNamaDivisiRight, null, false)
            bindMarqueeWithDelay(holder.binding.tvProjectRight, null, false)
            holder.binding.tvNamaDivisiRight.isSelected = false
            holder.binding.tvProjectRight.isSelected = false
            holder.binding.tvSeparatorRight1.visibility = android.view.View.INVISIBLE
            holder.binding.tvSeparatorRight2.visibility = android.view.View.INVISIBLE
        }
    }

    private fun bindMarqueeWithDelay(textView: TextView, fullText: String?, enabled: Boolean) {
        val previous = textView.getTag() as? Runnable
        if (previous != null) textView.removeCallbacks(previous)
        textView.setTag(null)

        textView.isSelected = false

        if (!enabled || fullText.isNullOrBlank()) {
            textView.text = ""
            return
        }

        // Flatten multi-line text into a single line so native marquee applies perfectly
        val flattenedText = fullText.replace("\n", " • ").replace("  ", " ").trim()
        textView.text = flattenedText

        val runnable = object : Runnable {
            override fun run() {
                val currentRunnable = this
                if (textView.getTag() === currentRunnable) {
                    textView.isSelected = true
                }
            }
        }
        textView.setTag(runnable)
        textView.postDelayed(runnable, 2000L)
    }

    override fun getItemCount(): Int {
        // Pad to fill the screen when data is less than visible rows
        return maxOf(rowCount, visibleItemCount)
    }

    /** Total real data rows (= rowCount, the left-column count). Used by ContinuousScrollManager. */
    fun getDataRowCount(): Int = rowCount

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<ProgressDivisi>) {
        if (listProgress == newData) return
        listProgress = newData
        rowCount = if (newData.isEmpty()) 0 else (newData.size + 1) / 2
        notifyDataSetChanged()
    }
}
