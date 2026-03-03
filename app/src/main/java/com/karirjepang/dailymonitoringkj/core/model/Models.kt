package com.karirjepang.dailymonitoringkj.core.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: User
)

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String
)

data class Kehadiran(
    @SerializedName("status") val status: String,
    @SerializedName("note") val keterangan: String?,
    @SerializedName("user") val userObj: UserAttendance?
) {
    val nama: String get() = userObj?.name ?: "Tanpa Nama"
}

data class UserAttendance(
    @SerializedName("name") val name: String
)

data class Meeting(
    @SerializedName("waktu") val waktu: String?,
    @SerializedName("judul") val judul: String?
)

data class ProgressDivisi(
    @SerializedName("job_description") val projectProgress: String?,
    @SerializedName("progress_percentage") val persentaseInt: Int?,
    @SerializedName("division") val divisionObj: Division?
) {
    val namaDivisi: String get() = divisionObj?.name ?: "-"
    val persentase: String get() = "${persentaseInt ?: 0}%"
}

data class Division(
    @SerializedName("name") val name: String?
)

data class PmiDepartureResponse(
    @SerializedName("date") val date: String?,
    @SerializedName("total") val total: String?,
    @SerializedName("visa") val visa: VisaObj?
)

data class VisaObj(
    @SerializedName("name") val name: String?
)

data class KeberangkatanPMI(
    val tahun: Float,
    val tokuteiGinou: Float,
    val gijinkoku: Float
)

data class Mitra(
    @SerializedName("name") val nama: String?,
    @SerializedName("logo") val logoPath: String?
) {
    val logoUrl: String get() = if (logoPath?.startsWith("http") == true) logoPath else "https://dailymonitoringkj.id/storage/$logoPath"
}