package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.karirjepang.dailymonitoringkj.core.model.Meeting
import com.karirjepang.dailymonitoringkj.databinding.ItemMeetingBinding

class MeetingAdapter(private var listMeeting: List<Meeting>) :
    RecyclerView.Adapter<MeetingAdapter.ViewHolder>() {

    private var visibleItemCount: Int = 0

    fun setVisibleItemCount(count: Int) {
        if (visibleItemCount != count) {
            visibleItemCount = count
            notifyDataSetChanged()
        }
    }

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
            bindMarqueeWithDelay(holder.binding.tvJudulMeeting, true)
            holder.binding.tvSeparator.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.tvWaktu.text = ""
            holder.binding.tvJudulMeeting.text = ""
            bindMarqueeWithDelay(holder.binding.tvJudulMeeting, false)
            holder.binding.tvSeparator.visibility = android.view.View.INVISIBLE
        }
    }

    private fun bindMarqueeWithDelay(textView: TextView, enabled: Boolean) {
        val previous = textView.getTag() as? Runnable
        if (previous != null) textView.removeCallbacks(previous)

        textView.isSelected = false
        if (!enabled || textView.text.isNullOrBlank()) {
            textView.setTag(null)
            return
        }

        val runnable = Runnable { textView.isSelected = true }
        textView.setTag(runnable)
        textView.postDelayed(runnable, 2_000L)
    }

    override fun getItemCount(): Int {
        val placeholders = if (visibleItemCount > listMeeting.size)
            visibleItemCount - listMeeting.size else 0
        return listMeeting.size + placeholders
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Meeting>) {
        listMeeting = newData
        notifyDataSetChanged()
    }
}