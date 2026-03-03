package com.karirjepang.dailymonitoringkj.ui.main.slide3

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karirjepang.dailymonitoringkj.core.model.KeberangkatanPMI
import com.karirjepang.dailymonitoringkj.core.repository.MonitoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SlideTigaViewModel @Inject constructor(
    private val repository: MonitoringRepository
) : ViewModel() {

    private val _chartData = MutableLiveData<List<KeberangkatanPMI>>()
    val chartData: LiveData<List<KeberangkatanPMI>> get() = _chartData

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
            _chartData.value = repository.getKeberangkatanPMI()
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