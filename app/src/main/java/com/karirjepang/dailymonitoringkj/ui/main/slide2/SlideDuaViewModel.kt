package com.karirjepang.dailymonitoringkj.ui.main.slide2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.model.ProgressDivisi
import com.karirjepang.dailymonitoringkj.core.repository.MonitoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SlideDuaViewModel @Inject constructor(
    private val repository: MonitoringRepository
) : ViewModel() {

    private val _progressList = MutableLiveData<List<ProgressDivisi>>()
    val progressList: LiveData<List<ProgressDivisi>> get() = _progressList

    private val _progressLeftList = MutableLiveData<List<ProgressDivisi>>()
    val progressLeftList: LiveData<List<ProgressDivisi>> get() = _progressLeftList

    private val _progressRightList = MutableLiveData<List<ProgressDivisi>>()
    val progressRightList: LiveData<List<ProgressDivisi>> get() = _progressRightList

    private val _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String> get() = _currentDate

    private val _currentTime = MutableLiveData<String>()
    val currentTime: LiveData<String> get() = _currentTime

    init {
        loadData()
        startClock()
    }

    private fun loadData() {
        viewModelScope.launch {
            val fullList = repository.getProgressDivisi()
            _progressList.value = fullList

            // Split data into left and right
            val mid = (fullList.size + 1) / 2
            val leftList = fullList.take(mid)
            val rightList = fullList.drop(mid)

            _progressLeftList.value = leftList
            _progressRightList.value = rightList
        }
    }

    private fun startClock() {
        val dateFormatter = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("id", "ID"))
        val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))

        viewModelScope.launch {
            while (true) {
                val timeNow = Date()
                _currentDate.value = dateFormatter.format(timeNow).uppercase(Locale("id", "ID"))
                _currentTime.value = timeFormatter.format(timeNow)
                delay(1000)
            }
        }
    }
}