package com.example.digitaldextox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPrefsHelper = SharedPrefsHelper(context)

        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // EC4: Khôi phục lại khóa nếu máy vừa khởi động lại (Sẽ chạy nếu có quyền RECEIVE_BOOT_COMPLETED)
            if (sharedPrefsHelper.hasSchedule()) {
                // Ta có thể gọi lại một Activity ẩn hoặc start service logic tương tự để setup Alarm, 
                // ở phiên bản này, chỉ cần cài cắm sườn chức năng.
            }
            return
        }

        // Đến giờ khóa
        if (!sharedPrefsHelper.hasSchedule()) return

        val end = sharedPrefsHelper.getScheduleEnd()
        val start = sharedPrefsHelper.getScheduleStart()
        
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, end.first)
            set(Calendar.MINUTE, end.second)
            set(Calendar.SECOND, 0)
        }
        
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, start.first)
            set(Calendar.MINUTE, start.second)
            set(Calendar.SECOND, 0)
        }

        // EC1: Xét xử lý qua đêm (Cross Midnight)
        if (endCal.before(startCal) || endCal == startCal) {
            endCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Cập nhật targetUnlockTime vào Prefs
        sharedPrefsHelper.saveTargetUnlockTime(endCal.timeInMillis)

        // Bật màn hình khóa
        val serviceIntent = Intent(context, LockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
    }