package com.aisportspt.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aisportspt.app.databinding.ActivityMainBinding
import com.aisportspt.app.ui.fragments.UserFragment
import com.aisportspt.app.ui.fragments.AiCoachFragment
import com.aisportspt.app.ui.fragments.SportsFragment
import com.aisportspt.app.ui.fragments.TrainingFragment
import com.aisportspt.app.ui.fragments.ShoppingFragment
import com.aisportspt.app.ui.viewmodels.MainViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation()
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        if (savedInstanceState == null) {
            loadFragment(SportsFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_training
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_user -> {
                    loadFragment(UserFragment())
                    true
                }
                R.id.nav_ai_coach -> {
                    loadFragment(AiCoachFragment())
                    true
                }
                R.id.nav_training -> {
                    loadFragment(TrainingFragment())
                    true
                }
                R.id.nav_shopping -> {
                    loadFragment(ShoppingFragment())
                    true
                }
                R.id.nav_add ->{
                    loadFragment(SportsFragment())
                    true
                }
                else -> false
            }
        }
    }


    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    
    fun getViewModel(): MainViewModel = viewModel
}