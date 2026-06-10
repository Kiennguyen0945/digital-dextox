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
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class LockService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var countDownTimer: CountDownTimer? = null
    private lateinit var tvCountdown: TextView
    private lateinit var sharedPrefsHelper: SharedPrefsHelper
    private val CHANNEL_ID = "LockServiceChannel"
    
    private var currentRandomString: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sharedPrefsHelper = SharedPrefsHelper(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Chuyển việc khởi động notification và view sang onStartCommand
        startForeground(1, createNotification())
        
        // Tránh tình trạng vẽ đè nhiều lần nếu service gọi tới lui
        if (overlayView == null) {
            showOverlay()
            startCountdown()
        }
        
        return START_STICKY // Giúp Service tự khởi động lại nếu bị hệ thống giết
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
            PixelFormat.TRANSLUCENT
        )
        // Hỗ trợ cuộn / đẩy layout lên khi hiện bàn phím
        layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        layoutParams.gravity = Gravity.CENTER

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.layout_lock_overlay, null)
        
        tvCountdown = overlayView!!.findViewById(R.id.tvCountdown)
        
        setupUnlockEarlyLogic()

        windowManager.addView(overlayView, layoutParams)
    }

    private fun setupUnlockEarlyLogic() {
        val btnUnlockEarly = overlayView!!.findViewById<Button>(R.id.btnUnlockEarly)
        val layoutUnlockArea = overlayView!!.findViewById<LinearLayout>(R.id.layoutUnlockArea)
        val tvRandomHash = overlayView!!.findViewById<TextView>(R.id.tvRandomHash)
        val etHashInput = overlayView!!.findViewById<EditText>(R.id.etHashInput)
        val btnConfirmUnlock = overlayView!!.findViewById<Button>(R.id.btnConfirmUnlock)

        btnUnlockEarly.setOnClickListener {
            btnUnlockEarly.visibility = View.GONE
            layoutUnlockArea.visibility = View.VISIBLE

            // Sinh chuỗi nếu chưa có để giữ nguyên khi xoay màn hình/vẽ lại
            if (currentRandomString.isEmpty()) {
                val configuredLength = sharedPrefsHelper.getHashLength()
                currentRandomString = generateRandomString(configuredLength)
            }
            tvRandomHash.text = currentRandomString
        }

        btnConfirmUnlock.setOnClickListener {
            val input = etHashInput.text.toString().trim()
            if (input == currentRandomString) {
                Toast.makeText(this, "Kỷ luật đã bị phá vỡ!", Toast.LENGTH_SHORT).show()
                unlockAndStop()
            } else {
                Toast.makeText(this, "Sai mã! Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show()
                // Theo yêu cầu: KHÔNG xóa input, KHÔNG đổi mã
            }
        }
    }

    private fun generateRandomString(length: Int): String {
        // Bỏ các ký tự dễ nhầm lẫn như 0, O, o, 1, l, I
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
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
                
                // ĐÃ XÓA LỆNH GÂY CRASH Ở ĐÂY
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