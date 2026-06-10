package com.example.digitaldextox

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("DetoxPrefs", Context.MODE_PRIVATE)

    fun saveTargetUnlockTime(timeInMillis: Long) {
        prefs.edit().putLong("TARGET_UNLOCK_TIME", timeInMillis).apply()
    }

    fun getTargetUnlockTime(): Long {
        return prefs.getLong("TARGET_UNLOCK_TIME", 0L)
    }

    fun clearTargetUnlockTime() {
        prefs.edit().remove("TARGET_UNLOCK_TIME").apply()
    }

    fun saveSchedule(startH: Int, startM: Int, endH: Int, endM: Int) {
        prefs.edit().apply {
            putInt("START_H", startH)
            putInt("START_M", startM)
            putInt("END_H", endH)
            putInt("END_M", endM)
            putBoolean("HAS_SCHEDULE", true)
        }.apply()
    }

    fun getScheduleStart(): Pair<Int, Int> {
        return Pair(prefs.getInt("START_H", 22), prefs.getInt("START_M", 0))
    }

    fun getScheduleEnd(): Pair<Int, Int> {
        return Pair(prefs.getInt("END_H", 6), prefs.getInt("END_M", 0))
    }

    fun hasSchedule(): Boolean {
        return prefs.getBoolean("HAS_SCHEDULE", false)
    }

    // Chỉ xóa trạng thái lịch, giữ lại giờ đã cài đặt
    fun clearScheduleStatus() {
        prefs.edit().apply {
            putBoolean("HAS_SCHEDULE", false)
        }.commit()
    }

    fun saveHashLength(length: Int) {
        // ĐỔI .apply() THÀNH .commit() ĐỂ ÉP LƯU TỨC THÌ
        prefs.edit().putInt("HASH_LENGTH", length).commit()
    }

    fun getHashLength(): Int {
        return prefs.getInt("HASH_LENGTH", 50) // Mặc định là 50 ký tự
    }
}