package com.karirjepang.dailymonitoringkj.ui.main.slide3

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.karirjepang.dailymonitoringkj.databinding.FragmentSlideTigaBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SlideTiga : Fragment() {

    private val viewModel: SlideTigaViewModel by viewModels()
    private var _binding: FragmentSlideTigaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideTigaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChartAppearance()
        observeData()
    }

    private fun setupChartAppearance() {
        val chart = binding.barChart
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setPinchZoom(false)
        chart.setScaleEnabled(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.setDrawGridLines(true)

        chart.axisRight.isEnabled = false

        val legend = chart.legend
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
        legend.textSize = 12f
        legend.formSize = 12f
    }

    private fun observeData() {
        viewModel.chartData.observe(viewLifecycleOwner) { data ->
            val entriesTokutei = ArrayList<BarEntry>()
            val entriesGijinkoku = ArrayList<BarEntry>()

            for (item in data) {
                entriesTokutei.add(BarEntry(item.tahun, item.tokuteiGinou))
                entriesGijinkoku.add(BarEntry(item.tahun, item.gijinkoku))
            }

            val setTokutei = BarDataSet(entriesTokutei, "Tokutei Ginou")
            setTokutei.color = Color.parseColor("#E74C3C") // Merah
            setTokutei.valueTextSize = 12f

            val setGijinkoku = BarDataSet(entriesGijinkoku, "Gijinkoku")
            setGijinkoku.color = Color.parseColor("#144b78")
            setGijinkoku.valueTextSize = 12f

            val intFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
            setTokutei.valueFormatter = intFormatter
            setGijinkoku.valueFormatter = intFormatter

            val barData = BarData(setTokutei, setGijinkoku)

            val barWidth = 0.35f
            val barSpace = 0.05f
            val groupSpace = 0.20f

            barData.barWidth = barWidth
            binding.barChart.data = barData

            val startYear = data.firstOrNull()?.tahun ?: 2022f
            binding.barChart.groupBars(startYear, groupSpace, barSpace)

            binding.barChart.xAxis.axisMinimum = startYear
            binding.barChart.xAxis.axisMaximum = startYear + data.size

            binding.barChart.invalidate()
            binding.barChart.animateY(1000)
        }

        viewModel.currentDate.observe(viewLifecycleOwner) { date ->
            binding.currentDateTime.text = date
        }

        viewModel.currentTime.observe(viewLifecycleOwner) { time ->
            binding.currentDayTime.text = time
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}