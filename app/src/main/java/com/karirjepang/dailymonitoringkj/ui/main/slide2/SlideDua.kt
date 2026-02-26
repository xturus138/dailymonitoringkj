package com.karirjepang.dailymonitoringkj.ui.main.slide2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.karirjepang.dailymonitoringkj.databinding.FragmentSlideDuaBinding
import com.karirjepang.dailymonitoringkj.ui.adapter.ProgressAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SlideDua : Fragment() {

    private val viewModel: SlideDuaViewModel by viewModels()

    private lateinit var progressLeftAdapter: ProgressAdapter
    private lateinit var progressRightAdapter: ProgressAdapter

    private var _binding: FragmentSlideDuaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideDuaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView Kiri
        progressLeftAdapter = ProgressAdapter(emptyList())
        binding.rvProgressLeft.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProgressLeft.adapter = progressLeftAdapter

        // Setup RecyclerView Kanan
        progressRightAdapter = ProgressAdapter(emptyList())
        binding.rvProgressRight.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProgressRight.adapter = progressRightAdapter

        observeData()
    }

    private fun observeData() {
        viewModel.progressList.observe(viewLifecycleOwner) { data ->
            val midPoint = (data.size + 1) / 2
            val leftData = data.take(midPoint)
            val rightData = data.drop(midPoint)

            progressLeftAdapter.updateData(leftData)
            progressRightAdapter.updateData(rightData)
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