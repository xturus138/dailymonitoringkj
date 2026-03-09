package com.karirjepang.dailymonitoringkj.ui.main.slide3

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.cache.CachedDataManager
import com.karirjepang.dailymonitoringkj.core.model.KeberangkatanPMI
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideTigaViewModel @Inject constructor(
    private val cachedDataManager: CachedDataManager,
    private val apiClock: ApiClock
) : ViewModel() {

    private val TAG = "SlideTigaViewModel"

    private val _chartData = MutableLiveData<List<KeberangkatanPMI>>()
    val chartData: LiveData<List<KeberangkatanPMI>> get() = _chartData

    val currentDate: LiveData<String> get() = apiClock.currentDate
    val currentTime: LiveData<String> get() = apiClock.currentTime

    init {
        observeCachedData()
    }

    private fun observeCachedData() {
        viewModelScope.launch {
            cachedDataManager.keberangkatanPMIList.collectLatest { data ->
                Log.d(TAG, "KeberangkatanPMI from cache: ${data.size} items")
                _chartData.value = data
            }
        }
    }
}