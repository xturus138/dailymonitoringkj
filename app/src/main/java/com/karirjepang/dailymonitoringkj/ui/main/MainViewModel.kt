package com.karirjepang.dailymonitoringkj.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.cache.SlideDataCache
import com.karirjepang.dailymonitoringkj.core.model.LoginRequest
import com.karirjepang.dailymonitoringkj.core.repository.MonitoringRepository
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MonitoringRepository,
    val slideDataCache: SlideDataCache,
    val apiClock: ApiClock
) : ViewModel() {

    private val TAG = "MainViewModel"

    private val _isLoginSuccessful = MutableLiveData<Boolean>()
    val isLoginSuccessful: LiveData<Boolean> get() = _isLoginSuccessful

    fun autoLoginBackground() {
        viewModelScope.launch {
            val request = LoginRequest("adminkj@gmail.com", "Kerjajepang12$")
            val result = repository.login(request)

            _isLoginSuccessful.value = result.isSuccess
        }
    }

    /**
     * Prefetch data for the next slide while the current slide is being displayed.
     * Called from MainActivity during each slide's display time.
     *
     * @param nextSlideIndex The index of the next slide (0-3)
     */
    fun prefetchNextSlideData(nextSlideIndex: Int) {
        viewModelScope.launch {
            Log.d(TAG, "Prefetching data for slide $nextSlideIndex")
            when (nextSlideIndex) {
                0 -> {
                    val kehadiran = repository.getKehadiran()
                    val meeting = repository.getMeeting()
                    slideDataCache.storeSlide1Data(kehadiran, meeting)
                }
                1 -> {
                    val progressDivisi = repository.getProgressDivisi()
                    slideDataCache.storeSlide2Data(progressDivisi)
                }
                2 -> {
                    val keberangkatanPMI = repository.getKeberangkatanPMI()
                    slideDataCache.storeSlide3Data(keberangkatanPMI)
                }
                3 -> {
                    val mitra = repository.getDaftarMitra()
                    slideDataCache.storeSlide4Data(mitra)
                }
            }
            Log.d(TAG, "Prefetch complete for slide $nextSlideIndex")
        }
    }
}