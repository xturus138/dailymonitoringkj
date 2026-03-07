package com.karirjepang.dailymonitoringkj.core.cache

import android.util.Log
import com.karirjepang.dailymonitoringkj.core.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton cache that stores prefetched data for upcoming slides.
 * Data is stored temporarily and consumed (removed) when the slide reads it,
 * ensuring fresh data is always prefetched for the next cycle.
 */
@Singleton
class SlideDataCache @Inject constructor() {

    private val TAG = "SlideDataCache"

    // Slide 1 data
    @Volatile var kehadiranList: List<Kehadiran>? = null
        private set
    @Volatile var meetingList: List<Meeting>? = null
        private set

    // Slide 2 data
    @Volatile var progressDivisiList: List<ProgressDivisi>? = null
        private set

    // Slide 3 data
    @Volatile var keberangkatanPMIList: List<KeberangkatanPMI>? = null
        private set

    // Slide 4 data
    @Volatile var mitraList: List<Mitra>? = null
        private set

    // --- Store methods (called by prefetcher) ---

    fun storeSlide1Data(kehadiran: List<Kehadiran>, meeting: List<Meeting>) {
        Log.d(TAG, "Stored Slide1 data: kehadiran=${kehadiran.size}, meeting=${meeting.size}")
        kehadiranList = kehadiran
        meetingList = meeting
    }

    fun storeSlide2Data(progressDivisi: List<ProgressDivisi>) {
        Log.d(TAG, "Stored Slide2 data: progressDivisi=${progressDivisi.size}")
        progressDivisiList = progressDivisi
    }

    fun storeSlide3Data(keberangkatanPMI: List<KeberangkatanPMI>) {
        Log.d(TAG, "Stored Slide3 data: keberangkatanPMI=${keberangkatanPMI.size}")
        keberangkatanPMIList = keberangkatanPMI
    }

    fun storeSlide4Data(mitra: List<Mitra>) {
        Log.d(TAG, "Stored Slide4 data: mitra=${mitra.size}")
        mitraList = mitra
    }

    // --- Consume methods (called by slide ViewModels) ---
    // Returns cached data and clears it so next cycle will prefetch fresh data.

    fun consumeSlide1Data(): Pair<List<Kehadiran>, List<Meeting>>? {
        val kehadiran = kehadiranList
        val meeting = meetingList
        if (kehadiran != null && meeting != null) {
            kehadiranList = null
            meetingList = null
            Log.d(TAG, "Consumed Slide1 data from cache")
            return Pair(kehadiran, meeting)
        }
        Log.d(TAG, "No cached Slide1 data available")
        return null
    }

    fun consumeSlide2Data(): List<ProgressDivisi>? {
        val data = progressDivisiList
        if (data != null) {
            progressDivisiList = null
            Log.d(TAG, "Consumed Slide2 data from cache")
            return data
        }
        Log.d(TAG, "No cached Slide2 data available")
        return null
    }

    fun consumeSlide3Data(): List<KeberangkatanPMI>? {
        val data = keberangkatanPMIList
        if (data != null) {
            keberangkatanPMIList = null
            Log.d(TAG, "Consumed Slide3 data from cache")
            return data
        }
        Log.d(TAG, "No cached Slide3 data available")
        return null
    }

    fun consumeSlide4Data(): List<Mitra>? {
        val data = mitraList
        if (data != null) {
            mitraList = null
            Log.d(TAG, "Consumed Slide4 data from cache")
            return data
        }
        Log.d(TAG, "No cached Slide4 data available")
        return null
    }
}


