// CalendarActivity.kt
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

    private val viewModel: CalendarViewModel by viewModels()
    var currentStatusMap: Map<String, Pair<Boolean, String?>> = emptyMap()
    private var currentMonthLiveData: LiveData<Map<String, Pair<Boolean, String?>>>? = null

    // 캘린더 언어 설정
    private var currentLanguage: CalendarLanguage = CalendarLanguage.KOREAN

    // ★ 오늘/이번달 기준 값을 한 번만 계산해서 재사용
    private val today: LocalDate by lazy { LocalDate.now() }
    private val currentYearMonth: YearMonth by lazy { YearMonth.from(today) }

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
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setNavigationBar() {
        val btnCalendar = findViewById<android.widget.ImageButton>(R.id.btnCalendar)
        val btnDaily = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnDaily)
        val btnInfo = findViewById<android.widget.ImageButton>(R.id.btnInfo)

        btnCalendar.setOnClickListener { /* 현재 화면 */ }
        btnDaily.setOnClickListener {
            val todayStr = today.toString()
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", todayStr)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        btnInfo.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    /** setCustomCalendar: 달력의 스크롤, 버튼에 의한 달 전환과 현재 월을 표시 */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setCustomCalendar() {
        // ★ MonthAdapter에 최대 허용 월/날짜 전달 (미래 차단 & 셀 흐리기 용)
        monthAdapter = MonthAdapter(activity = this)
        calendarViewPager.adapter = monthAdapter

        // 0. 언어에 따른 헤더 설정
        val headerLayout = findViewById<LinearLayout>(R.id.day_of_week_header)
        headerLayout.removeAllViews()

        val dayNamesKOR = listOf("일", "월", "화", "수", "목", "금", "토")
        val dayNamesENG = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        val dayNames = if (currentLanguage == CalendarLanguage.KOREAN) dayNamesKOR else dayNamesENG

        for (dayName in dayNames) {
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                gravity = Gravity.CENTER
                text = dayName
                setTextColor(
                    when (dayName) {
                        "일", "SUN" -> ContextCompat.getColor(this@CalendarActivity, android.R.color.holo_red_dark)
                        "토", "SAT" -> ContextCompat.getColor(this@CalendarActivity, android.R.color.holo_blue_dark)
                        else -> Color.WHITE
                    }
                )
            }
            headerLayout.addView(tv)
        }

        // 1. Scroll로 달 전환: 중앙에서 시작
        val startPosition = Int.MAX_VALUE / 2
        calendarViewPager.setCurrentItem(startPosition, false)

        calendarViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateMonthYearTitle(position)
                observeMonthlyData(position) // 월 변경 시 데이터 구독 갱신
            }
        })

        // 2. 이전/다음 달 버튼으로 달 전환 (미래 달 진입 방지)
        btnPrevMonth.setOnClickListener {
            val currentItem = calendarViewPager.currentItem
            calendarViewPager.setCurrentItem(currentItem - 1, true)
        }
        btnNextMonth.setOnClickListener {
            val currentItem = calendarViewPager.currentItem
            val nextPos = currentItem + 1
            calendarViewPager.setCurrentItem(nextPos, true)
        }

        // 현재 월 표시 & 데이터 구독
        updateMonthYearTitle(startPosition)
        observeMonthlyData(startPosition)
    }

    /** 특정 position(월)에 해당하는 데이터를 구독 */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeMonthlyData(position: Int) {
        val targetMonth = getTargetMonthForPosition(position)
        val fromDate = targetMonth.atDay(1).toString()
        val toDate = targetMonth.atEndOfMonth().toString()

        currentMonthLiveData?.removeObservers(this)
        currentMonthLiveData = viewModel.getMonthlyEmojis(fromDate, toDate)
        currentMonthLiveData?.observe(this) { map ->
            currentStatusMap = map
            monthAdapter.notifyItemChanged(calendarViewPager.currentItem)
        }
    }

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