package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.karirjepang.dailymonitoringkj.core.model.Kehadiran
import com.karirjepang.dailymonitoringkj.databinding.ItemKehadiranBinding

class KehadiranAdapter(private var listKehadiran: List<Kehadiran>) :
    RecyclerView.Adapter<KehadiranAdapter.ViewHolder>() {

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
            holder.binding.tvKeterangan.text = item.keterangan
        } else {
            holder.binding.tvNamaStaff.text = ""
            holder.binding.tvStatus.text = ""
            holder.binding.tvKeterangan.text = ""
        }
    }

    override fun getItemCount(): Int = 10

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Kehadiran>) {
        listKehadiran = newData
        notifyDataSetChanged()
    }
}