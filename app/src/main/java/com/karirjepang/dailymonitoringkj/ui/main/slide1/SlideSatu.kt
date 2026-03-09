package com.karirjepang.dailymonitoringkj.ui.main.slide1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.karirjepang.dailymonitoringkj.R
import com.karirjepang.dailymonitoringkj.databinding.FragmentSlideSatuBinding
import com.karirjepang.dailymonitoringkj.ui.adapter.KehadiranAdapter
import com.karirjepang.dailymonitoringkj.ui.adapter.MeetingAdapter
import com.karirjepang.dailymonitoringkj.ui.util.ContinuousScrollManager
import com.karirjepang.dailymonitoringkj.ui.util.SynchronizedScrollListener
import com.karirjepang.dailymonitoringkj.ui.util.SmoothLinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class SlideSatu : Fragment() {

    private val viewModel: SlideSatuViewModel by viewModels()
    private lateinit var kehadiranAdapter: KehadiranAdapter
    private lateinit var meetingAdapter: MeetingAdapter

    private var _binding: FragmentSlideSatuBinding? = null
    private val binding get() = _binding!!

    // Only ONE manager drives scrolling; meeting mirrors kehadiran via SynchronizedScrollListener
    private var autoScrollManager: ContinuousScrollManager? = null
    private var syncKehadiranToMeeting: SynchronizedScrollListener? = null

    private var kehadiranDataSize: Int = 0
    private var meetingDataSize: Int = 0

    private var scrollFinishedDeferred: CompletableDeferred<Unit> = CompletableDeferred()

    private val TAG = "SlideSatu"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideSatuBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        kehadiranAdapter = KehadiranAdapter(emptyList())
        binding.rvKehadiran.layoutManager = SmoothLinearLayoutManager(requireContext())
        binding.rvKehadiran.adapter = kehadiranAdapter

        meetingAdapter = MeetingAdapter(emptyList())
        binding.rvMeeting.layoutManager = SmoothLinearLayoutManager(requireContext())
        binding.rvMeeting.adapter = meetingAdapter

        binding.rvKehadiran.post { updateVisibleCounts() }
        binding.rvMeeting.post { updateVisibleCounts() }

        observeData()
    }

    /** Calculate how many rows fit in each RecyclerView and tell the adapters.
     *  Row height = 56dp item + 6dp marginTop = 62dp total per row. */
    private fun updateVisibleCounts() {
        val rowHeightPx = resources.getDimensionPixelSize(R.dimen.signage_row_height)
        val marginPx = resources.getDimensionPixelSize(R.dimen.dimen_margin_6)
        val itemHeightPx = rowHeightPx + marginPx

        if (itemHeightPx <= 0) return

        val rvKehadiranHeight = binding.rvKehadiran.height
        if (rvKehadiranHeight > 0) {
            kehadiranAdapter.setVisibleItemCount(rvKehadiranHeight / itemHeightPx)
        }

        val rvMeetingHeight = binding.rvMeeting.height
        if (rvMeetingHeight > 0) {
            meetingAdapter.setVisibleItemCount(rvMeetingHeight / itemHeightPx)
        }
    }

    private fun observeData() {
        Log.d(TAG, "observeData: setting up observers")

        viewModel.kehadiranList.observe(viewLifecycleOwner) { data ->
            Log.d(TAG, "kehadiranList observed: size=${data.size}, lifecycle=${viewLifecycleOwner.lifecycle.currentState}")
            kehadiranDataSize = data.size
            kehadiranAdapter.updateData(data)
            binding.rvKehadiran.post { updateVisibleCounts() }
            rebuildScrollManager()
        }

        viewModel.meetingList.observe(viewLifecycleOwner) { data ->
            Log.d(TAG, "meetingList observed: size=${data.size}")
            meetingDataSize = data.size
            meetingAdapter.updateData(data)
            binding.rvMeeting.post { updateVisibleCounts() }
            rebuildScrollManager()
        }
    }

    /**
     * Kehadiran is always the driver (it has the larger or equal data count).
     * Meeting mirrors kehadiran's scroll via SynchronizedScrollListener.
     * If meeting is empty, kehadiran scrolls alone (no mirror needed).
     */
    private fun rebuildScrollManager() {
        autoScrollManager?.stopAutoScroll()
        autoScrollManager = null

        syncKehadiranToMeeting?.let { binding.rvKehadiran.removeOnScrollListener(it) }
        syncKehadiranToMeeting = null

        if (kehadiranDataSize <= 0) {
            setupScrollFinishedWatcher(null)
            return
        }

        autoScrollManager = ContinuousScrollManager(
            recyclerView = binding.rvKehadiran,
            delayMillis = 2000,
            actualDataCount = kehadiranDataSize
        )

        // Mirror kehadiran → meeting only when meeting has data
        if (meetingDataSize > 0) {
            syncKehadiranToMeeting = SynchronizedScrollListener(binding.rvMeeting)
            binding.rvKehadiran.addOnScrollListener(syncKehadiranToMeeting!!)
        } else {
            // Reset meeting scroll position to top
            binding.rvMeeting.scrollToPosition(0)
        }

        if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            Log.d(TAG, "Fragment RESUMED, starting autoScroll")
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
        Log.d(TAG, "onResume: calling startAutoScroll")
        autoScrollManager?.startAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: calling stopAutoScroll")
        autoScrollManager?.stopAutoScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        autoScrollManager?.cleanup()
        _binding = null
    }
}
