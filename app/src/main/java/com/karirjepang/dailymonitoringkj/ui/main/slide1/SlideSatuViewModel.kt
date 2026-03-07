package com.karirjepang.dailymonitoringkj.ui.main.slide1

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.cache.SlideDataCache
import com.karirjepang.dailymonitoringkj.core.model.Kehadiran
import com.karirjepang.dailymonitoringkj.core.model.Meeting
import com.karirjepang.dailymonitoringkj.core.repository.MonitoringRepository
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideSatuViewModel @Inject constructor(
    private val repository: MonitoringRepository,
    private val slideDataCache: SlideDataCache,
    private val apiClock: ApiClock
) : ViewModel() {

    private val TAG = "SlideSatuViewModel"

    private val _kehadiranList = MutableLiveData<List<Kehadiran>>()
    val kehadiranList: LiveData<List<Kehadiran>> get() = _kehadiranList

    private val _meetingList = MutableLiveData<List<Meeting>>()
    val meetingList: LiveData<List<Meeting>> get() = _meetingList

    val currentDate: LiveData<String> get() = apiClock.currentDate
    val currentTime: LiveData<String> get() = apiClock.currentTime

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val cached = slideDataCache.consumeSlide1Data()
            if (cached != null) {
                Log.d(TAG, "Using prefetched data from cache")
                _kehadiranList.value = cached.first
                _meetingList.value = cached.second
            } else {
                Log.d(TAG, "No cache available, fetching from API")
                _kehadiranList.value = repository.getKehadiran()
                _meetingList.value = repository.getMeeting()
            }
        }
    }
}