package com.karirjepang.dailymonitoringkj.ui.main.slide3

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.cache.SlideDataCache
import com.karirjepang.dailymonitoringkj.core.model.KeberangkatanPMI
import com.karirjepang.dailymonitoringkj.core.repository.MonitoringRepository
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideTigaViewModel @Inject constructor(
    private val repository: MonitoringRepository,
    private val slideDataCache: SlideDataCache,
    private val apiClock: ApiClock
) : ViewModel() {

    private val TAG = "SlideTigaViewModel"

    private val _chartData = MutableLiveData<List<KeberangkatanPMI>>()
    val chartData: LiveData<List<KeberangkatanPMI>> get() = _chartData

    val currentDate: LiveData<String> get() = apiClock.currentDate
    val currentTime: LiveData<String> get() = apiClock.currentTime

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val cached = slideDataCache.consumeSlide3Data()
            if (cached != null) {
                Log.d(TAG, "Using prefetched data from cache")
                _chartData.value = cached
            } else {
                Log.d(TAG, "No cache available, fetching from API")
                _chartData.value = repository.getKeberangkatanPMI()
            }
        }
    }
}