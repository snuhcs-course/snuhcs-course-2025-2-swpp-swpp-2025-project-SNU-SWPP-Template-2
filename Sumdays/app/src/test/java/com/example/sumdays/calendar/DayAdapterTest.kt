package com.example.sumdays.calendar

import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.CalendarActivity
import com.example.sumdays.DailyReadActivity
import com.example.sumdays.DailyWriteActivity
import com.example.sumdays.R
import com.example.sumdays.TestApplication
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE],
    application = TestApplication::class)
class DayAdapterTest {

    private lateinit var activity: CalendarActivity
    private lateinit var recyclerView: RecyclerView
    private lateinit var root: FrameLayout

    private val baseToday: LocalDate = LocalDate.of(2025, 1, 15)
    private val baseYearMonth: YearMonth = YearMonth.from(baseToday)

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(CalendarActivity::class.java).setup().get()
        recyclerView = RecyclerView(activity).apply {
            layoutManager = GridLayoutManager(activity, 7)
        }
        root = FrameLayout(activity).apply {
            addView(recyclerView)
        }
        val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.AT_MOST)
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, 1080, 1920)
    }

    @Test
    fun bind_invalidDay_disablesCell() {
        val cells = listOf(DateCell(day = 0, dateString = "", isCurrentMonth = true))
        val adapter = DayAdapter(cells, activity, today = baseToday, maxYearMonth = baseYearMonth)
        recyclerView.adapter = adapter

        recyclerView.measure(0, 0)
        recyclerView.layout(0, 0, 1080, 200)

        val vh = recyclerView.findViewHolderForAdapterPosition(0) as DayAdapter.DayViewHolder
        val tvDayNumber = vh.itemView.findViewById<TextView>(R.id.tv_day_number)
        val tvEmoji = vh.itemView.findViewById<TextView>(R.id.tv_emoji)

        assertThat(tvDayNumber.text.toString(), `is`(""))
        assertThat(tvEmoji.visibility, `is`(View.GONE))
        assertThat(vh.itemView.isClickable, `is`(false))
    }

    @Test
    fun bind_pastDay_withDiary_clickStartsDailyReadActivity() {
        val pastDate = baseToday.minusDays(1)
        val dateString = pastDate.toString()

        val field = CalendarActivity::class.java.getDeclaredField("currentStatusMap")
        field.isAccessible = true
        field.set(activity, mapOf(dateString to Pair(true, "🙂")))

        val cells = listOf(
            DateCell(
                day = pastDate.dayOfMonth,
                dateString = dateString,
                isCurrentMonth = true
            )
        )
        val adapter = DayAdapter(cells, activity, today = baseToday, maxYearMonth = baseYearMonth)
        recyclerView.adapter = adapter

        recyclerView.measure(0, 0)
        recyclerView.layout(0, 0, 1080, 200)

        val vh = recyclerView.findViewHolderForAdapterPosition(0) as DayAdapter.DayViewHolder
        val shadowActivity = Shadows.shadowOf(activity)
        shadowActivity.clearNextStartedActivities()

        vh.itemView.performClick()

        val startedIntent = shadowActivity.nextStartedActivity
        assertThat(startedIntent.component?.className, `is`(DailyReadActivity::class.java.name))
        assertThat(startedIntent.getStringExtra("date"), `is`(dateString))
    }

    @Test
    fun bind_pastDay_withoutDiary_clickStartsDailyWriteActivity() {
        val pastDate = baseToday.minusDays(2)
        val dateString = pastDate.toString()

        val field = CalendarActivity::class.java.getDeclaredField("currentStatusMap")
        field.isAccessible = true
        field.set(activity, mapOf(dateString to Pair(false, null)))

        val cells = listOf(
            DateCell(
                day = pastDate.dayOfMonth,
                dateString = dateString,
                isCurrentMonth = true
            )
        )
        val adapter = DayAdapter(cells, activity, today = baseToday, maxYearMonth = baseYearMonth)
        recyclerView.adapter = adapter

        recyclerView.measure(0, 0)
        recyclerView.layout(0, 0, 1080, 200)

        val vh = recyclerView.findViewHolderForAdapterPosition(0) as DayAdapter.DayViewHolder
        val shadowActivity = Shadows.shadowOf(activity)
        shadowActivity.clearNextStartedActivities()

        vh.itemView.performClick()

        val startedIntent = shadowActivity.nextStartedActivity
        assertThat(startedIntent.component?.className, `is`(DailyWriteActivity::class.java.name))
        assertThat(startedIntent.getStringExtra("date"), `is`(dateString))
    }

    @Test
    fun bind_nonCurrentMonth_makesCellDimmed() {
        val date = baseToday.minusDays(3)
        val dateString = date.toString()

        val cells = listOf(
            DateCell(
                day = date.dayOfMonth,
                dateString = dateString,
                isCurrentMonth = false
            )
        )
        val adapter = DayAdapter(cells, activity, today = baseToday, maxYearMonth = baseYearMonth)
        recyclerView.adapter = adapter

        recyclerView.measure(0, 0)
        recyclerView.layout(0, 0, 1080, 200)

        val vh = recyclerView.findViewHolderForAdapterPosition(0) as DayAdapter.DayViewHolder
        val tvDayNumber = vh.itemView.findViewById<TextView>(R.id.tv_day_number)
        val tvEmoji = vh.itemView.findViewById<TextView>(R.id.tv_emoji)
        val tvCircle = vh.itemView.findViewById<TextView>(R.id.tv_circle)

        assertThat(tvEmoji.visibility, `is`(View.GONE))
        assertThat(tvDayNumber.alpha, `is`(0.4f))
        assertThat(tvCircle.alpha, `is`(0.4f))
        assertThat(tvDayNumber.currentTextColor, `is`(Color.GRAY))
    }
}
