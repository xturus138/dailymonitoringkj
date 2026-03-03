package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.karirjepang.dailymonitoringkj.R
import com.karirjepang.dailymonitoringkj.core.model.Mitra
import com.karirjepang.dailymonitoringkj.databinding.ItemMitraBinding

class MitraAdapter(private var listMitra: List<Mitra>) :
    RecyclerView.Adapter<MitraAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMitraBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMitraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listMitra[position]
        holder.binding.tvNamaMitra.text = item.nama

        holder.binding.ivLogoMitra.load(item.logoUrl) {
            crossfade(true)
            placeholder(R.mipmap.ic_launcher)
            error(R.mipmap.ic_launcher)
        }
    }

    override fun getItemCount(): Int = listMitra.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Mitra>) {
        listMitra = newData
        notifyDataSetChanged()
    }
}