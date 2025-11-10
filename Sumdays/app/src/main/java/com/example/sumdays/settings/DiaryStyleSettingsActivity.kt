package com.example.sumdays.settings

// com.example.sumdays.settings.DiaryStyleSettingsActivity.kt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sumdays.auth.SessionManager
import com.example.sumdays.databinding.ActivitySettingsDiaryStyleBinding
import com.example.sumdays.network.*
import com.example.sumdays.settings.prefs.UserStatsPrefs
import com.example.sumdays.data.* // UserStyle, UserStyleViewModel 등
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.data.style.UserStyleViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class DiaryStyleSettingsActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var binding: ActivitySettingsDiaryStyleBinding
    private lateinit var userStatsPrefs: UserStatsPrefs
    private lateinit var styleViewModel: UserStyleViewModel // Room ViewModel 가정
    private lateinit var styleAdapter: UserStyleAdapter

    // Coroutine Job 관리 (Activity 종료 시 취소)
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsDiaryStyleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userStatsPrefs = UserStatsPrefs(this)
        styleViewModel = ViewModelProvider(this).get(UserStyleViewModel::class.java) // ViewModel 초기화

        setupHeader()
        setupRecyclerView()
        setupListeners()
        observeStyles()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // 코루틴 Job 취소
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "AI 스타일 설정"
        binding.header.headerBackIcon.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        styleAdapter = UserStyleAdapter(
            onStyleSelected = { styleId -> handleStyleSelection(styleId) },
            onDeleteClicked = { style -> handleDeleteStyle(style) },
            // ★★★ 비활성화 콜백 추가 ★★★
            onStyleDeactivated = { handleStyleDeactivation() }
        )
        binding.styleRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DiaryStyleSettingsActivity)
            adapter = styleAdapter
        }
    }

    private fun setupListeners() {
        binding.extractStyleButton.setOnClickListener {
            // ★ 스타일 추출 액티비티(데이터 수집 화면)로 이동
            val intent = Intent(this, StyleExtractionActivity::class.java) // StyleExtractionActivity는 별도 구현 필요
            startActivity(intent)
        }
    }

    // --- 1. Room LiveData 관찰 및 UI 업데이트 ---

    private fun observeStyles() {
        // UserStyleViewModel에서 해당 유저의 모든 스타일 목록 LiveData를 가져옴 (가정)
        // SessionManager.getUserId()는 Int를 반환하지만, DB FK는 String이어야 하므로 변환 (SessionManager의 userId가 String이라고 가정하고 진행)

        styleViewModel.getAllStyles().observe(this) { stylesList ->
            // Prefs에서 현재 활성 스타일 ID를 가져와 함께 전달
            val activeStyleId = userStatsPrefs.getActiveStyleId() // Prefs에 activeStyleId getter 추가 가정
            styleAdapter.updateList(stylesList, activeStyleId)
        }
    }

    // --- 2. 스타일 선택 로직 (UserStats Prefs 업데이트) ---

    private fun handleStyleSelection(styleId: Long) = launch(Dispatchers.IO) {
        // Prefs에 활성 스타일 ID 저장 (IO 스레드에서 처리)
        userStatsPrefs.saveActiveStyleId(styleId) // Prefs에 setter 추가 가정

        withContext(Dispatchers.Main) {
            Toast.makeText(this@DiaryStyleSettingsActivity, "스타일이 활성화되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 3. 스타일 삭제 로직 (Room DB 및 Prefs 업데이트) ---

    private fun handleDeleteStyle(style: UserStyle) = launch(Dispatchers.IO) {
        // 스타일을 Room DB에서 삭제
        styleViewModel.deleteStyle(style)

        // 만약 삭제된 스타일이 활성 스타일이었다면, 활성 스타일을 해제 (null 또는 기본 스타일 ID)
        if (style.styleId == userStatsPrefs.getActiveStyleId()) {
            userStatsPrefs.clearActiveStyleId() // Prefs에 clear 로직 추가 가정
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(this@DiaryStyleSettingsActivity, "${style.styleName}이(가) 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun handleStyleDeactivation() {
        userStatsPrefs.clearActiveStyleId() // SharedPreferences에서 ID 제거
        // 뷰모델 또는 LiveData를 통해 목록을 다시 로드하여 UI를 업데이트해야 합니다.
        // styleViewModel.loadStyles()
        Toast.makeText(this, "활성 스타일이 해제되었습니다.", Toast.LENGTH_SHORT).show()
    }
}