package com.example.sumdays.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.R
import com.example.sumdays.databinding.ActivitySettingsAccountBinding
import com.example.sumdays.databinding.ActivitySettingsBackupBinding

class BackupSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBackupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "알림 설정"

        val backAction = { finish() }
        binding.header.headerBackIcon.setOnClickListener { backAction() }
    }
}