package com.karirjepang.dailymonitoringkj.ui.main.slide2

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
import com.karirjepang.dailymonitoringkj.core.model.ProgressDivisi
import com.karirjepang.dailymonitoringkj.databinding.FragmentSlideDuaBinding
import com.karirjepang.dailymonitoringkj.ui.adapter.ProgressDualAdapter
import com.karirjepang.dailymonitoringkj.ui.util.AutoScrollManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class SlideDua : Fragment() {

    private val viewModel: SlideDuaViewModel by viewModels()

    private lateinit var progressAdapter: ProgressDualAdapter

    private var _binding: FragmentSlideDuaBinding? = null
    private val binding get() = _binding!!

    private var pagingManager: AutoScrollManager? = null

    private var fullProgress: List<ProgressDivisi> = emptyList()
    private var rowsPerPage: Int = 0
    private var hasInitialPageRendered = false

    private var scrollFinishedDeferred: CompletableDeferred<Unit> = CompletableDeferred()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideDuaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressAdapter = ProgressDualAdapter(emptyList())
        binding.rvProgress.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProgress.adapter = progressAdapter
        binding.rvProgress.setHasFixedSize(true)
        binding.rvProgress.itemAnimator = null
        binding.rvProgress.setItemViewCacheSize(20)

        // Keep list hidden until page 0 is ready to avoid jump/flicker.
        binding.rvProgress.alpha = 0f

        binding.rvProgress.post { updateVisibleCount() }

        observeData()
    }

    private fun updateVisibleCount() {
        if (_binding == null) return

        val rowHeightPx = resources.getDimensionPixelSize(R.dimen.signage_row_height)
        val marginPx = resources.getDimensionPixelSize(R.dimen.dimen_margin_6)
        val itemHeightPx = rowHeightPx + marginPx

        if (itemHeightPx <= 0) return

        val rvHeight = binding.rvProgress.height
        if (rvHeight > 0) {
            val newRowsPerPage = (rvHeight / itemHeightPx).coerceAtLeast(1)
            if (rowsPerPage != newRowsPerPage) {
                rowsPerPage = newRowsPerPage
                progressAdapter.setVisibleItemCount(rowsPerPage)
                rebuildPagingManagerIfReady()
            } else if (pagingManager == null) {
                rebuildPagingManagerIfReady()
            }
        }
    }

    private fun observeData() {
        viewModel.progressList.observe(viewLifecycleOwner) { data ->
            fullProgress = data
            hasInitialPageRendered = false
            binding.rvProgress.alpha = 0f
            rebuildPagingManagerIfReady()
        }
    }

    private fun rebuildPagingManagerIfReady() {
        if (_binding == null) return
        if (rowsPerPage <= 0) return

        pagingManager?.stopAutoScroll()
        pagingManager = null

        val totalPages = calculatePageCount(fullProgress.size, rowsPerPage * 2)

        if (totalPages <= 0) {
            applyPage(0)
            binding.rvProgress.alpha = 1f
            setupScrollFinishedWatcher(null)
            return
        }

        pagingManager = AutoScrollManager(
            delayMillis = 15_000L,
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
                if (!hasInitialPageRendered && _binding != null) {
                    hasInitialPageRendered = true
                    binding.rvProgress.alpha = 1f
                }
            }

            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                manager.startAutoScroll()
            }
        }

        setupScrollFinishedWatcher(pagingManager)
    }

    private fun calculateDwellDelayForPage(pageIndex: Int): Long {
        val pageSize = rowsPerPage * 2
        val pageData = getPageData(fullProgress, pageSize, pageIndex)

        // Use real rendered widths but measuring actual text for the UPCOMING page.
        val measuredDelay = estimateMarqueeWaitFromData(pageData)
        if (measuredDelay != -1L) {
            // Buffer: 2000ms adapter delay + ~2000ms Android native start delay + 1000ms safe margin
            return (measuredDelay + 5_000L).coerceIn(6_000L, 120_000L)
        }

        // Fallback before first layout/frame.
        val maxChars = pageData.maxOfOrNull { item ->
            maxOf(item.projectProgress?.length ?: 0, item.namaDivisi.length, item.persentase.length)
        } ?: 0
        val fallback = 6_000L + (maxChars * 100L)
        return fallback.coerceIn(6_000L, 60_000L)
    }

    private fun estimateMarqueeWaitFromData(pageData: List<ProgressDivisi>): Long {
        if (_binding == null) return -1L

        val tvDivisiLeft: TextView
        val tvProjectLeft: TextView
        var availableWidthDivisi = 0
        var availableWidthProject = 0

        if (binding.rvProgress.childCount > 0) {
            val sampleChild = binding.rvProgress.getChildAt(0)
            tvDivisiLeft = sampleChild.findViewById(R.id.tvNamaDivisiLeft) ?: return -1L
            tvProjectLeft = sampleChild.findViewById(R.id.tvProjectLeft) ?: return -1L

            availableWidthDivisi = (tvDivisiLeft.width - tvDivisiLeft.paddingLeft - tvDivisiLeft.paddingRight).coerceAtLeast(0)
            availableWidthProject = (tvProjectLeft.width - tvProjectLeft.paddingLeft - tvProjectLeft.paddingRight).coerceAtLeast(0)
        } else {
            val rvWidth = binding.rvProgress.width
            val leftHeader = binding.headerRow.getChildAt(0) as? android.view.ViewGroup ?: return -1L
            tvDivisiLeft = leftHeader.getChildAt(0) as? TextView ?: return -1L
            tvProjectLeft = leftHeader.getChildAt(1) as? TextView ?: return -1L

            if (rvWidth > 0) {
                val columnW = rvWidth / 2f
                val density = tvProjectLeft.context.resources.displayMetrics.density
                val marginPx = 40f * density

                availableWidthDivisi = ((columnW - marginPx) * (1.0f / 3.5f)).toInt().coerceAtLeast(0)
                availableWidthProject = ((columnW - marginPx) * (1.5f / 3.5f)).toInt().coerceAtLeast(0)
            } else {
                return -1L
            }
        }

        if (availableWidthDivisi <= 0 || availableWidthProject <= 0) return -1L

        var maxDurationMs = 0L

        pageData.forEach { item ->
            val divDuration = estimateSingleDuration(item.namaDivisi, tvDivisiLeft, availableWidthDivisi)
            if (divDuration > maxDurationMs) maxDurationMs = divDuration

            val progText = item.projectProgress?.replace("\n", " • ")?.replace("  ", " ")?.trim() ?: ""
            val progDuration = estimateSingleDuration(progText, tvProjectLeft, availableWidthProject)
            if (progDuration > maxDurationMs) maxDurationMs = progDuration
        }

        return maxDurationMs
    }

    private fun estimateSingleDuration(content: String, textView: TextView, availableWidth: Int): Long {
        if (content.isBlank() || availableWidth <= 0) return 0L
        
        val textWidth = textView.paint.measureText(content)
        if (textWidth <= availableWidth) return 0L

        val density = textView.context.resources.displayMetrics.density
        // Android native marquee speed is exactly 30 * screen density
        // Android native marquee speed is often slower than 30dp/s for long texts
        val speedPxPerSecond = 25f * density
        // Increase padding to ensure user can read the end of the text
        val distancePx = textWidth - availableWidth + 120f
        return ((distancePx / speedPxPerSecond) * 1000f).toLong().coerceAtLeast(0L)
    }

    private fun runFadeTransition(nextPage: Int, onFinished: () -> Unit) {
        if (_binding == null) {
            onFinished()
            return
        }

        // Do not animate before the first page has ever been shown.
        if (!hasInitialPageRendered) {
            applyPage(nextPage)
            hasInitialPageRendered = true
            binding.rvProgress.alpha = 1f
            onFinished()
            return
        }

        val target = binding.rvProgress
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
        val pageSize = rowsPerPage * 2
        progressAdapter.updateData(getPageData(fullProgress, pageSize, pageIndex))
        progressAdapter.setVisibleItemCount(rowsPerPage)
    }

    private fun getPageData(source: List<ProgressDivisi>, pageSize: Int, pageIndex: Int): List<ProgressDivisi> {
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
