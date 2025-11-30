package com.example.mindlog.features.journal.presentation.detail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.R
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.databinding.ActivityJournalDetailBinding
import com.example.mindlog.features.journal.presentation.write.JournalEditActivity
import com.example.mindlog.features.journal.presentation.write.JournalEditViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JournalDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalDetailBinding
    private val viewModel: JournalEditViewModel by viewModels()

    companion object {
        const val EXTRA_JOURNAL_ID = "EXTRA_JOURNAL_ID"
        // ✨ 결과를 반환할 때 사용할 새로운 상수 정의
        const val EXTRA_UPDATED_JOURNAL_ID = "EXTRA_UPDATED_JOURNAL_ID"
        const val EXTRA_DELETED_JOURNAL_ID = "EXTRA_DELETED_JOURNAL_ID"
    }

    private val editJournalLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 수정이 완료되면, 수정된 ID를 결과 Intent에 담아 설정
            val resultIntent = Intent().apply {
                putExtra(EXTRA_UPDATED_JOURNAL_ID, viewModel.journalId!!)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            // 현재 화면의 내용도 새로고침
            viewModel.loadJournalDetails(viewModel.journalId!!, forceRefresh = true)
        }
        // 단순 뒤로가기(RESULT_OK가 아님)의 경우 아무것도 하지 않음
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        val journalId = intent.getIntExtra(EXTRA_JOURNAL_ID, -1)
        if (journalId == -1) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupFragment()
        setupClickListeners()
        observeViewModel()

        viewModel.loadJournalDetails(journalId)
    }

    private fun setupToolbar() {
        // 뒤로가기 버튼은 아무런 result도 설정하지 않고 그냥 종료만 함
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.detail_fragment_container, JournalDetailFragment())
            .commit()
    }

    private fun setupClickListeners() {
        binding.btnDetailEdit.setOnClickListener {
            val intent = Intent(this, JournalEditActivity::class.java).apply {
                putExtra(JournalEditActivity.EXTRA_JOURNAL_ID, viewModel.journalId)
            }
            editJournalLauncher.launch(intent)
        }
        binding.btnDetailDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.editResult.collect { result ->
                if (result is Result.Success && result.data == "삭제 완료") {
                    Toast.makeText(this@JournalDetailActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    // 삭제가 완료되면, 삭제된 ID를 결과 Intent에 담아 설정하고 종료
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_DELETED_JOURNAL_ID, viewModel.journalId!!)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else if (result is Result.Error) {
                    Toast.makeText(this@JournalDetailActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("일기 삭제")
            .setMessage("정말로 이 일기를 삭제하시겠습니까? 삭제된 데이터는 복구할 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteJournal()
            }
            .show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiHelper.hideSystemUI(this)
        }
    }
}
