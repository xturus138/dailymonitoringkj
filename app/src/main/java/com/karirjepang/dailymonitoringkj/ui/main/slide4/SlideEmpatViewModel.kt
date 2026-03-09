package com.karirjepang.dailymonitoringkj.ui.main.slide4

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.cache.CachedDataManager
import com.karirjepang.dailymonitoringkj.core.model.Mitra
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideEmpatViewModel @Inject constructor(
    private val cachedDataManager: CachedDataManager,
    private val apiClock: ApiClock
) : ViewModel() {

    private val TAG = "SlideEmpatViewModel"

    private val _mitraList = MutableLiveData<List<Mitra>>()
    val mitraList: LiveData<List<Mitra>> get() = _mitraList

    val currentDate: LiveData<String> get() = apiClock.currentDate
    val currentTime: LiveData<String> get() = apiClock.currentTime

    init {
        observeCachedData()
    }

    private fun observeCachedData() {
        viewModelScope.launch {
            cachedDataManager.mitraList.collectLatest { data ->
                Log.d(TAG, "Mitra from cache: ${data.size} items")
                _mitraList.value = data
            }
        }
    }
}