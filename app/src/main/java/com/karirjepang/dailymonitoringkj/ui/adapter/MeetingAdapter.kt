package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.karirjepang.dailymonitoringkj.core.model.Meeting
import com.karirjepang.dailymonitoringkj.databinding.ItemMeetingBinding

class MeetingAdapter(private var listMeeting: List<Meeting>) :
    RecyclerView.Adapter<MeetingAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemMeetingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMeetingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < listMeeting.size) {
            val item = listMeeting[position]
            holder.binding.tvWaktu.text = item.waktu
            holder.binding.tvJudulMeeting.text = item.judul
        } else {
            holder.binding.tvWaktu.text = ""
            holder.binding.tvJudulMeeting.text = ""
        }
    }

    override fun getItemCount(): Int = 10

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Meeting>) {
        listMeeting = newData
        notifyDataSetChanged()
    }
}