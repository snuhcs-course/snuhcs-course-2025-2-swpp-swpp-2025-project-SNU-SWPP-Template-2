package com.example.sumdays.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.R
import com.example.sumdays.databinding.ActivitySettingsBackupBinding
import com.example.sumdays.databinding.ActivitySettingsDiaryStyleBinding

class DiaryStyleSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsDiaryStyleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsDiaryStyleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "일기 생성 스타일 설정"

        val backAction = { finish() }
        binding.header.headerBackIcon.setOnClickListener { backAction() }
    }
}