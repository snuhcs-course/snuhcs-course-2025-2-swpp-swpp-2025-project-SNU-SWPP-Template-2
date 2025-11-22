package com.example.mindlog.features.journal.presentation.write

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.setText
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.R
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.databinding.ActivityJournalWriteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JournalWriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalWriteBinding

    private val emotionSelectFragment by lazy { EmotionSelectFragment() }
    private val contentWriteFragment by lazy { ContentWriteFragment() }

    // 1. ViewModel을 주입받음
    private val viewModel: JournalWriteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.write_fragment_container, emotionSelectFragment)
                .commit()
            updateButtonsForEmotionSelect()
        }

        setupButtonClickListeners()
        setupOnBackPressed()
        observeViewModel() // 3. ViewModel의 상태 변화를 관찰하는 함수 호출
    }

    private fun setupButtonClickListeners() {
        binding.btnCancelOrBack.setOnClickListener {
            handleBackButton()
        }

        binding.btnNextOrSave.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.write_fragment_container)
            if (currentFragment is EmotionSelectFragment) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.write_fragment_container, contentWriteFragment)
                    .addToBackStack(null)
                    .commit()
                updateButtonsForContentWrite()
            } else {
                // 2. '작성' 버튼 클릭 시 ViewModel의 saveJournal 함수 호출
                viewModel.saveJournal()
            }
        }
    }

    // 3. ViewModel의 saveResult 이벤트를 구독하고 결과에 따라 UI 처리

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.saveResult.collect { result ->
                when (result) {
                    is Result.Success -> {
                        setResult(RESULT_OK)
                        Toast.makeText(this@JournalWriteActivity, "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is Result.Error -> {
                        binding.btnNextOrSave.isEnabled = true
                        binding.btnCancelOrBack.isEnabled = true
                        val errorMessage = result.message ?: "알 수 없는 오류가 발생했습니다."

                        // ✨ [핵심 수정] '감사한 일' 칸에 에러를 쓰는 대신, Log.e로 에러를 기록하고 사용자에게는 간단한 토스트만 보여줍니다.
                        Log.e("JournalWriteError", "일기 저장 실패: $errorMessage")
                        Toast.makeText(this@JournalWriteActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun handleBackButton() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            updateButtonsForEmotionSelect()
        } else {
            finish()
        }
    }

    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackButton()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun updateButtonsForEmotionSelect() {
        binding.btnCancelOrBack.text = "취소"
        binding.btnNextOrSave.text = "다음"
    }

    private fun updateButtonsForContentWrite() {
        binding.btnCancelOrBack.text = "뒤로"
        binding.btnNextOrSave.text = "작성"
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }
}
