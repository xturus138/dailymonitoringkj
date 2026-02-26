package com.karirjepang.dailymonitoringkj.ui.main.slide1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.karirjepang.dailymonitoringkj.databinding.FragmentSlideSatuBinding
import com.karirjepang.dailymonitoringkj.ui.adapter.KehadiranAdapter
import com.karirjepang.dailymonitoringkj.ui.adapter.MeetingAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SlideSatu : Fragment() {

    private val viewModel: SlideSatuViewModel by viewModels()
    private lateinit var kehadiranAdapter: KehadiranAdapter
    private lateinit var meetingAdapter: MeetingAdapter

    private var _binding: FragmentSlideSatuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideSatuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        kehadiranAdapter = KehadiranAdapter(emptyList())
        binding.rvKehadiran.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKehadiran.adapter = kehadiranAdapter

        meetingAdapter = MeetingAdapter(emptyList())
        binding.rvMeeting.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMeeting.adapter = meetingAdapter

        observeData()
    }

    private fun observeData() {
        viewModel.kehadiranList.observe(viewLifecycleOwner) { data ->
            kehadiranAdapter.updateData(data)
        }

        viewModel.meetingList.observe(viewLifecycleOwner) { data ->
            meetingAdapter.updateData(data)
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