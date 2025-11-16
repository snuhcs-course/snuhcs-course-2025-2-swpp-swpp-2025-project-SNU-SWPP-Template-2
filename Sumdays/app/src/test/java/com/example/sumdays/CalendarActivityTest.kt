package com.example.sumdays.data.dao

import androidx.room.Room
import com.example.sumdays.data.*
import com.example.sumdays.statistics.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.content.Context

@RunWith(RobolectricTestRunner::class) // 👈 추가됨 (필수)
@Config(sdk = [33])
class DaoIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var dailyDao: DailyEntryDao
    private lateinit var weekDao: WeekSummaryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        dailyDao = db.dailyEntryDao()
        weekDao = db.weekSummaryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---------------------------
    // ① DailyEntryDao 테스트
    // ---------------------------
    @Test
    fun test_dailyEntryDao_allFunctions() = runBlocking {
        val date = "2025-01-01"

        // insert/update
        dailyDao.updateEntry(date, diary = "hello", themeIcon = "icon1")

        val value1 = dailyDao.getEntry(date).first()
        assertThat(value1?.diary, `is`("hello"))

        // update again
        dailyDao.updateEntry(date, diary = "changed")
        val value2 = dailyDao.getEntry(date).first()
        assertThat(value2?.diary, `is`("changed"))

        // monthly emojis
        val list = dailyDao.getMonthlyEmojis("2025-01-01", "2025-01-31").first()
        assertThat(list.size, `is`(1))
        assertThat(list[0].themeIcon, `is`("icon1"))

        // delete
        dailyDao.deleteEntry(date)
        val afterDelete = dailyDao.getEntry(date).first()
        assertThat(afterDelete, nullValue())
    }

    // ---------------------------
    // ② WeekSummaryDao 테스트
    // ---------------------------
    @Test
    fun test_weekSummaryDao_allFunctions() = runBlocking {

        val summary = WeekSummary(
            startDate = "2025-10-13",
            endDate = "2025-10-19",
            diaryCount = 5,
            emotionAnalysis = EmotionAnalysis(
                distribution = mapOf("positive" to 3, "neutral" to 1, "negative" to 1),
                dominantEmoji = "😊",
                emotionScore = 0.8f
            ),
            highlights = listOf(
                Highlight("2025-10-15", "좋은 하루였다")
            ),
            insights = Insights("충분한 휴식 필요", "감정 기복 있음"),
            summary = SummaryDetails(
                emergingTopics = listOf("공부", "운동"),
                overview = "전반적으로 긍정적인 주",
                title = "긍정적인 한 주"
            )
        )

        // upsert
        weekDao.upsert(summary)

        val loaded = weekDao.getWeekSummary("2025-10-13")
        assertThat(loaded?.diaryCount, `is`(5))

        // getAllDatesAsc
        val dates = weekDao.getAllDatesAsc()
        assertThat(dates.size, `is`(1))
        assertThat(dates[0], `is`("2025-10-13"))

        // clear
        weekDao.clearAll()
        val afterClear = weekDao.getAllDatesAsc()

        assertThat(afterClear, `is`(emptyList<String>()))

    }
}