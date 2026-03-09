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
import com.karirjepang.dailymonitoringkj.ui.util.CenteringSpanSizeLookup
import com.karirjepang.dailymonitoringkj.ui.util.ZigZagItemDecoration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SlideEmpat : Fragment() {

    private val viewModel: SlideEmpatViewModel by viewModels()
    private lateinit var mitraAdapter: MitraAdapter

    private var _binding: FragmentSlideEmpatBinding? = null
    private val binding get() = _binding!!

    private val spanCount = 5

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideEmpatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mitraAdapter = MitraAdapter(emptyList(), spanCount)
        val gridLayoutManager = GridLayoutManager(requireContext(), spanCount)
        gridLayoutManager.spanSizeLookup = CenteringSpanSizeLookup(spanCount, mitraAdapter)
        binding.rvMitra.layoutManager = gridLayoutManager
        binding.rvMitra.adapter = mitraAdapter
        binding.rvMitra.addItemDecoration(ZigZagItemDecoration(spanCount))

        observeData()
    }

    private fun observeData() {
        viewModel.mitraList.observe(viewLifecycleOwner) { data ->
            mitraAdapter.updateData(data)
            // Invalidate span lookup cache since item count changed
            (binding.rvMitra.layoutManager as? GridLayoutManager)
                ?.spanSizeLookup?.invalidateSpanIndexCache()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}