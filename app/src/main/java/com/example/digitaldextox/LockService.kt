package com.example.digitaldextox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class LockService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var countDownTimer: CountDownTimer? = null
    private lateinit var tvCountdown: TextView
    private lateinit var sharedPrefsHelper: SharedPrefsHelper
    private val CHANNEL_ID = "LockServiceChannel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sharedPrefsHelper = SharedPrefsHelper(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        createNotificationChannel()
        startForeground(1, createNotification())
        
        showOverlay()
        startCountdown()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Digital Detox Lock",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Digital Detox")
            .setContentText("Kỷ luật thép - Điện thoại đang bị khóa")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun showOverlay() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            // Không set FLAG_NOT_FOCUSABLE để overlay nuốt trọn sự kiện phím Back
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.layout_lock_overlay, null)
        
        tvCountdown = overlayView!!.findViewById(R.id.tvCountdown)
        windowManager.addView(overlayView, layoutParams)
    }

    private fun startCountdown() {
        val targetTime = sharedPrefsHelper.getTargetUnlockTime()
        val currentTime = System.currentTimeMillis()
        val timeLeft = targetTime - currentTime

        if (timeLeft <= 0) {
            unlockAndStop()
            return
        }

        countDownTimer = object : CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val h = (millisUntilFinished / 3600000).toInt()
                val m = ((millisUntilFinished % 3600000) / 60000).toInt()
                val s = ((millisUntilFinished % 60000) / 1000).toInt()
                tvCountdown.text = String.format("%02d:%02d:%02d", h, m, s)
            }

            override fun onFinish() {
                unlockAndStop()
            }
        }.start()
    }

    private fun unlockAndStop() {
        sharedPrefsHelper.clearTargetUnlockTime()
        stopSelf() // Sẽ gọi hàm onDestroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}