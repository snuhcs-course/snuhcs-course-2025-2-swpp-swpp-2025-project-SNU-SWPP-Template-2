package com.example.sumdays

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.databinding.ActivitySettingsMainBinding
import com.example.sumdays.settings.AccountSettingsActivity
import com.example.sumdays.settings.DiaryStyleSettingsActivity
import com.example.sumdays.settings.NotificationSettingsActivity
import com.example.sumdays.settings.prefs.UserStatsPrefs
import org.threeten.bp.LocalDate
import androidx.lifecycle.ViewModelProvider
import com.example.sumdays.data.viewModel.DailyEntryViewModel
import kotlinx.coroutines.*
import com.example.sumdays.auth.SessionManager
import com.example.sumdays.LoginActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsMainBinding
    private lateinit var viewModel: DailyEntryViewModel
    private lateinit var userStatsPrefs: UserStatsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. UserStatsPrefs 인스턴스 초기화
        userStatsPrefs = UserStatsPrefs(this)
        viewModel = ViewModelProvider(this).get(DailyEntryViewModel::class.java)


        binding.logoutBlock.setOnClickListener {
            SessionManager.clearSession()
            // 로그인 화면으로 이동
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        // 3. 닉네임 로드 및 표시 함수 호출
        loadAndDisplayNickname()
//        calculateAndDisplayStreak() // ★ Strike 계산 시작
//
//        loadAndDisplayCounts() // ★ Leaf와 Grape 카운트 표시 함수 호출 ★

        setSettingsBtnListener()
        setNavigationBtnListener()
    }

//    private fun calculateAndDisplayStreak() {
//        // CoroutineScope를 사용하여 백그라운드 (IO 스레드)에서 DB 접근 및 계산 수행
//        CoroutineScope(Dispatchers.IO).launch {
//
//            // 1. Room에서 모든 작성 날짜(String)를 가져옵니다.
//            val allDates = viewModel.getAllWrittenDates()
//
//            // 2. Strike 횟수를 계산합니다.
//            val streak = calculateCurrentStreak(allDates) // 아래 정의할 계산 함수 호출
//
//            // 3. UserStatsPrefs에 저장합니다.
//            userStatsPrefs.saveStreak(streak)
//
//            // 4. Main 스레드에서 UI 업데이트 (선택 사항: 계산된 값을 화면에 바로 반영)
//            withContext(Dispatchers.Main) {
//                binding.strike.text = "\uD83D\uDD25 Strike: "+streak.toString()
//            }
//        }
//    }
//
//    fun calculateCurrentStreak(dates: List<String>): Int {
//        if (dates.isEmpty()) return 0
//
//        // 날짜 문자열을 LocalDate 객체로 변환하고 중복 제거, 내림차순 정렬
//        val uniqueDates = dates.toSet()
//            .map { LocalDate.parse(it) }
//            .sortedDescending()
//
//        var currentStreak = 0
//        var currentDate = LocalDate.now()
//        var isTodayWritten = uniqueDates.any { it.isEqual(currentDate) }
//
//        // 1. 오늘 날짜부터 시작하여 연속성 검사
//        while (true) {
//            if (uniqueDates.contains(currentDate)) {
//                currentStreak++
//            } else if (!isTodayWritten && currentDate.isEqual(LocalDate.now())) {
//                // 오늘 날짜이고, 오늘 일기가 작성되지 않았다면 스킵하고 어제로 이동
//                // (이 로직은 사실상 uniqueDates.contains(currentDate)에서 처리됨)
//            } else {
//                // 연속성이 끊어지면 종료
//                break
//            }
//            currentDate = currentDate.minusDays(1) // 이전 날짜로 이동
//        }
//
//        return currentStreak
//    }
//
//    /**
//     * UserStatsPrefs에서 Leaf 및 Grape 카운트를 가져와 UI에 표시하는 함수
//     */
//    private fun loadAndDisplayCounts() {
//        // 1. Leaf Count 로드
//        val leafCount = userStatsPrefs.getLeafCount()
//        // 2. Grape Count 로드
//        val grapeCount = userStatsPrefs.getGrapeCount()
//
//        // 3. UI에 반영
//        // ActivitySettingsMainBinding.xml 레이아웃에 각각의 TextView ID가 있다고 가정합니다.
//        binding.leaves.text = " \uD83C\uDF43 leaves: "+leafCount.toString()
//        binding.grapes.text = " \uD83C\uDF47 Grapes: "+grapeCount.toString()
//    }

    /**
     * SharedPreferences에서 닉네임을 가져와 UI에 표시하는 함수
     */
    private fun loadAndDisplayNickname() {
        // 4. 저장된 닉네임 값을 가져옵니다.
        val nickname = userStatsPrefs.getNickname()

        // 5. 가져온 닉네임을 UI의 TextView(userNickname)에 설정합니다.
        // ActivitySettingsMainBinding.xml 레이아웃에 userBlock 내부에
        // userNicknameTextView와 같은 ID를 가진 TextView가 있다고 가정합니다.
        binding.nickname.text = nickname
    }

    private fun setNavigationBtnListener() = with(binding.navigationBar) {
        btnCalendar.setOnClickListener {
            val intent = Intent(this@SettingsActivity, CalendarActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val intent = Intent(this@SettingsActivity, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        btnInfo.setOnClickListener {

        }
    }

    private fun setSettingsBtnListener() = with(binding) {

        binding.notificationBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, NotificationSettingsActivity::class.java))
        }

        binding.diaryStyleBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, DiaryStyleSettingsActivity::class.java))
        }

        binding.accountBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, AccountSettingsActivity::class.java))
        }
    }
}