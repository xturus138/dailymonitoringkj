package com.karirjepang.dailymonitoringkj.core.repository

import com.karirjepang.dailymonitoringkj.core.model.KeberangkatanPMI
import com.karirjepang.dailymonitoringkj.core.model.Kehadiran
import com.karirjepang.dailymonitoringkj.core.model.Meeting
import com.karirjepang.dailymonitoringkj.core.model.ProgressDivisi
import javax.inject.Inject

class MonitoringRepository @Inject constructor() {

    fun getKehadiran(): List<Kehadiran> {
        return listOf(
            Kehadiran("Andika", "Hadir", "-"),
            Kehadiran("Bagas", "Hadir", "-"),
            Kehadiran("Johnson", "Cuti", "-"),
            Kehadiran("Angel", "Hadir", "Perjalanan Dinas")
        )
    }

    fun getMeeting(): List<Meeting> {
        return listOf(
            Meeting("11:00 WIB", "Pemaparan Program"),
            Meeting("13:00 WIB", "Client PT. BAP Persada"),
            Meeting("13:15 WIB", "Kunjungan Kementrian")
        )
    }

    fun getProgressDivisi(): List<ProgressDivisi> {
        return listOf(
            ProgressDivisi("IT Development", "Sistem Inkubator KUMKM", "75%"),
            ProgressDivisi("Kemitraan", "Kerjasama Perusahaan Jepang", "90%"),
            ProgressDivisi("Pelatihan", "Program Peserta LPK", "60%"),
            ProgressDivisi("UI/UX Design", "Dashboard Tenant Startup", "85%")
        )
    }

    fun getKeberangkatanPMI(): List<KeberangkatanPMI> {
        return listOf(
            KeberangkatanPMI(2022f, 250f, 10f),
            KeberangkatanPMI(2023f, 315f, 9f),
            KeberangkatanPMI(2024f, 285f, 11f),
            KeberangkatanPMI(2025f, 415f, 8f),
            KeberangkatanPMI(2026f, 10f, 2f)
        )
    }
}