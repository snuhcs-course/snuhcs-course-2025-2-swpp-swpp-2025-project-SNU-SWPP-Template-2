package com.aisportspt.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aisportspt.app.R
import com.aisportspt.app.ui.fragments.LoginEmailFragment

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.login_container, LoginEmailFragment())
                .commit()
        }
    }
}
