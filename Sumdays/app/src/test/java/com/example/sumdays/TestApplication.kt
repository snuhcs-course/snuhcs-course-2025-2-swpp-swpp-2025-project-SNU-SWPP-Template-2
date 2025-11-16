package com.example.sumdays

import android.app.Application
import com.example.sumdays.R

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Material Components 기반 테마 적용
        setTheme(R.style.Theme_First)
    }
}
