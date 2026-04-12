package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.karirjepang.dailymonitoringkj.core.model.Kehadiran
import com.karirjepang.dailymonitoringkj.databinding.ItemKehadiranBinding

class KehadiranAdapter(private var listKehadiran: List<Kehadiran>) :
    RecyclerView.Adapter<KehadiranAdapter.ViewHolder>() {

    // How many rows fit visibly on screen — set from the fragment after first layout
    private var visibleItemCount: Int = 0

    fun setVisibleItemCount(count: Int) {
        if (visibleItemCount != count) {
            visibleItemCount = count
            notifyDataSetChanged()
        }
    }

    class ViewHolder(val binding: ItemKehadiranBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKehadiranBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < listKehadiran.size) {
            val item = listKehadiran[position]
            holder.binding.tvNamaStaff.text = item.nama
            holder.binding.tvStatus.text = item.status
            holder.binding.tvKeterangan.text = item.keterangan ?: ""

            holder.binding.tvNamaStaff.isSelected = true
            holder.binding.tvKeterangan.isSelected = true

            // Munculkan pemisah
            holder.binding.tvSeparator1.visibility = android.view.View.VISIBLE
            holder.binding.tvSeparator2.visibility = android.view.View.VISIBLE
        } else {
            // Placeholder — only shown when data fits within screen
            holder.binding.tvNamaStaff.text = ""
            holder.binding.tvStatus.text = ""
            holder.binding.tvKeterangan.text = ""

            holder.binding.tvNamaStaff.isSelected = false
            holder.binding.tvKeterangan.isSelected = false

            // Sembunyikan pemisah
            holder.binding.tvSeparator1.visibility = android.view.View.INVISIBLE
            holder.binding.tvSeparator2.visibility = android.view.View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        val placeholders = if (visibleItemCount > listKehadiran.size)
            visibleItemCount - listKehadiran.size else 0
        return listKehadiran.size + placeholders
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Kehadiran>) {
        listKehadiran = newData
        notifyDataSetChanged()
    }
}