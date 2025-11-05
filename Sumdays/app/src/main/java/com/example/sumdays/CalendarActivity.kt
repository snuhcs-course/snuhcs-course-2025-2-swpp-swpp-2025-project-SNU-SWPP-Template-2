package com.example.sumdays

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageButton
import android.widget.TextView
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale
import androidx.viewpager2.widget.ViewPager2
import com.example.sumdays.calendar.CalendarLanguage
import com.example.sumdays.calendar.MonthAdapter
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import com.jakewharton.threetenabp.AndroidThreeTen
import com.example.sumdays.data.viewModel.CalendarViewModel
import androidx.activity.viewModels
import androidx.lifecycle.LiveData

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarViewPager: ViewPager2
    private lateinit var tvMonthYear: TextView
    private lateinit var monthAdapter: MonthAdapter
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    // ▼▼▼▼▼ 2. '구독 연결선' 역할을 할 LiveData 변수 추가 ▼▼▼▼▼
    private val viewModel: CalendarViewModel by viewModels()
    var currentStatusMap: Map<String, Pair<Boolean, String?>> = emptyMap()
    private var currentMonthLiveData: LiveData<Map<String, Pair<Boolean, String?>>>? = null
    // ▲▲▲▲▲ 2. 끝 ▲▲▲▲▲

    // 캘린더 언어 설정
    private var currentLanguage: CalendarLanguage = CalendarLanguage.ENGLISH


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        AndroidThreeTen.init(this)
        calendarViewPager = findViewById(R.id.calendarViewPager)
        tvMonthYear = findViewById(R.id.tv_month_year)
        btnPrevMonth = findViewById(R.id.btn_prev_month)
        btnNextMonth = findViewById(R.id.btn_next_month)
        setCustomCalendar()
        setStatisticBtnListener()
        setNavigationBar()
    }

    private fun setStatisticBtnListener() {
        val btnStats = findViewById<ImageButton>(R.id.statistic_btn)

        btnStats.setOnClickListener {
            // ⭐ 통계 화면(StatisticsActivity)으로 이동하는 Intent 추가
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setNavigationBar() {
        val btnCalendar = findViewById<android.widget.ImageButton>(R.id.btnCalendar)
        val btnDaily = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnDaily)
        val btnInfo = findViewById<android.widget.ImageButton>(R.id.btnInfo)

        btnCalendar.setOnClickListener {
            // 이미 캘린더 화면 → 아무 일도 안 함
        }
        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
        }
        btnInfo.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    /** setCustomCalendar: 달력의 스크롤, 버튼에 의한 달 전환과 현재 월을 표시하는 기능을 수행 */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setCustomCalendar() {
        monthAdapter = MonthAdapter(this)
        calendarViewPager.adapter = monthAdapter

        // 0. 언어에 따른 헤더 설정
        val headerLayout = findViewById<LinearLayout>(R.id.day_of_week_header)
        headerLayout.removeAllViews()

        val dayNamesKOR = listOf("일", "월", "화", "수", "목", "금", "토")
        val dayNamesENG = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

        val dayNames = if (currentLanguage == CalendarLanguage.KOREAN) dayNamesKOR else dayNamesENG

        for (dayName in dayNames) {
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
                )
                gravity = Gravity.CENTER
                text = dayName

                // 주말 색상 적용
                setTextColor(
                    if (dayName == "일" || dayName == "SUN") {
                        ContextCompat.getColor(this@CalendarActivity, android.R.color.holo_red_dark)
                    } else if (dayName == "토" || dayName == "SAT") {
                        ContextCompat.getColor(this@CalendarActivity, android.R.color.holo_blue_dark)
                    } else {
                        Color.WHITE
                    }
                )
            }
            headerLayout.addView(tv)
        }

        // 1. Scroll로 달 전환
            // ViewPager2의 무한 스크롤 시작 위치를 중앙으로 설정 (START_POSITION = Int.MAX_VALUE / 2)
        val startPosition = Int.MAX_VALUE / 2
        calendarViewPager.setCurrentItem(startPosition, false)
            // ViewPager2 페이지 전환시 달/년도 업데이트 되도록 함수 연결
        calendarViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateMonthYearTitle(position)
                // ▼▼▼▼▼ 4. [트리거 1] 달이 바뀌면 데이터 구독 함수 호출 ▼▼▼▼▼
                observeMonthlyData(position)
                // ▲▲▲▲▲ 4. 끝 ▲▲▲▲▲
            }
        })

        // 2. 이전/다음 달 버튼으로 달 전환
        btnPrevMonth.setOnClickListener {
            val currentItem = calendarViewPager.currentItem
            calendarViewPager.setCurrentItem(currentItem - 1, true)
        }
        btnNextMonth.setOnClickListener {
            val currentItem = calendarViewPager.currentItem
            calendarViewPager.setCurrentItem(currentItem + 1, true)
        }

        // 현재 월 표시
        updateMonthYearTitle(startPosition)

        // ▼▼▼▼▼ 5. [트리거 2] 앱 실행 시 첫 화면의 데이터 구독 ▼▼▼▼▼
        observeMonthlyData(startPosition)
        // ▲▲▲▲▲ 5. 끝 ▲▲▲▲▲
    }

    // ▼▼▼▼▼ 6. DailyReadActivity의 observeEntry()와 동일한 패턴의 함수 (새로 추가) ▼▼▼▼▼
    /**
     * 특정 position(월)에 해당하는 데이터를 구독(observe)합니다.
     * DailyReadActivity.observeEntry()와 동일한 로직을 수행합니다.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeMonthlyData(position: Int) {
        // 1. 날짜 키(fromDate, toDate) 계산
        val targetMonth = getTargetMonthForPosition(position)
        val fromDate = targetMonth.atDay(1).toString()
        val toDate = targetMonth.atEndOfMonth().toString()

        // 2. (핵심) 기존 옵저버 해제
        currentMonthLiveData?.removeObservers(this)

        // 3. (핵심) ViewModel에서 새로운 LiveData 받아오기
        currentMonthLiveData = viewModel.getMonthlyEmojis(fromDate, toDate)

        // 4. (핵심) 새로운 LiveData 구독 시작
        currentMonthLiveData?.observe(this) { map ->
            // LiveData가 (DB 변경 혹은 월 변경으로) 업데이트되면 실행됨

            // 5. Activity의 데이터 변수 업데이트
            currentStatusMap = map

            // 6. 어댑터에 갱신 알림 (현재 보이는 페이지만 갱신)
            monthAdapter.notifyItemChanged(calendarViewPager.currentItem)
        }
    }
    // ▲▲▲▲▲ 6. 끝 ▲▲▲▲▲
    /** 입력 받은 position 위치의 달/년도 계산하여 반환함 */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateMonthYearTitle(position: Int) {
        val targetMonth = getTargetMonthForPosition(position)

        val (pattern, locale) = when (currentLanguage) {
            CalendarLanguage.KOREAN -> Pair(R.string.month_year_format, Locale.KOREAN)
            CalendarLanguage.ENGLISH -> Pair(R.string.month_year_format_english, Locale.US)
        }

        val formatter = DateTimeFormatter.ofPattern(getString(pattern), locale)
        tvMonthYear.text = targetMonth.format(formatter)
    }

    private fun getTargetMonthForPosition(position: Int): YearMonth {
        val baseYearMonth = YearMonth.now()
        val startPosition = Int.MAX_VALUE / 2
        val monthDiff = position - startPosition
        return baseYearMonth.plusMonths(monthDiff.toLong())
    }
}
