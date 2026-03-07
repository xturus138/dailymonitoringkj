package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.karirjepang.dailymonitoringkj.core.model.ProgressDivisi
import com.karirjepang.dailymonitoringkj.databinding.ItemProgressBinding

class ProgressAdapter(private var listProgress: List<ProgressDivisi>) :
    RecyclerView.Adapter<ProgressAdapter.ViewHolder>() {

    private var visibleItemCount: Int = 0

    /**
     * When set > 0, forces getItemCount() to return at least this value.
     * Used so both left/right tables report the same item count, ensuring
     * identical scroll distances for synchronized scrolling.
     */
    private var forcedItemCount: Int = 0

    fun setVisibleItemCount(count: Int) {
        if (visibleItemCount != count) {
            visibleItemCount = count
            notifyDataSetChanged()
        }
    }

    fun setForcedItemCount(count: Int) {
        if (forcedItemCount != count) {
            forcedItemCount = count
            notifyDataSetChanged()
        }
    }

    class ViewHolder(val binding: ItemProgressBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProgressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < listProgress.size) {
            val item = listProgress[position]
            holder.binding.tvNamaDivisi.text = item.namaDivisi
            holder.binding.tvProject.text = item.projectProgress
            holder.binding.tvPersentase.text = item.persentase.toString()
        } else {
            holder.binding.tvNamaDivisi.text = ""
            holder.binding.tvProject.text = ""
            holder.binding.tvPersentase.text = ""
        }
    }

    override fun getItemCount(): Int {
        val placeholders = if (visibleItemCount > listProgress.size)
            visibleItemCount - listProgress.size else 0
        val baseCount = listProgress.size + placeholders
        // If a forced count is set (for sync-scrolling), use the larger value
        return maxOf(baseCount, forcedItemCount)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<ProgressDivisi>) {
        listProgress = newData
        notifyDataSetChanged()
    }
}