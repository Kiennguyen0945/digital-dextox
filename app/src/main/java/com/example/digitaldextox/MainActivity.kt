package com.example.digitaldextox

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.text.InputType
import android.graphics.Color
import java.util.Calendar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPrefsHelper: SharedPrefsHelper
    private var startHour = 22
    private var startMinute = 0
    private var endHour = 6
    private var endMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        sharedPrefsHelper = SharedPrefsHelper(this)

        val etMinutes = findViewById<EditText>(R.id.etMinutes)
        val btnLockNow = findViewById<Button>(R.id.btnLockNow)
        
        val btnStartTime = findViewById<Button>(R.id.btnStartTime)
        val btnEndTime = findViewById<Button>(R.id.btnEndTime)
        val btnSaveSchedule = findViewById<Button>(R.id.btnSaveSchedule)
        val btnCancelSchedule = findViewById<Button>(R.id.btnCancelSchedule)
        
        // UI Độ khó
        val etHashLength = findViewById<EditText>(R.id.etHashLength)
        val btnSaveHashLength = findViewById<Button>(R.id.btnSaveHashLength)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Logic lưu độ khó ---
        etHashLength.setText(sharedPrefsHelper.getHashLength().toString())

        btnSaveHashLength.setOnClickListener {
            saveCurrentHashLength(etHashLength)
            Toast.makeText(this, "Đã lưu độ dài chuỗi!", Toast.LENGTH_SHORT).show()
        }

        btnLockNow.setOnClickListener {
            val minutesStr = etMinutes.text.toString()
            if (minutesStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số phút!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tự động lưu độ dài Hash trước khi khóa
            saveCurrentHashLength(etHashLength)

            val minutes = minutesStr.toLong()
            if (Settings.canDrawOverlays(this)) {
                startLock(minutes)
            } else {
                requestOverlayPermission()
            }
        }

        // --- Bổ sung Logic Chọn giờ và Lưu lịch trình ---
        
        if (sharedPrefsHelper.hasSchedule()) {
            val start = sharedPrefsHelper.getScheduleStart()
            val end = sharedPrefsHelper.getScheduleEnd()
            startHour = start.first
            startMinute = start.second
            endHour = end.first
            endMinute = end.second
            updateTimeButtonsUI(btnStartTime, btnEndTime)
        }

        btnStartTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                startHour = h
                startMinute = m
                updateTimeButtonsUI(btnStartTime, btnEndTime)
            }, startHour, startMinute, true).show()
        }

        btnEndTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                endHour = h
                endMinute = m
                updateTimeButtonsUI(btnStartTime, btnEndTime)
            }, endHour, endMinute, true).show()
        }

        btnSaveSchedule.setOnClickListener {
            handleSaveScheduleClick()
        }

        btnCancelSchedule.setOnClickListener {
            handleCancelScheduleClick()
        }
    }

    private fun handleSaveScheduleClick() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        // Tính toán thời gian khóa
        var durationHours = endHour - startHour + (endMinute - startMinute) / 60.0
        if (durationHours <= 0) {
            durationHours += 24.0 // Xuyên đêm
        }

        if (durationHours >= 16.0) {
            Toast.makeText(this, "Không thể khóa quá 16 tiếng/ngày để đảm bảo an toàn!", Toast.LENGTH_LONG).show()
            return
        }

        val freeTime = 24.0 - durationHours

        val saveRoutine = {
            AlertDialog.Builder(this)
                .setTitle("Xác nhận kích hoạt")
                .setMessage("Bạn sẽ bị khóa máy khoảng ${"%.1f".format(durationHours)} tiếng.\n" +
                        "Bạn có ${"%.1f".format(freeTime)} tiếng tự do mỗi ngày.\n" +
                        "Để sửa hoặc hủy, bạn sẽ phải gõ mã xác nhận. Bạn chắc chắn chứ?")
                .setPositiveButton("CHẮC CHẮN") { _, _ ->
                    executeSaveSchedule()
                }
                .setNegativeButton("HỦY", null)
                .show()
        }

        if (sharedPrefsHelper.hasSchedule()) {
            showChallengeDialog("Để thay đổi lịch, hãy nhập mã dưới đây:") { saveRoutine() }
        } else {
            saveRoutine()
        }
    }

    private fun handleCancelScheduleClick() {
        if (!sharedPrefsHelper.hasSchedule()) {
            Toast.makeText(this, "Bạn chưa có lịch trình nào!", Toast.LENGTH_SHORT).show()
            return
        }

        showChallengeDialog("Để HỦY lịch khóa, hãy nhập mã dưới đây:") {
            sharedPrefsHelper.clearSchedule()
            
            // Hủy báo thức
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java)
            intent.action = "ACTION_START_LOCK"
            val pendingIntent = PendingIntent.getBroadcast(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarmManager.cancel(pendingIntent)

            Toast.makeText(this, "Đã hủy lịch trình!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showChallengeDialog(message: String, onSuccess: () -> Unit) {
        val hashLength = sharedPrefsHelper.getHashLength()
        val randomString = generateRandomString(hashLength)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        // TextView hiển thị mã
        val tvCode = android.widget.TextView(this).apply {
            text = randomString
            textSize = 16f
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#EEEEEE"))
            setTextIsSelectable(false)
        }
        layout.addView(tvCode)

        // Dùng lớp NoPasteEditText đã code ở UC3
        val etInput = NoPasteEditText(this).apply {
            hint = "Nhập chính xác chuỗi trên"
            inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        layout.addView(etInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Xác thực kỷ luật")
            .setMessage(message)
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("XÁC NHẬN", null) // Để null để không tự động đóng
            .setNegativeButton("QUAY LẠI", null)
            .create()

        // Can thiệp sự kiện Click sau khi dialog hiển thị để không bị mất text và không bị đóng
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (etInput.text.toString().trim() == randomString) {
                    dialog.dismiss()
                    onSuccess()
                } else {
                    Toast.makeText(this, "Nhập sai! Vui lòng kiểm tra lại lỗi sai của bạn.", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss() // Chỉ thoát khi bấm Quay lại
            }
        }
        dialog.show()
    }

    private fun generateRandomString(length: Int): String {
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
        return (1..length).map { allowedChars.random() }.joinToString("")
    }

    private fun executeSaveSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                Toast.makeText(this, "Vui lòng cấp quyền đặt báo thức chính xác!", Toast.LENGTH_LONG).show()
                return
            }
        }
        val etHashLength = findViewById<EditText>(R.id.etHashLength)
        saveCurrentHashLength(etHashLength)
        sharedPrefsHelper.saveSchedule(startHour, startMinute, endHour, endMinute)
        scheduleLockLogic()
        Toast.makeText(this, "Đã lưu lịch trình thành công!", Toast.LENGTH_SHORT).show()
    }

    // Hàm tiện ích để bắt và lưu giá trị hash ngay lập tức
    private fun saveCurrentHashLength(etHashLength: EditText) {
        // THÊM .trim() ĐỂ XÓA KHOẢNG TRẮNG THỪA DO BÀN PHÍM TỰ ĐỘNG SINH RA
        val lenStr = etHashLength.text.toString().trim()
        val len = lenStr.toIntOrNull()
        if (len != null && len > 0) {
            sharedPrefsHelper.saveHashLength(len)
        }
    }

    private fun updateTimeButtonsUI(btnStart: Button, btnEnd: Button) {
        btnStart.text = String.format("Bắt đầu: %02d:%02d", startHour, startMinute)
        btnEnd.text = String.format("Kết thúc: %02d:%02d", endHour, endMinute)
    }

    private fun scheduleLockLogic() {
        val now = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
        }

        if (endCal.before(startCal) || endCal == startCal) {
            endCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Nếu qua ngày hôm sau và lúc này giờ đang thuộc ngày hôm sau nhưng nằm trong khoảng khóa
        val startCalYesterday = startCal.clone() as Calendar
        startCalYesterday.add(Calendar.DAY_OF_YEAR, -1)
        val endCalYesterday = endCal.clone() as Calendar
        endCalYesterday.add(Calendar.DAY_OF_YEAR, -1)

        val isInsideToday = now.after(startCal) && now.before(endCal)
        val isInsideYesterday = now.after(startCalYesterday) && now.before(endCalYesterday)

        if (isInsideToday || isInsideYesterday) {
            // EC2: Đang trong khung giờ khóa -> Khóa ngay lập tức
            val target = if (isInsideToday) endCal.timeInMillis else endCalYesterday.timeInMillis
            sharedPrefsHelper.saveTargetUnlockTime(target)
            val serviceIntent = Intent(this, LockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            finish()
        } else {
            // Setup Alarm
            if (now.after(startCal)) {
                startCal.add(Calendar.DAY_OF_YEAR, 1) // Ngày mai khóa
            }
            setAlarmFor(startCal.timeInMillis)
        }
    }

    private fun setAlarmFor(timeInMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.action = "ACTION_START_LOCK"
        val pendingIntent = PendingIntent.getBroadcast(
            this, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
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