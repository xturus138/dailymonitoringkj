package com.karirjepang.dailymonitoringkj.core.network

import com.karirjepang.dailymonitoringkj.core.model.TimeResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TimeApiService {
    @GET("api/Time/current/zone")
    suspend fun getCurrentTime(
        @Query("timeZone") timeZone: String = "Asia/Jakarta"
    ): Response<TimeResponse>
}

