package com.example.digitaldextox

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
            val packageName = event.packageName?.toString()?.lowercase() ?: return

            // 1. Quét thẳng tay: SystemUI (Thanh thông báo)
            if (packageName.contains("systemui")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                }
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }

            // 2. Danh sách chặn nâng cao: Cài đặt & Gỡ ứng dụng nói chung
            val blockedKeywords = listOf(
                "setting", "installer", "packageinstaller", 
                "securitycenter", "safecenter", "appmanager"
            )
            for (keyword in blockedKeywords) {
                if (packageName.contains(keyword)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    Toast.makeText(this, "Ứng dụng hệ thống đang bị khóa!", Toast.LENGTH_SHORT).show()
                    return // Đã chặn thì return để không làm các bước sau
                }
            }

            // 3. PHÒNG THỦ TUYỆT ĐỐI: Quét Text hiển thị trên màn hình
            // Bất kể nó nằm trong package nào, hễ có các nút nhạy cảm là đá văng.
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                if (containsBannedText(rootNode)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    Toast.makeText(this, "Nội dung nhạy cảm / Lách luật đã bị chặn!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun containsBannedText(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        // Danh sách các từ ngữ nguy hiểm mà màn hình gỡ/ẩn app sẽ có
        val bannedWords = listOf(
            "gỡ cài đặt", "buộc dừng", "uninstall", "force stop", 
            "chi tiết ứng dụng", "thông tin ứng dụng", "app info", "kết thúc ứng dụng"
        )
        
        for (word in bannedWords) {
            if (text.contains(word)) return true
        }

        // Quét đệ quy toàn bộ nút con (Children) trên màn hình
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (containsBannedText(child)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    override fun onInterrupt() {}
}