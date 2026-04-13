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

    private var availableWidthJudul = 0

    fun setAvailableWidth(judul: Int) {
        if (availableWidthJudul != judul) {
            availableWidthJudul = judul
            notifyDataSetChanged()
        }
    }

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
            bindMarqueeWithDelay(holder.binding.tvJudulMeeting, item.judul ?: "", availableWidthJudul, true)
            holder.binding.tvSeparator.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.tvWaktu.text = ""
            holder.binding.tvJudulMeeting.text = ""
            bindMarqueeWithDelay(holder.binding.tvJudulMeeting, null, 0, false)
            holder.binding.tvSeparator.visibility = android.view.View.INVISIBLE
        }
    }

    private fun bindMarqueeWithDelay(textView: TextView, content: String?, availableWidth: Int, enabled: Boolean) {
        val previous = textView.getTag() as? Runnable
        if (previous != null) textView.removeCallbacks(previous)
        textView.setTag(null)

        textView.isSelected = false
        
        if (!enabled || content.isNullOrBlank() || availableWidth <= 0) {
            textView.maxLines = 1
            textView.isSingleLine = true
            textView.ellipsize = null
            return
        }

        // Check if it fits in 2 lines
        val fitsIn2 = com.karirjepang.dailymonitoringkj.ui.util.MarqueeUtils.fitsInLines(content, textView, availableWidth, 2)

        if (fitsIn2) {
            // Priority: 2 lines static
            textView.maxLines = 2
            textView.isSingleLine = false
            textView.ellipsize = android.text.TextUtils.TruncateAt.END
            textView.text = content
        } else {
            // Fallback: 1 line marquee
            textView.maxLines = 1
            textView.isSingleLine = true
            textView.ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
            
            // Flatten
            textView.text = content.replace("\n", " • ").replace("  ", " ").trim()

            val runnable = Runnable { textView.isSelected = true }
            textView.setTag(runnable)
            textView.postDelayed(runnable, 2_000L)
        }
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