package com.karirjepang.dailymonitoringkj.core.repository

import com.karirjepang.dailymonitoringkj.core.model.*
import com.karirjepang.dailymonitoringkj.core.network.ApiService
import com.karirjepang.dailymonitoringkj.core.network.TokenManager
import com.karirjepang.dailymonitoringkj.ui.util.ApiClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MonitoringRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val apiClock: ApiClock
) {

    suspend fun login(request: LoginRequest): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                tokenManager.saveToken(response.body()!!.token)
                Result.success(true)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getKehadiran(): List<Kehadiran> = withContext(Dispatchers.IO) {
        val response = apiService.getAttendances()
        if (response.isSuccessful) {
            val today = apiClock.getTodayDateString()
            (response.body() ?: emptyList()).filter {
                it.date?.take(10) == today
            }
        } else {
            throw Exception("Gagal mengambil data kehadiran") // Lempar error jika gagal
        }
    }

    suspend fun getMeeting(): List<Meeting> = withContext(Dispatchers.IO) {
        val response = apiService.getMeetings()
        if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            throw Exception("Gagal mengambil data meeting") // Lempar error jika gagal
        }
    }

    suspend fun getProgressDivisi(): List<ProgressDivisi> = withContext(Dispatchers.IO) {
        val response = apiService.getDivisionReports()
        if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            throw Exception("Gagal mengambil data progress divisi") // Lempar error jika gagal
        }
    }

    suspend fun getKeberangkatanPMI(): List<KeberangkatanPMI> = withContext(Dispatchers.IO) {
        val response = apiService.getPmiDepartures()
        if (response.isSuccessful) {
            val rawData = response.body() ?: emptyList()

            val groupedByYear = rawData.groupBy { it.date?.take(4) ?: "0" }

            groupedByYear.map { (yearStr, items) ->
                val year = yearStr.toFloatOrNull() ?: 0f
                var tokutei = 0f
                var gijinkoku = 0f

                items.forEach { item ->
                    val total = item.total?.toFloatOrNull() ?: 0f
                    if (item.visa?.name?.contains("Tokutei", ignoreCase = true) == true) {
                        tokutei += total
                    } else if (item.visa?.name?.contains("Gijinkoku", ignoreCase = true) == true) {
                        gijinkoku += total
                    }
                }
                KeberangkatanPMI(year, tokutei, gijinkoku)
            }.filter { it.tahun > 0f }.sortedBy { it.tahun }.takeLast(4)
        } else {
            throw Exception("Gagal mengambil data keberangkatan PMI") // Lempar error jika gagal
        }
    }

    suspend fun getDaftarMitra(): List<Mitra> = withContext(Dispatchers.IO) {
        val response = apiService.getPartners()
        if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            throw Exception("Gagal mengambil data mitra") // Lempar error jika gagal
        }
    }
}