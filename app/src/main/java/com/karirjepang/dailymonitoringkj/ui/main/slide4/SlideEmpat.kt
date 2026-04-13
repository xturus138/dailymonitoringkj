package com.karirjepang.dailymonitoringkj.ui.main.slide4

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.karirjepang.dailymonitoringkj.R
import com.karirjepang.dailymonitoringkj.core.model.Mitra
import com.karirjepang.dailymonitoringkj.databinding.FragmentSlideEmpatBinding
import com.karirjepang.dailymonitoringkj.ui.adapter.MitraAdapter
import com.karirjepang.dailymonitoringkj.ui.util.AutoScrollManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred

@AndroidEntryPoint
class SlideEmpat : Fragment() {

    private val viewModel: SlideEmpatViewModel by viewModels()
    private lateinit var mitraAdapter: MitraAdapter

    private var _binding: FragmentSlideEmpatBinding? = null
    private val binding get() = _binding!!

    private val spanCount = 5

    private var pagingManager: AutoScrollManager? = null
    private var scrollFinishedDeferred: CompletableDeferred<Unit> = CompletableDeferred()

    private var fullMitra: List<Mitra> = emptyList()
    private var rowsPerPage: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideEmpatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mitraAdapter = MitraAdapter(emptyList(), spanCount)
        binding.rvMitra.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMitra.adapter = mitraAdapter

        observeData()
    }

    private fun observeData() {
        viewModel.mitraList.observe(viewLifecycleOwner) { data ->
            fullMitra = data

            // Render once so RecyclerView can report visible row count accurately.
            mitraAdapter.updateData(fullMitra)
            binding.rvMitra.post {
                if (_binding == null) return@post

                rowsPerPage = detectVisibleRows().coerceAtLeast(1)
                rebuildPagingManager()
            }
        }
    }

    private fun detectVisibleRows(): Int {
        val lm = binding.rvMitra.layoutManager as? LinearLayoutManager ?: return 1
        val first = lm.findFirstVisibleItemPosition()
        val last = lm.findLastVisibleItemPosition()
        if (first == -1 || last == -1) return 1
        return (last - first + 1).coerceAtLeast(1)
    }

    private fun rebuildPagingManager() {
        if (_binding == null) return

        pagingManager?.stopAutoScroll()
        pagingManager = null

        val totalPages = mitraAdapter.getPageCountByRows(rowsPerPage, fullMitra)

        if (totalPages <= 0) {
            mitraAdapter.updateData(emptyList())
            setupScrollFinishedWatcher(null)
            return
        }

        pagingManager = AutoScrollManager(
            delayMillis = 10_000L,
            totalPages = totalPages,
            onPageTransition = { nextPage, onTransitionFinished ->
                runFadeTransition(nextPage, onTransitionFinished)
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

        val target = binding.rvMitra
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
        mitraAdapter.updateData(
            mitraAdapter.getPageDataByRows(
                source = fullMitra,
                rowPerPage = rowsPerPage,
                pageIndex = pageIndex
            )
        )
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