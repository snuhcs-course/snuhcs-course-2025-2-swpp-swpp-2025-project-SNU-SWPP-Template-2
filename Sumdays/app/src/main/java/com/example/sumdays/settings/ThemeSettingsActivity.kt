package com.example.sumdays.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.R
import com.example.sumdays.databinding.ActivitySettingsThemeBinding


enum class ViewType {
    SYSTEM, LIGHT, DARK
}
class ThemeSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsThemeBinding

    private var selectedTheme: ViewType = ViewType.SYSTEM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()
        updateUI()
        setClickListeners()
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "테마 설정"

        val backAction = { finish() }
        binding.header.headerBackIcon.setOnClickListener { backAction() }
    }

    private fun setClickListeners() = with(binding) {
        themeSystem.setOnClickListener {
            selectedTheme = ViewType.SYSTEM
            updateUI()
        }
        themeLight.setOnClickListener {
            selectedTheme = ViewType.LIGHT
            updateUI()
        }
        themeDark.setOnClickListener {
            selectedTheme = ViewType.DARK
            updateUI()
        }
    }

    private fun updateUI() = with(binding) {
        themeSystem.setBackgroundResource(
            if (selectedTheme == ViewType.SYSTEM) R.drawable.settings_theme_btn_selected else R.drawable.settings_theme_btn_unselected
        )
        themeLight.setBackgroundResource(
            if (selectedTheme == ViewType.LIGHT) R.drawable.settings_theme_btn_selected else R.drawable.settings_theme_btn_unselected
        )
        themeDark.setBackgroundResource(
            if (selectedTheme == ViewType.DARK) R.drawable.settings_theme_btn_selected else R.drawable.settings_theme_btn_unselected
        )
    }
}