package com.karirjepang.dailymonitoringkj.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.BuildConfig
import com.karirjepang.dailymonitoringkj.core.cache.CachedDataManager
import com.karirjepang.dailymonitoringkj.core.model.LoginRequest
import com.karirjepang.dailymonitoringkj.core.network.NetworkMonitor
import com.karirjepang.dailymonitoringkj.core.repository.MonitoringRepository
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MonitoringRepository,
    val cachedDataManager: CachedDataManager,
    val networkMonitor: NetworkMonitor,
    val apiClock: ApiClock
) : ViewModel() {

    private val TAG = "MainViewModel"

    private val _isLoginSuccessful = MutableLiveData<Boolean>()
    val isLoginSuccessful: LiveData<Boolean> get() = _isLoginSuccessful

    @Volatile
    private var isLoggingIn = false

    fun autoLoginBackground() {
        if (isLoggingIn) {
            Log.d(TAG, "Login already in progress — skipping")
            return
        }
        isLoggingIn = true
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting login...")
                val request = LoginRequest(BuildConfig.API_EMAIL, BuildConfig.API_PASSWORD)
                val result = repository.login(request)
                _isLoginSuccessful.value = result.isSuccess
                Log.d(TAG, "Login result: ${result.isSuccess}")
            } finally {
                isLoggingIn = false
            }
        }
    }

    /**
     * Starts the centralized 5-minute auto-refresh cycle.
     * Called once after successful login. Safe to call multiple times.
     */
    fun startCacheRefresh() {
        Log.d(TAG, "Starting centralized cache refresh cycle")
        cachedDataManager.startAutoRefresh(viewModelScope)
    }
}