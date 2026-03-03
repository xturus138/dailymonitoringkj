package com.karirjepang.dailymonitoringkj.ui.main.slide1

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.model.Kehadiran
import com.karirjepang.dailymonitoringkj.core.model.Meeting
import com.karirjepang.dailymonitoringkj.core.repository.MonitoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SlideSatuViewModel @Inject constructor(
    private val repository: MonitoringRepository
) : ViewModel() {

    private val _kehadiranList = MutableLiveData<List<Kehadiran>>()
    val kehadiranList: LiveData<List<Kehadiran>> get() = _kehadiranList

    private val _meetingList = MutableLiveData<List<Meeting>>()
    val meetingList: LiveData<List<Meeting>> get() = _meetingList

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
            _kehadiranList.value = repository.getKehadiran()
            _meetingList.value = repository.getMeeting()
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