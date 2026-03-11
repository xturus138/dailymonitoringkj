package com.karirjepang.dailymonitoringkj.ui.main.slide2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.karirjepang.dailymonitoringkj.R
import com.karirjepang.dailymonitoringkj.databinding.FragmentSlideDuaBinding
import com.karirjepang.dailymonitoringkj.ui.adapter.ProgressDualAdapter
import com.karirjepang.dailymonitoringkj.ui.util.ContinuousScrollManager
import com.karirjepang.dailymonitoringkj.ui.util.SmoothLinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class SlideDua : Fragment() {

    private val viewModel: SlideDuaViewModel by viewModels()

    private lateinit var progressAdapter: ProgressDualAdapter

    private var _binding: FragmentSlideDuaBinding? = null
    private val binding get() = _binding!!

    private var autoScrollManager: ContinuousScrollManager? = null

    private var scrollFinishedDeferred: CompletableDeferred<Unit> = CompletableDeferred()

    private var dataRowCount: Int = 0

    private val TAG = "SlideDua"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideDuaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressAdapter = ProgressDualAdapter(emptyList())
        binding.rvProgress.layoutManager = SmoothLinearLayoutManager(requireContext())
        binding.rvProgress.adapter = progressAdapter
        binding.rvProgress.setHasFixedSize(true)
        binding.rvProgress.itemAnimator = null
        binding.rvProgress.setItemViewCacheSize(20)

        binding.rvProgress.post { updateVisibleCount() }

        observeData()
    }

    /** Calculate how many rows fit in the RecyclerView.
     *  Row height = 56dp item + 6dp marginTop = 62dp total per row. */
    private fun updateVisibleCount() {
        if (_binding == null) return
        val rowHeightPx = resources.getDimensionPixelSize(R.dimen.signage_row_height)
        val marginPx = resources.getDimensionPixelSize(R.dimen.dimen_margin_6)
        val itemHeightPx = rowHeightPx + marginPx

        if (itemHeightPx <= 0) return

        val rvHeight = binding.rvProgress.height
        if (rvHeight > 0) {
            progressAdapter.setVisibleItemCount(rvHeight / itemHeightPx)
        }
    }

    private fun observeData() {
        viewModel.progressList.observe(viewLifecycleOwner) { data ->
            Log.d(TAG, "progressList observed: size=${data.size}")
            progressAdapter.updateData(data)
            dataRowCount = progressAdapter.getDataRowCount()
            _binding?.rvProgress?.post { updateVisibleCount() }
            rebuildScrollManager()
        }
    }

    /**
     * Exactly the same pattern as Slide 1:
     * ONE RecyclerView, ONE ContinuousScrollManager, smooth Choreographer scroll.
     */
    private fun rebuildScrollManager() {
        autoScrollManager?.stopAutoScroll()
        autoScrollManager = null

        if (dataRowCount <= 0) {
            setupScrollFinishedWatcher(null)
            return
        }

        autoScrollManager = ContinuousScrollManager(
            recyclerView = binding.rvProgress,
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

    suspend fun awaitScrollFinishedWithin(timeoutMs: Long): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            while (!scrollFinishedDeferred.isCompleted) {
                kotlinx.coroutines.delay(200)
            }
            true
        } ?: false
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
