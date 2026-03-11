package com.karirjepang.dailymonitoringkj.ui.adapter

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.karirjepang.dailymonitoringkj.core.model.Mitra
import com.karirjepang.dailymonitoringkj.databinding.ItemMitraBinding
import kotlin.math.min

class MitraAdapter(
    private var listMitra: List<Mitra> = emptyList(),
    private val spanCount: Int = 5
) : RecyclerView.Adapter<MitraAdapter.ViewHolder>() {

    private var rows: List<List<Mitra>> = emptyList()

    class ViewHolder(val rowContainer: LinearLayout) : RecyclerView.ViewHolder(rowContainer)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowContainer = LinearLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            clipToPadding = false
            clipChildren = false
        }
        return ViewHolder(rowContainer)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rowItems = rows[position]
        val context = holder.rowContainer.context

        holder.rowContainer.removeAllViews()

        // Baris ke-1, 3, 5 (Ganjil secara urutan visual)
        val isOddRow = (position % 2 == 0)
        val maxItemsInThisRow = if (isOddRow) spanCount else (spanCount - 1)

        // 1. PENGATURAN RASIO (WEIGHT)
        // Logika ini menghilangkan masalah "terlalu kanan", karena ruang dibagi berdasarkan rasio layar asli.
        val itemWeight = 2f
        val edgeSpacerWeight = 1.0f // Anda bisa ubah ke 1.5f atau 2.0f jika grid ingin lebih merapat ke tengah

        // Total ruang (Rasio) selalu tetap agar grid atas dan bawah sejajar sempurna
        val totalWeightSum = (edgeSpacerWeight * 2) + (spanCount * itemWeight)
        holder.rowContainer.weightSum = totalWeightSum

        // 2. SPACER KIRI (Pendorong)
        // Jika baris genap, spacer kiri otomatis membesar setengah ukuran item agar tercipta Zigzag.
        val leftWeight = if (isOddRow) edgeSpacerWeight else (edgeSpacerWeight + (itemWeight / 2f))
        holder.rowContainer.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, leftWeight)
        })

        // 3. ISI ITEM (SISTEM SLOT)
        // Membuat "slot" imajiner agar data tetap lurus meski jumlahnya sedikit (misal: hanya 2 item).
        val slotItems = arrayOfNulls<Mitra>(maxItemsInThisRow)
        val startIndex = (maxItemsInThisRow - rowItems.size) / 2
        for (i in rowItems.indices) {
            slotItems[startIndex + i] = rowItems[i]
        }

        for (item in slotItems) {
            if (item != null) {
                val itemBinding = ItemMitraBinding.inflate(LayoutInflater.from(context), holder.rowContainer, false)

                // Kunci lebar menggunakan weight (0dp), bukan piksel mutlak.
                itemBinding.root.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, itemWeight)

                itemBinding.tvNamaMitra.text = item.nama
                itemBinding.ivLogoMitra.visibility = View.INVISIBLE

                itemBinding.ivLogoMitra.load(item.logoUrl) {
                    crossfade(true)
                    listener(
                        onSuccess = { _, _ -> itemBinding.ivLogoMitra.visibility = View.VISIBLE },
                        onError = { _, _ -> itemBinding.ivLogoMitra.visibility = View.VISIBLE }
                    )
                }
                holder.rowContainer.addView(itemBinding.root)
            } else {
                // Slot Kosong (Menjaga grid tetap lurus jika item kurang dari kapasitas maksimal)
                holder.rowContainer.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 1, itemWeight)
                })
            }
        }

        // 4. SPACER KANAN (Penyeimbang)
        val rightWeight = if (isOddRow) edgeSpacerWeight else (edgeSpacerWeight + (itemWeight / 2f))
        holder.rowContainer.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, rightWeight)
        })
    }

    override fun getItemCount(): Int = rows.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Mitra>) {
        listMitra = newData
        val newRows = mutableListOf<List<Mitra>>()
        var i = 0
        var isOddRow = true

        while (i < newData.size) {
            val chunkSize = if (isOddRow) spanCount else (spanCount - 1)
            val end = min(i + chunkSize, newData.size)
            newRows.add(newData.subList(i, end))
            i += chunkSize
            isOddRow = !isOddRow
        }

        rows = newRows
        notifyDataSetChanged()
    }
}