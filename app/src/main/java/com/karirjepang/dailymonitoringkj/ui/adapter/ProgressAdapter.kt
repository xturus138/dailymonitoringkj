package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.karirjepang.dailymonitoringkj.core.model.ProgressDivisi
import com.karirjepang.dailymonitoringkj.databinding.ItemProgressBinding

class ProgressAdapter(private var listProgress: List<ProgressDivisi>) :
    RecyclerView.Adapter<ProgressAdapter.ViewHolder>() {

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
            holder.binding.tvPersentase.text = item.persentase
        } else {
            holder.binding.tvNamaDivisi.text = ""
            holder.binding.tvProject.text = ""
            holder.binding.tvPersentase.text = ""
        }
    }

    override fun getItemCount(): Int = 10

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<ProgressDivisi>) {
        listProgress = newData
        notifyDataSetChanged()
    }
}