package com.karirjepang.dailymonitoringkj.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.model.LoginRequest
import com.karirjepang.dailymonitoringkj.core.repository.MonitoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MonitoringRepository
) : ViewModel() {

    private val _isLoginSuccessful = MutableLiveData<Boolean>()
    val isLoginSuccessful: LiveData<Boolean> get() = _isLoginSuccessful

    fun autoLoginBackground() {
        viewModelScope.launch {
            val request = LoginRequest("adminkj@gmail.com", "Kerjajepang12$")
            val result = repository.login(request)

            _isLoginSuccessful.value = result.isSuccess
        }
    }
}