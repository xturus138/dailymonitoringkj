package com.karirjepang.dailymonitoringkj.ui.main.slide1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.karirjepang.dailymonitoringkj.R
import com.karirjepang.dailymonitoringkj.core.model.Kehadiran
import com.karirjepang.dailymonitoringkj.core.model.Meeting
import com.karirjepang.dailymonitoringkj.databinding.FragmentSlideSatuBinding
import com.karirjepang.dailymonitoringkj.ui.adapter.KehadiranAdapter
import com.karirjepang.dailymonitoringkj.ui.adapter.MeetingAdapter
import com.karirjepang.dailymonitoringkj.ui.util.AutoScrollManager
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

    private var pagingManager: AutoScrollManager? = null

    private var fullKehadiran: List<Kehadiran> = emptyList()
    private var fullMeeting: List<Meeting> = emptyList()
    private var kehadiranRowsPerPage: Int = 0
    private var meetingRowsPerPage: Int = 0

    private var scrollFinishedDeferred: CompletableDeferred<Unit> = CompletableDeferred()

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

        binding.rvKehadiran.post { updateVisibleCounts() }
        binding.rvMeeting.post { updateVisibleCounts() }

        observeData()
    }

    private fun updateVisibleCounts() {
        if (_binding == null) return

        val rowHeightPx = resources.getDimensionPixelSize(R.dimen.signage_row_height)
        val marginPx = resources.getDimensionPixelSize(R.dimen.dimen_margin_6)
        val itemHeightPx = rowHeightPx + marginPx
        if (itemHeightPx <= 0) return

        val rvKehadiranHeight = binding.rvKehadiran.height
        if (rvKehadiranHeight > 0) {
            kehadiranRowsPerPage = (rvKehadiranHeight / itemHeightPx).coerceAtLeast(1)
            kehadiranAdapter.setVisibleItemCount(kehadiranRowsPerPage)
        }

        val rvMeetingHeight = binding.rvMeeting.height
        if (rvMeetingHeight > 0) {
            meetingRowsPerPage = (rvMeetingHeight / itemHeightPx).coerceAtLeast(1)
            meetingAdapter.setVisibleItemCount(meetingRowsPerPage)
        }

        rebuildPagingManagerIfReady()
    }

    private fun observeData() {
        viewModel.kehadiranList.observe(viewLifecycleOwner) { data ->
            fullKehadiran = data
            rebuildPagingManagerIfReady()
        }

        viewModel.meetingList.observe(viewLifecycleOwner) { data ->
            fullMeeting = data
            rebuildPagingManagerIfReady()
        }
    }

    private fun rebuildPagingManagerIfReady() {
        if (_binding == null) return
        if (kehadiranRowsPerPage <= 0 || meetingRowsPerPage <= 0) return

        pagingManager?.stopAutoScroll()
        pagingManager = null

        val kehadiranPages = calculatePageCount(fullKehadiran.size, kehadiranRowsPerPage)
        val meetingPages = calculatePageCount(fullMeeting.size, meetingRowsPerPage)
        val totalPages = maxOf(kehadiranPages, meetingPages)

        if (totalPages <= 0) {
            applyPage(0)
            setupScrollFinishedWatcher(null)
            return
        }

        pagingManager = AutoScrollManager(
            delayMillis = 10_000L,
            totalPages = totalPages,
            onPageTransition = { nextPage, onTransitionFinished ->
                runFadeTransition(nextPage, onTransitionFinished)
            },
            pageDelayProvider = { currentPage ->
                calculateDwellDelayForPage(currentPage)
            }
        ).also { manager ->
            manager.setOnPageShownListener { pageIndex ->
                applyPage(pageIndex)
            }

            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                manager.startAutoScroll()
            }
        }

        setupScrollFinishedWatcher(pagingManager)
    }

    private fun runFadeTransition(nextPage: Int, onFinished: () -> Unit) {
        if (_binding == null) {
            onFinished()
            return
        }

        val target = binding.containerTable
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out_slow)
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_slow)

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) = Unit

            override fun onAnimationEnd(animation: Animation?) {
                if (_binding == null) {
                    onFinished()
                    return
                }

                applyPage(nextPage)
                target.clearAnimation()

                fadeIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) = Unit
                    override fun onAnimationRepeat(animation: Animation?) = Unit
                    override fun onAnimationEnd(animation: Animation?) {
                        target.clearAnimation()
                        onFinished()
                    }
                })
                target.startAnimation(fadeIn)
            }

            override fun onAnimationRepeat(animation: Animation?) = Unit
        })

        target.startAnimation(fadeOut)
    }

    private fun applyPage(pageIndex: Int) {
        kehadiranAdapter.updateData(getPageData(fullKehadiran, kehadiranRowsPerPage, pageIndex))
        meetingAdapter.updateData(getPageData(fullMeeting, meetingRowsPerPage, pageIndex))
    }

    private fun <T> getPageData(source: List<T>, pageSize: Int, pageIndex: Int): List<T> {
        if (source.isEmpty() || pageSize <= 0) return emptyList()
        val pageCount = calculatePageCount(source.size, pageSize)
        if (pageCount <= 0) return emptyList()

        val safePageIndex = pageIndex % pageCount
        val start = safePageIndex * pageSize
        if (start >= source.size) return emptyList()
        val end = minOf(start + pageSize, source.size)
        return source.subList(start, end)
    }

    private fun calculatePageCount(itemCount: Int, pageSize: Int): Int {
        if (itemCount <= 0 || pageSize <= 0) return 0
        return (itemCount + pageSize - 1) / pageSize
    }

    private fun setupScrollFinishedWatcher(manager: AutoScrollManager?) {
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

    private fun calculateDwellDelayForPage(pageIndex: Int): Long {
        val kehadiranPage = getPageData(fullKehadiran, kehadiranRowsPerPage, pageIndex)
        val meetingPage = getPageData(fullMeeting, meetingRowsPerPage, pageIndex)

        val measuredDelay = estimateMarqueeWaitFromData(kehadiranPage, meetingPage)
        if (measuredDelay != -1L) {
            return (measuredDelay + 3_700L).coerceIn(6_000L, 60_000L)
        }

        val kehadiranMax = kehadiranPage.maxOfOrNull { maxOf(it.nama.length, it.keterangan?.length ?: 0) } ?: 0
        val meetingMax = meetingPage.maxOfOrNull { it.judul?.length ?: 0 } ?: 0

        val fallback = 6_000L + (maxOf(kehadiranMax, meetingMax) * 100L)
        return fallback.coerceIn(6_000L, 60_000L)
    }

    private fun estimateMarqueeWaitFromData(kehadiranData: List<Kehadiran>, meetingData: List<Meeting>): Long {
        if (_binding == null) return -1L

        var maxDurationMs = 0L

        val tvNamaK: TextView
        val tvKetK: TextView
        var availableNama = 0
        var availableStatus = 0
        var availableKet = 0

        if (binding.rvKehadiran.childCount > 0) {
            val sampleChildK = binding.rvKehadiran.getChildAt(0)
            tvNamaK = sampleChildK.findViewById(R.id.tvNamaStaff) ?: return -1L
            val tvStatusK: TextView = sampleChildK.findViewById(R.id.tvStatus) ?: return -1L
            tvKetK = sampleChildK.findViewById(R.id.tvKeterangan) ?: return -1L
            
            availableNama = (tvNamaK.width - tvNamaK.paddingLeft - tvNamaK.paddingRight).coerceAtLeast(0)
            availableStatus = (tvStatusK.width - tvStatusK.paddingLeft - tvStatusK.paddingRight).coerceAtLeast(0)
            availableKet = (tvKetK.width - tvKetK.paddingLeft - tvKetK.paddingRight).coerceAtLeast(0)
        } else {
            val headerK = binding.linearLayoutHeaderKehadiran
            tvNamaK = headerK.getChildAt(0) as? TextView ?: return -1L
            tvKetK = headerK.getChildAt(2) as? TextView ?: return -1L
            val rvWidth = binding.rvKehadiran.width
            if (rvWidth > 0) {
                val marginPx = 40f * tvNamaK.context.resources.displayMetrics.density
                availableNama = ((rvWidth - marginPx) * (1.35f / 3.5f)).toInt().coerceAtLeast(0)
                availableStatus = ((rvWidth - marginPx) * (0.8f / 3.5f)).toInt().coerceAtLeast(0)
                availableKet = ((rvWidth - marginPx) * (1.35f / 3.5f)).toInt().coerceAtLeast(0)
            }
        }

        if (availableNama > 0 && availableStatus > 0 && availableKet > 0) {
            kehadiranAdapter.setAvailableWidths(availableNama, availableStatus, availableKet)
            kehadiranData.forEach { item ->
                val namaDur = estimateSingleDuration(item.nama, tvNamaK, availableNama)
                if (namaDur > maxDurationMs) maxDurationMs = namaDur

                val statusDur = estimateSingleDuration(item.status, tvNamaK, availableStatus) // Use Nama textview for paint properties
                if (statusDur > maxDurationMs) maxDurationMs = statusDur

                val ketDur = estimateSingleDuration(item.keterangan ?: "", tvKetK, availableKet)
                if (ketDur > maxDurationMs) maxDurationMs = ketDur
            }
        }

        val tvJudulM: TextView
        var availableJudul = 0

        if (binding.rvMeeting.childCount > 0) {
            val sampleChildM = binding.rvMeeting.getChildAt(0)
            tvJudulM = sampleChildM.findViewById(R.id.tvJudulMeeting) ?: return -1L
            availableJudul = (tvJudulM.width - tvJudulM.paddingLeft - tvJudulM.paddingRight).coerceAtLeast(0)
        } else {
            val headerM = binding.linearLayoutHeaderMeeting
            tvJudulM = headerM.getChildAt(1) as? TextView ?: return -1L
            val rvWidth = binding.rvMeeting.width
            if (rvWidth > 0) {
                val marginPx = 40f * tvJudulM.context.resources.displayMetrics.density
                availableJudul = ((rvWidth - marginPx) * (1.5f / 2.5f)).toInt().coerceAtLeast(0)
            }
        }

        if (availableJudul > 0) {
            meetingAdapter.setAvailableWidth(availableJudul)
            meetingData.forEach { item ->
                val judulDur = estimateSingleDuration(item.judul ?: "", tvJudulM, availableJudul)
                if (judulDur > maxDurationMs) maxDurationMs = judulDur
            }
        }

        if ((binding.rvKehadiran.childCount == 0 && availableNama <= 0) || 
            (binding.rvMeeting.childCount == 0 && availableJudul <= 0)) {
            return -1L
        }

        return maxDurationMs
    }

    private fun estimateSingleDuration(content: String, textView: TextView, availableWidth: Int): Long {
        if (content.isBlank() || availableWidth <= 0) return 0L

        // NEW LOGIC: If it fits in 2 lines, it won't scroll.
        val fitsIn2 = com.karirjepang.dailymonitoringkj.ui.util.MarqueeUtils.fitsInLines(content, textView, availableWidth, 2)
        if (fitsIn2) return 0L

        val textWidth = textView.paint.measureText(content.replace("\n", " • ").replace("  ", " ").trim())
        if (textWidth <= availableWidth) return 0L

        val density = textView.context.resources.displayMetrics.density
        // Android native marquee speed is often slower than 30dp/s for long texts
        val speedPxPerSecond = 25f * density
        // Increase padding to ensure user can read the end of the text
        val distancePx = textWidth - availableWidth + 120f
        return ((distancePx / speedPxPerSecond) * 1000f).toLong().coerceAtLeast(0L)
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
        pagingManager?.startAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        pagingManager?.stopAutoScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pagingManager?.cleanup()
        _binding = null
    }
}
