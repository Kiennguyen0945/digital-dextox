package com.example.digitaldextox

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPrefsHelper: SharedPrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        sharedPrefsHelper = SharedPrefsHelper(this)

        val etMinutes = findViewById<EditText>(R.id.etMinutes)
        val btnLockNow = findViewById<Button>(R.id.btnLockNow)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnLockNow.setOnClickListener {
            val minutesStr = etMinutes.text.toString()
            if (minutesStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số phút!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val minutes = minutesStr.toLong()
            if (Settings.canDrawOverlays(this)) {
                startLock(minutes)
            } else {
                requestOverlayPermission()
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Đã cấp quyền! Bấm lại nút để khóa.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền vẽ đè để dùng app!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLock(minutes: Long) {
        val targetTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        sharedPrefsHelper.saveTargetUnlockTime(targetTime)

        val serviceIntent = Intent(this, LockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        finish() // Thoát activity để test overlay
    }
}