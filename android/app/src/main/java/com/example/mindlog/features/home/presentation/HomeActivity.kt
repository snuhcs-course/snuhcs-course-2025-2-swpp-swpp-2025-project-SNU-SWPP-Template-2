package com.example.mindlog.features.home.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mindlog.R
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.databinding.ActivityHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    interface FabClickListener {
        fun onFabClick()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        setupBottomNav()
        setupFab()
    }

    private fun setupBottomNav() {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.homeNavHost) as NavHostFragment
        val navController = navHost.navController

        binding.bottomNav.apply {
            setupWithNavController(navController)
            setOnItemReselectedListener { /* 탭 재선택시 스크롤 탑 등 필요하면 처리 */ }
        }
    }

    private fun setupFab() {
        binding.fabWrite.setOnClickListener {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.homeNavHost)
            val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)

            if (currentFragment is FabClickListener) {
                currentFragment.onFabClick()
            }
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }
}
