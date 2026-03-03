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
import com.karirjepang.dailymonitoringkj.ui.util.AutoScrollManager
import com.karirjepang.dailymonitoringkj.ui.util.SynchronizedScrollListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SlideDua : Fragment() {

    private val viewModel: SlideDuaViewModel by viewModels()

    private lateinit var progressLeftAdapter: ProgressAdapter
    private lateinit var progressRightAdapter: ProgressAdapter

    private var _binding: FragmentSlideDuaBinding? = null
    private val binding get() = _binding!!

    private var autoScrollProgressLeft: AutoScrollManager? = null
    private var autoScrollProgressRight: AutoScrollManager? = null


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

        // Setup synchronized scroll between left and right progress
        binding.rvProgressLeft.addOnScrollListener(SynchronizedScrollListener(binding.rvProgressRight))
        binding.rvProgressRight.addOnScrollListener(SynchronizedScrollListener(binding.rvProgressLeft))

        observeData()
    }

    private fun observeData() {
        viewModel.progressLeftList.observe(viewLifecycleOwner) { leftData ->
            progressLeftAdapter.updateData(leftData)

            // Setup auto-scroll untuk left if needed
            autoScrollProgressLeft?.stopAutoScroll()
            if (leftData.size > 5) {
                autoScrollProgressLeft = AutoScrollManager(binding.rvProgressLeft, delayMillis = 2000, actualDataCount = leftData.size)
                autoScrollProgressLeft?.startAutoScroll(intervalMillis = 3000)
            }
        }

        viewModel.progressRightList.observe(viewLifecycleOwner) { rightData ->
            progressRightAdapter.updateData(rightData)

            // Setup auto-scroll untuk right if needed
            autoScrollProgressRight?.stopAutoScroll()
            if (rightData.size > 5) {
                autoScrollProgressRight = AutoScrollManager(binding.rvProgressRight, delayMillis = 2000, actualDataCount = rightData.size)
                autoScrollProgressRight?.startAutoScroll(intervalMillis = 3000)
            }
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
        autoScrollProgressLeft?.cleanup()
        autoScrollProgressRight?.cleanup()
        _binding = null
    }
}

