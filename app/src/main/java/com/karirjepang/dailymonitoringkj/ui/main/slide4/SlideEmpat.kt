package com.karirjepang.dailymonitoringkj.ui.main.slide4

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.karirjepang.dailymonitoringkj.databinding.FragmentSlideEmpatBinding
import com.karirjepang.dailymonitoringkj.ui.adapter.MitraAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SlideEmpat : Fragment() {

    private val viewModel: SlideEmpatViewModel by viewModels()
    private lateinit var mitraAdapter: MitraAdapter

    private var _binding: FragmentSlideEmpatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideEmpatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mitraAdapter = MitraAdapter(emptyList())
        binding.rvMitra.layoutManager = GridLayoutManager(requireContext(), 6)
        binding.rvMitra.adapter = mitraAdapter

        observeData()
    }

    private fun observeData() {
        viewModel.mitraList.observe(viewLifecycleOwner) { data ->
            mitraAdapter.updateData(data)
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