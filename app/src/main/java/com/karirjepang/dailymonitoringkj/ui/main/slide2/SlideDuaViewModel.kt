package com.karirjepang.dailymonitoringkj.ui.main.slide2

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.cache.CachedDataManager
import com.karirjepang.dailymonitoringkj.core.model.ProgressDivisi
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideDuaViewModel @Inject constructor(
    private val cachedDataManager: CachedDataManager,
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
        observeCachedData()
    }

    private fun observeCachedData() {
        viewModelScope.launch {
            cachedDataManager.progressDivisiList.collectLatest { fullList ->
                Log.d(TAG, "ProgressDivisi from cache: ${fullList.size} items")
                _progressList.value = fullList

                val mid = (fullList.size + 1) / 2
                _progressLeftList.value = fullList.take(mid)
                _progressRightList.value = fullList.drop(mid)
            }
        }
    }
}