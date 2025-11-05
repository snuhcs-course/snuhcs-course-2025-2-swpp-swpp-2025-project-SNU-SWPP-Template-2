package com.example.sumdays.statistics

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.CalendarActivity
import com.example.sumdays.DailyWriteActivity
import com.example.sumdays.R
import com.example.sumdays.SettingsActivity
import com.example.sumdays.databinding.ActivityMonthStatsDetailBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.LocalDate

// ... 필요한 차트 라이브러리 임포트 ...

// 월간 통계 상세 분석 화면
class MonthStatsDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonthStatsDetailBinding
    private lateinit var monthSummary: MonthSummary

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthStatsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Intent에서 데이터 받기
        monthSummary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("month_summary", MonthSummary::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<MonthSummary>("month_summary")!!
        }

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        // 월간 제목 설정 (예: 9월은 개발 기반을 다진 달)
        binding.monthTitleTextView.text = monthSummary.summary.title

        // ⭐ 날짜 범위 및 카운트 표시
        binding.monthRangeTextView.text =
            "${monthSummary.startDate} ~ ${monthSummary.endDate} | ${monthSummary.summary.emergingTopics.joinToString(", ")}"

        // ⭐ 월간 일기 작성 횟수 표시
        val daysInMonth = 30 // 9월 기준. 실제로는 Calendar나 Period를 사용해야 함
        val count = monthSummary.diaryCount
        binding.diaryCountRatio.text = "$count/$daysInMonth"
        binding.diaryCountProgress.max = daysInMonth
        binding.diaryCountProgress.progress = count


        // 2. 섹션별 데이터 바인딩 및 시각화 (WeekStatsDetailActivity와 유사하게 구성)

        // 1. 월간 개요 섹션
        binding.overviewTextView.text = monthSummary.summary.overview

        // 2. 분석 섹션 (막대 그래프 + 라인 그래프)
        // 월 전체 감정 분포 (Bar Chart)
        displayBarChart(binding.emotionAnalysisBarChart, monthSummary.emotionAnalysis)

        // 주간별 감정 변화 추이 (Line Chart)
        displayWeeklyEmotionTrend(binding.weeklyEmotionLineChart, monthSummary.weeksForMonth)

        // 3. 주간 요약 목록 (Weeks List)
        binding.weekSummariesList.text = monthSummary.weeksForMonth
            .joinToString("\n\n") {
                // ⭐ overview 내용을 추가하여 주간 요약 상세 내용을 표시
                "(${it.dateRange}) ${it.title} - ${it.dominantEmoji}\n" +
                        "키워드: ${it.topics.joinToString()}\n" +
                        "개요: ${it.summary}" // <--- 이 부분이 추가되었습니다.
            }

        // 4. 통찰/조언 섹션
        binding.adviceTextView.text = monthSummary.insights.advice
        binding.emotionCycleTextView.text = monthSummary.insights.emotionCycle
    }
    private fun displayBarChart(barChart: BarChart, analysis: EmotionAnalysis) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        val distribution = analysis.distribution
        val keys = distribution.keys.sorted() // positive, neutral, negative 순으로 정렬

        keys.forEachIndexed { index, key ->
            val value = distribution[key]?.toFloat() ?: 0f
            entries.add(BarEntry(index.toFloat(), value))

            // X축 라벨 설정
            val label = when(key) {
                "positive" -> "긍정"
                "neutral" -> "중립"
                "negative" -> "부정"
                else -> key
            }
            labels.add(label)
        }

        val dataSet = BarDataSet(entries, "감정 분포")
        dataSet.color = getColor(R.color.colorPrimary) // 프로젝트 색상 사용 가정

        val barData = BarData(dataSet)
        barChart.data = barData

        // X축 설정
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        barChart.xAxis.axisMinimum = -0.5f // 첫 번째 바의 절반 너치만큼 왼쪽으로 이동
        barChart.xAxis.axisMaximum = labels.size.toFloat() - 0.5f // 마지막 바의 절반 너치만큼 오른쪽으로 이동
        // ⭐ setGranularity(1f)는 중복 라벨 출력을 방지하여 정렬에 도움을 줍니다.
        xAxis.granularity = 1f

        // 기타 설정
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setFitBars(true)
        barChart.invalidate() // 그래프 갱신
    }

    // ⭐⭐ 라인 그래프 구현 함수 추가 (주간 평균 감정 추이)
    private fun displayWeeklyEmotionTrend(lineChart: LineChart, weeks: List<WeekSummaryForMonth>) {
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        weeks.forEachIndexed { index, week ->
            // 평균 감정 점수를 Y값으로 사용
            entries.add(Entry(index.toFloat(), week.emotionScore))

            // X축 라벨은 주차 정보로 설정
            labels.add(week.dateRange)

        }

        val dataSet = LineDataSet(entries, "주간 감정 변화")
        dataSet.color = getColor(R.color.colorPrimary)
        dataSet.setDrawCircles(true)

        // X축 설정
        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f

        lineChart.data = LineData(dataSet)
        lineChart.description.isEnabled = false
        lineChart.invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupListeners() {
        // include_nav_daily 레이아웃에서 뷰들을 찾습니다.
        // 뒤로가기 버튼
        binding.backButton.setOnClickListener {
            finish()
        }
        val btnCalendar = findViewById<ImageButton>(R.id.btnCalendar)
        val btnDaily = findViewById<ImageButton>(R.id.btnDaily)
        val btnInfo = findViewById<ImageButton>(R.id.btnInfo)

        // Calendar로 이동
        btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
            finish()
        }

        // DailyWrite (오늘의 일기)로 이동
        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val intent = Intent(this, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
            finish()
        }

        // Info 화면 (정보/설정)
        btnInfo.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}