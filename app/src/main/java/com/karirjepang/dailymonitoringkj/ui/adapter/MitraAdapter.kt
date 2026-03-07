package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
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
        listMitra = newData
        notifyDataSetChanged()
    }
}

