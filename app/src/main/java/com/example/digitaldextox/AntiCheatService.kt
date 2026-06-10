package com.example.digitaldextox

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class AntiCheatService : AccessibilityService() {

    private lateinit var sharedPrefsHelper: SharedPrefsHelper

    override fun onServiceConnected() {
        super.onServiceConnected()
        sharedPrefsHelper = SharedPrefsHelper(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val targetTime = sharedPrefsHelper.getTargetUnlockTime()
        val currentTime = System.currentTimeMillis()

        // Nếu app ĐANG TRONG TRẠNG THÁI KHÓA
        if (targetTime > currentTime) {
            val packageName = event.packageName?.toString() ?: return

            // 1. Quét thẳng tay: SystemUI (Thanh thông báo)
            if (packageName.contains("systemui", ignoreCase = true)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                }
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }

            // 2. Danh sách chặn cứng (Smart Blacklist): Cài đặt & Gỡ ứng dụng
            val blockedKeywords = listOf("setting", "installer", "packageinstaller")
            
            for (keyword in blockedKeywords) {
                if (packageName.contains(keyword, ignoreCase = true)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    Toast.makeText(this, "Hành vi lách luật đã bị chặn!", Toast.LENGTH_SHORT).show()
                    break
                }
            }
        }
    }

    override fun onInterrupt() {}
}