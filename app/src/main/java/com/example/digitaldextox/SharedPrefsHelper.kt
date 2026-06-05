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
}