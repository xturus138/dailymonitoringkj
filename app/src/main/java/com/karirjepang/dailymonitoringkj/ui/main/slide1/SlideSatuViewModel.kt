package com.karirjepang.dailymonitoringkj.ui.main.slide1

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.cache.CachedDataManager
import com.karirjepang.dailymonitoringkj.core.model.Kehadiran
import com.karirjepang.dailymonitoringkj.core.model.Meeting
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideSatuViewModel @Inject constructor(
    private val cachedDataManager: CachedDataManager,
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
        observeCachedData()
    }

    private fun observeCachedData() {
        viewModelScope.launch {
            cachedDataManager.kehadiranList.collectLatest { data ->
                Log.d(TAG, "Kehadiran from cache: ${data.size} items")
                _kehadiranList.value = data
            }
        }
        viewModelScope.launch {
            cachedDataManager.meetingList.collectLatest { data ->
                Log.d(TAG, "Meeting from cache: ${data.size} items")
                _meetingList.value = data
            }
        }
    }
}