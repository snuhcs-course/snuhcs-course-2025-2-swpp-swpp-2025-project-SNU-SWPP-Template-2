package com.example.sumdays

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.databinding.ActivitySettingsMainBinding
import com.example.sumdays.settings.AccountSettingsActivity
import com.example.sumdays.settings.BackupSettingsActivity
import com.example.sumdays.settings.DiaryStyleSettingsActivity
import com.example.sumdays.settings.NotificationSettingsActivity
import com.example.sumdays.settings.ScreenLockSettingsActivity
import com.example.sumdays.settings.ThemeSettingsActivity
import org.threeten.bp.LocalDate

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 사용자 블록: 프로필 눌렀을 때
        binding.userBlock.setOnClickListener {

        }

        setSettingsBtnListener()
        setNavigationBtnListener()
    }

    private fun setNavigationBtnListener() = with(binding.navigationBar) {
        btnCalendar.setOnClickListener {
            val intent = Intent(this@SettingsActivity, CalendarActivity::class.java)
            startActivity(intent)
        }
        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val intent = Intent(this@SettingsActivity, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
        }
        btnInfo.setOnClickListener {

        }
    }

    private fun setSettingsBtnListener() = with(binding) {
        binding.themeBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, ThemeSettingsActivity::class.java))
        }

        binding.notificationBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, NotificationSettingsActivity::class.java))
        }

        binding.lockBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, ScreenLockSettingsActivity::class.java))
        }

        binding.diaryStyleBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, DiaryStyleSettingsActivity::class.java))
        }

        binding.accountBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, AccountSettingsActivity::class.java))
        }

        binding.backupBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, BackupSettingsActivity::class.java))
        }
    }
}