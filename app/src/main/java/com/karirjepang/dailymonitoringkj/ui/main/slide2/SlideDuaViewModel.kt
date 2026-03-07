package com.karirjepang.dailymonitoringkj.ui.main.slide2

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.cache.SlideDataCache
import com.karirjepang.dailymonitoringkj.core.model.ProgressDivisi
import com.karirjepang.dailymonitoringkj.core.repository.MonitoringRepository
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideDuaViewModel @Inject constructor(
    private val repository: MonitoringRepository,
    private val slideDataCache: SlideDataCache,
    private val apiClock: ApiClock
) : ViewModel() {

    private val TAG = "SlideDuaViewModel"

    private val _progressList = MutableLiveData<List<ProgressDivisi>>()
    val progressList: LiveData<List<ProgressDivisi>> get() = _progressList

    private val _progressLeftList = MutableLiveData<List<ProgressDivisi>>()
    val progressLeftList: LiveData<List<ProgressDivisi>> get() = _progressLeftList

    private val _progressRightList = MutableLiveData<List<ProgressDivisi>>()
    val progressRightList: LiveData<List<ProgressDivisi>> get() = _progressRightList

    val currentDate: LiveData<String> get() = apiClock.currentDate
    val currentTime: LiveData<String> get() = apiClock.currentTime

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val cached = slideDataCache.consumeSlide2Data()
            val fullList = if (cached != null) {
                Log.d(TAG, "Using prefetched data from cache")
                cached
            } else {
                Log.d(TAG, "No cache available, fetching from API")
                repository.getProgressDivisi()
            }

            _progressList.value = fullList

            val mid = (fullList.size + 1) / 2
            val leftList = fullList.take(mid)
            val rightList = fullList.drop(mid)

            _progressLeftList.value = leftList
            _progressRightList.value = rightList
        }
    }
}