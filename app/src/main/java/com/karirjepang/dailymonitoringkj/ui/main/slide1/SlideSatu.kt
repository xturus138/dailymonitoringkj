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
import com.karirjepang.dailymonitoringkj.ui.util.AutoScrollManager
import com.karirjepang.dailymonitoringkj.ui.util.SynchronizedScrollListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SlideSatu : Fragment() {

    private val viewModel: SlideSatuViewModel by viewModels()
    private lateinit var kehadiranAdapter: KehadiranAdapter
    private lateinit var meetingAdapter: MeetingAdapter

    private var _binding: FragmentSlideSatuBinding? = null
    private val binding get() = _binding!!

    private var autoScrollKehadiran: AutoScrollManager? = null
    private var autoScrollMeeting: AutoScrollManager? = null

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

        // Setup synchronized scroll between kehadiran and meeting
        binding.rvKehadiran.addOnScrollListener(SynchronizedScrollListener(binding.rvMeeting))
        binding.rvMeeting.addOnScrollListener(SynchronizedScrollListener(binding.rvKehadiran))

        observeData()
    }

    private fun observeData() {
        viewModel.kehadiranList.observe(viewLifecycleOwner) { data ->
            kehadiranAdapter.updateData(data)

            // Setup auto-scroll for kehadiran if needed
            autoScrollKehadiran?.stopAutoScroll()
            if (data.size > 5) {
                autoScrollKehadiran = AutoScrollManager(binding.rvKehadiran, delayMillis = 2000, actualDataCount = data.size)
                autoScrollKehadiran?.startAutoScroll(intervalMillis = 3000)
            }
        }

        viewModel.meetingList.observe(viewLifecycleOwner) { data ->
            meetingAdapter.updateData(data)

            // Setup auto-scroll for meeting if needed
            autoScrollMeeting?.stopAutoScroll()
            if (data.size > 5) {
                autoScrollMeeting = AutoScrollManager(binding.rvMeeting, delayMillis = 2000, actualDataCount = data.size)
                autoScrollMeeting?.startAutoScroll(intervalMillis = 3000)
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
        autoScrollKehadiran?.cleanup()
        autoScrollMeeting?.cleanup()
        _binding = null
    }
}