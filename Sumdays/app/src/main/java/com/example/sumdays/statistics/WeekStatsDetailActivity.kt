package com.example.sumdays.statistics

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.CalendarActivity
import com.example.sumdays.DailyWriteActivity
import com.example.sumdays.R
import com.example.sumdays.SettingsActivity
import com.example.sumdays.databinding.ActivityWeekStatsDetailBinding // View Binding 사용 가정
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.LocalDate
import java.util.Locale

// 주간 통계 상세 분석 화면
class WeekStatsDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeekStatsDetailBinding
    private lateinit var weekSummary: WeekSummary

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // View Binding 초기화
        binding = ActivityWeekStatsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Intent에서 데이터 받기
        weekSummary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("week_summary", WeekSummary::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<WeekSummary>("week_summary")!!
        }

        setupViews()
        setupListeners()
    }

    private fun setupViews() {

        // 주간 제목 설정
        binding.weekTitleTextView.text = weekSummary.summary.title
        // ⭐ 문제 1 해결: 날짜 범위 표시
        binding.weekRangeTextView.text =
            "${weekSummary.startDate} ~ ${weekSummary.endDate} | ${weekSummary.summary.emergingTopics.joinToString(", ")}"

        // ⭐ 문제 2 해결: 일기 작성 횟수 표시
        val count = weekSummary.diaryCount
        val maxCount = 7
        binding.diaryCountRatio.text = "7일 중 ${count}일 일기를 썼어요!"
        binding.diaryCountProgress.max = maxCount
        binding.diaryCountProgress.progress = count

        // 2. 섹션별 데이터 바인딩 및 시각화

        // 2.1. 요약 개요 섹션
        binding.overviewTextView.text = weekSummary.summary.overview

        // 2.2. 감정 분석 섹션
        displayEmotionAnalysis(binding.emotionAnalysisBarChart, weekSummary.emotionAnalysis)
        binding.dominantEmojiTextView.text = "이번 주 감정을 이모지로 나타내면 : ${weekSummary.emotionAnalysis.dominantEmoji}"

        // 감정 점수 로직 수정
        val score = weekSummary.emotionAnalysis.emotionScore?.toDouble() ?: 0.0 // 점수 가져오기 (기본값 0.0)

        // 1. 온도계 아이콘 설정
        val thermometerResId = when {
            score > 0.5 -> R.drawable.ic_thermometer_high       // ( 0.5 ~  1.0] : 빨간색
            score > 0.0 -> R.drawable.ic_thermometer_medium     // ( 0.0 ~  0.5] : 주황색
            score > -0.5 -> R.drawable.ic_thermometer_low       // (-0.5 ~  0.0] : 하늘색
            else -> R.drawable.ic_thermometer_very_low          // [-1.0 ~ -0.5] : 파란색
        }
        binding.thermometerIcon.setImageResource(thermometerResId)

        // 2. 온도 텍스트 설정 (score * 100)
        val temperature = score * 100
        val temptext = String.format(Locale.getDefault(), "%.0f°C", temperature)
        binding.emotionScore.text = "감정 온도: ${temptext}"
        binding.emotionScore.visibility = View.VISIBLE // GONE이었던 것을 보이도록


        // ⭐ 추세 (Trend) 데이터 바인딩 추가
        val trendValue = when (weekSummary.emotionAnalysis.trend) {
            "increasing" -> "상승세 📈"
            "decreasing" -> "하락세 📉"
            else -> "안정적"
        }
        binding.emotionTrendTextView.text = "감정 추세: $trendValue"

        // 2.4. 통찰/조언 섹션
        binding.adviceTextView.text = weekSummary.insights.advice
        binding.emotionCycleTextView.text = weekSummary.insights.emotionCycle

    }

    // 막대 그래프(Bar Chart)를 이용한 감정 분포 시각화
    private fun displayEmotionAnalysis(barChart: BarChart, analysis: EmotionAnalysis) {
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
        dataSet.color = getColor(R.color.violet_dark) // 프로젝트 색상 사용 가정

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupListeners() {
        // include_nav_daily 레이아웃에서 뷰들을 찾습니다.
        // 뒤로가기 버튼
        binding.backButton.setOnClickListener {
            finish()
        }
    }
}