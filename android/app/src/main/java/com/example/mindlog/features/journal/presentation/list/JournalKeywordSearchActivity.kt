package com.example.mindlog.features.journal.presentation.list

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mindlog.R
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.databinding.ActivityJournalKeywordSearchBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JournalKeywordSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalKeywordSearchBinding

    companion object {
        const val EXTRA_KEYWORD = "EXTRA_KEYWORD"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalKeywordSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        val keyword = intent.getStringExtra(EXTRA_KEYWORD)

        if (keyword.isNullOrBlank()) {
            finish() // 키워드가 없으면 액티비티 종료
            return
        }

        binding.toolbar.title = "키워드: $keyword"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, JournalKeywordSearchFragment.newInstance(keyword))
                .commit()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }
}
