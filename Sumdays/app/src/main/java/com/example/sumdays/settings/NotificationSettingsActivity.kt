package com.example.sumdays.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.R
import com.example.sumdays.databinding.ActivitySettingsDiaryStyleBinding
import com.example.sumdays.databinding.ActivitySettingsNotificationBinding

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "알림 설정"

        val backAction = { finish() }
        binding.header.headerBackIcon.setOnClickListener { backAction() }
    }

}