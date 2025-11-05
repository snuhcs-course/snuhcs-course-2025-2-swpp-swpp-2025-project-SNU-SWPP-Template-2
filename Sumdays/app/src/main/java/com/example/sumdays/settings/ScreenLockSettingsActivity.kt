package com.example.sumdays.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.R
import com.example.sumdays.databinding.ActivitySettingsBackupBinding
import com.example.sumdays.databinding.ActivitySettingsNotificationBinding
import com.example.sumdays.databinding.ActivitySettingsScreenLockBinding

class ScreenLockSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsScreenLockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsScreenLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "잠금 설정"

        val backAction = { finish() }
        binding.header.headerBackIcon.setOnClickListener { backAction() }
    }
}