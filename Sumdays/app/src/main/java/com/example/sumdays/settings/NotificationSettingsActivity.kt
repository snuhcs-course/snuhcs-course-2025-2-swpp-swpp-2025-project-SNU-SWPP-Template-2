package com.example.sumdays.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sumdays.databinding.ActivitySettingsNotificationBinding
import com.example.sumdays.reminder.ReminderPrefs
import com.example.sumdays.reminder.ReminderScheduler
import java.util.Calendar
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsNotificationBinding
    private lateinit var reminderPrefs: ReminderPrefs
    private lateinit var alarmAdapter: AlarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reminderPrefs = ReminderPrefs(this)

        // DUMMY: AlarmAdapter 초기화 (실제 프로젝트에서는 AlarmAdapter 클래스가 존재해야 함)
        alarmAdapter = AlarmAdapter(
            onTimeClicked = { position, currentTime -> showTimePicker(position, currentTime) },
            onDeleteClicked = { position -> deleteAlarm(position) }
        )

        setupHeader()
        setupRecyclerView()
        setupMasterSwitch()
        setupListeners()

        // 화면 진입 시 알람 목록 로드 및 UI 업데이트
        loadAlarmSettings()
    }

    // 정확한 알람 권한 확인 및 요청 함수
    private fun checkExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "정확한 알람 설정을 위해 권한이 필요합니다.", Toast.LENGTH_LONG).show()

                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)

                return false // 권한이 없으므로 알람 등록 중단
            }
        }
        return true // 권한이 있거나 API 레벨이 낮아 권한이 불필요함
    }


    private fun setupHeader() {
        binding.header.headerTitle.text = "리마인더 설정"
        binding.header.headerBackIcon.setOnClickListener { finish() }
    }

    // --- 1. 마스터 스위치 로직 ---

    private fun setupMasterSwitch() {
        // 1. 초기 상태 로드
        val isMasterOn = reminderPrefs.isMasterOn()
        binding.masterSwitch.isChecked = isMasterOn
        updateUiState(isMasterOn)

        // 2. 스위치 변경 리스너
        binding.masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            reminderPrefs.setMasterSwitch(isChecked)
            updateUiState(isChecked)

            if (isChecked) {
                // ON: 권한 확인 후 알람 등록
                if (checkExactAlarmPermission(this)) {
                    ReminderScheduler.scheduleAllReminders(this)
                    Toast.makeText(this, "리마인더가 활성화되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 권한 요청으로 넘어갔으므로 스위치를 OFF 상태로 되돌리고 알람 등록 중단
                    binding.masterSwitch.isChecked = false
                    reminderPrefs.setMasterSwitch(false)
                    updateUiState(false)
                }
            } else {
                // OFF: 모든 알람 취소
                ReminderScheduler.cancelAllReminders(this)
                Toast.makeText(this, "리마인더가 비활성화되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 알람 마스터 스위치 상태에 따라 UI 활성화/비활성화 및 흐림 효과 적용
     */
    private fun updateUiState(isOn: Boolean) {
        val alpha = if (isOn) 1.0f else 0.5f

        // Container의 alpha와 isEnabled 상태 변경
        binding.alarmListContainer.alpha = alpha
        binding.alarmListContainer.isEnabled = isOn

        // 어댑터에도 상태를 전달하여 개별 아이템의 상태 업데이트 유도
        alarmAdapter.isMasterOn = isOn
    }

    // --- 2. RecyclerView 및 Adapter 설정 ---

    private fun setupRecyclerView() {
        // 콜백 함수 정의
        alarmAdapter = AlarmAdapter(
            onTimeClicked = { position, currentTime -> showTimePicker(position, currentTime) },
            onDeleteClicked = { position -> deleteAlarm(position) }
        )

        binding.alarmTimeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationSettingsActivity)
            adapter = alarmAdapter
        }
    }

    private fun loadAlarmSettings() {
        val times = reminderPrefs.getAlarmTimes()
        alarmAdapter.updateList(times)

        // 마스터 스위치 상태에 따라 초기 UI 상태 설정
        updateUiState(reminderPrefs.isMasterOn())
    }

    private fun setupListeners() {
        binding.addAlarmButton.setOnClickListener {
            // 최대 10개 제한 확인
            if (alarmAdapter.itemCount >= 10) {
                Toast.makeText(this, "알림은 최대 10개까지 설정할 수 있습니다.", Toast.LENGTH_SHORT).show()
            } else {
                showTimePicker(null, null) // 새 알람 추가 모드
            }
        }
    }

    // --- 3. 시간 선택기(Time Picker) 로직 ---

    private fun showTimePicker(position: Int?, currentTime: String?) {
        val calendar = Calendar.getInstance()
        val initialHour =
            currentTime?.split(":")?.get(0)?.toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute =
            currentTime?.split(":")?.get(1)?.toIntOrNull() ?: calendar.get(Calendar.MINUTE)

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H) // 또는 24H
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setTitleText(if (position == null) "새 알림 시간 설정" else "알림 시간 수정")
            .build()
        picker.show(supportFragmentManager, "time_picker_tag")

        picker.addOnPositiveButtonClickListener {
            val newTime = String.format("%02d:%02d", picker.hour, picker.minute)
            saveOrUpdateAlarm(position, newTime)
        }
    }

    private fun saveOrUpdateAlarm(position: Int?, newTime: String) {
        if (!checkExactAlarmPermission(this)) {
            // 권한 요청으로 넘어갔으므로 알람 등록을 중단합니다.
            return
        }

        val currentTimes = reminderPrefs.getAlarmTimes().toMutableList()

        if (position == null) {
            // 추가 모드
            currentTimes.add(newTime)
            Toast.makeText(this, "$newTime 알림이 추가되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            // 수정 모드
            currentTimes[position] = newTime
            Toast.makeText(this, "$newTime 으로 알림이 수정되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 1. Prefs에 새 목록 저장
        reminderPrefs.setAlarmTimes(currentTimes)
        // 2. UI 및 스케줄러 업데이트
        loadAlarmSettings()

        // 권한 확인 후 안전하게 호출
        if (reminderPrefs.isMasterOn()) {
            ReminderScheduler.scheduleAllReminders(this)
        }
    }

    // --- 4. 기타 알람 관리 로직 ---

    private fun deleteAlarm(position: Int) {
        val currentTimes = reminderPrefs.getAlarmTimes().toMutableList()
        val deletedTime = currentTimes.removeAt(position)

        reminderPrefs.setAlarmTimes(currentTimes)
        Toast.makeText(this, "$deletedTime 알림이 삭제되었습니다.", Toast.LENGTH_SHORT).show()

        // UI 및 스케줄러 업데이트
        loadAlarmSettings()

        // 권한 확인 후 안전하게 호출
        if (reminderPrefs.isMasterOn()) {
            if (checkExactAlarmPermission(this)) {
                ReminderScheduler.scheduleAllReminders(this)
            }
        }
    }
}