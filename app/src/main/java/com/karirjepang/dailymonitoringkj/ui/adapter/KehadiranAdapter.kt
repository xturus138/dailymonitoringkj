package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.karirjepang.dailymonitoringkj.core.model.Kehadiran
import com.karirjepang.dailymonitoringkj.databinding.ItemKehadiranBinding
import kotlin.math.min

class KehadiranAdapter(private var listKehadiran: List<Kehadiran>) :
    RecyclerView.Adapter<KehadiranAdapter.ViewHolder>() {

    private var availableWidthNama = 0
    private var availableWidthStatus = 0
    private var availableWidthKeterangan = 0

    fun setAvailableWidths(nama: Int, status: Int, keterangan: Int) {
        if (availableWidthNama != nama || availableWidthStatus != status || availableWidthKeterangan != keterangan) {
            availableWidthNama = nama
            availableWidthStatus = status
            availableWidthKeterangan = keterangan
            notifyDataSetChanged()
        }
    }

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

            bindMarqueeWithDelay(holder.binding.tvNamaStaff, item.nama, availableWidthNama, true)
            bindMarqueeWithDelay(holder.binding.tvStatus, item.status, availableWidthStatus, true)
            bindMarqueeWithDelay(holder.binding.tvKeterangan, item.keterangan ?: "", availableWidthKeterangan, true)

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

            bindMarqueeWithDelay(holder.binding.tvNamaStaff, null, 0, false)
            bindMarqueeWithDelay(holder.binding.tvStatus, null, 0, false)
            bindMarqueeWithDelay(holder.binding.tvKeterangan, null, 0, false)

            // Sembunyikan pemisah
            holder.binding.tvSeparator1.visibility = android.view.View.INVISIBLE
            holder.binding.tvSeparator2.visibility = android.view.View.INVISIBLE
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
            textView.ellipsize = android.text.TextUtils.TruncateAt.END // Just in case it's actually 3 lines
            textView.text = content
        } else {
            // Fallback: 1 line marquee
            textView.maxLines = 1
            textView.isSingleLine = true
            textView.ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
            
            // Flatten for cleaner marquee
            textView.text = content.replace("\n", " • ").replace("  ", " ").trim()

            val runnable = Runnable { textView.isSelected = true }
            textView.setTag(runnable)
            textView.postDelayed(runnable, 2_000L)
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

    fun getPageCount(pageSize: Int, sourceSize: Int): Int {
        if (pageSize <= 0 || sourceSize <= 0) return 0
        return (sourceSize + pageSize - 1) / pageSize
    }

    fun getPageData(source: List<Kehadiran>, pageSize: Int, pageIndex: Int): List<Kehadiran> {
        if (source.isEmpty() || pageSize <= 0) return emptyList()

        val pageCount = getPageCount(pageSize, source.size)
        if (pageCount <= 0) return emptyList()

        val safePage = pageIndex % pageCount
        val start = safePage * pageSize
        val end = min(start + pageSize, source.size)
        return source.subList(start, end)
    }
}