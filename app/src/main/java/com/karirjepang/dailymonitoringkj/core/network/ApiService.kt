package com.karirjepang.dailymonitoringkj.core.network

import com.karirjepang.dailymonitoringkj.core.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("attendances")
    suspend fun getAttendances(): Response<List<Kehadiran>>

    @GET("meetings-today")
    suspend fun getMeetings(): Response<List<Meeting>>

    @GET("division-reports")
    suspend fun getDivisionReports(): Response<List<ProgressDivisi>>

    @GET("pmi-departures")
    suspend fun getPmiDepartures(): Response<List<PmiDepartureResponse>>

    @GET("partners")
    suspend fun getPartners(): Response<List<Mitra>>
}