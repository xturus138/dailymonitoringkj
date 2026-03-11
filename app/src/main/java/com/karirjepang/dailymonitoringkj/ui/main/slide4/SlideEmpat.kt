package com.karirjepang.dailymonitoringkj.ui.main.slide4

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.karirjepang.dailymonitoringkj.databinding.FragmentSlideEmpatBinding
import com.karirjepang.dailymonitoringkj.ui.adapter.MitraAdapter
import com.karirjepang.dailymonitoringkj.ui.util.ContinuousScrollManager
import com.karirjepang.dailymonitoringkj.ui.util.SmoothLinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class SlideEmpat : Fragment() {

    private val viewModel: SlideEmpatViewModel by viewModels()
    private lateinit var mitraAdapter: MitraAdapter

    private var _binding: FragmentSlideEmpatBinding? = null
    private val binding get() = _binding!!

    private val spanCount = 5

    // Sistem Auto-Scroll
    private var autoScrollManager: ContinuousScrollManager? = null
    private var scrollFinishedDeferred: CompletableDeferred<Unit> = CompletableDeferred()
    private var dataRowCount: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideEmpatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mitraAdapter = MitraAdapter(emptyList(), spanCount)

        // Ubah ke SmoothLinearLayoutManager agar auto-scroll halus
        binding.rvMitra.layoutManager = SmoothLinearLayoutManager(requireContext())
        binding.rvMitra.adapter = mitraAdapter

        observeData()
    }

    private fun observeData() {
        viewModel.mitraList.observe(viewLifecycleOwner) { data ->
            mitraAdapter.updateData(data)
            // Karena di MitraAdapter data sudah di-chunk (dikelompokkan per baris),
            // itemCount adalah jumlah baris aktualnya.
            dataRowCount = mitraAdapter.itemCount
            rebuildScrollManager()
        }
    }

    private fun rebuildScrollManager() {
        autoScrollManager?.stopAutoScroll()
        autoScrollManager = null

        if (dataRowCount <= 0) {
            setupScrollFinishedWatcher(null)
            return
        }

        autoScrollManager = ContinuousScrollManager(
            recyclerView = binding.rvMitra,
            delayMillis = 2000,
            actualDataCount = dataRowCount
        )

        if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            autoScrollManager?.startAutoScroll()
        }

        setupScrollFinishedWatcher(autoScrollManager)
    }

    private fun setupScrollFinishedWatcher(manager: ContinuousScrollManager?) {
        val deferred = CompletableDeferred<Unit>()
        scrollFinishedDeferred = deferred

        if (manager == null) {
            deferred.complete(Unit)
            return
        }

        manager.setOnScrollCompleteListener {
            if (!deferred.isCompleted) deferred.complete(Unit)
        }
    }

    suspend fun awaitScrollFinished() {
        while (!scrollFinishedDeferred.isCompleted) {
            kotlinx.coroutines.delay(200)
        }
    }

    override fun onResume() {
        super.onResume()
        autoScrollManager?.startAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        autoScrollManager?.stopAutoScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoScrollManager?.cleanup()
        _binding = null
    }
}