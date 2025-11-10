package com.example.sumdays.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.CalendarActivity
import com.example.sumdays.R
import org.threeten.bp.YearMonth

class MonthAdapter(private val activity: CalendarActivity) :
        RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

    // 무한 스크롤을 위해 다룰 달의 개수를 Int.MAX_VALUE로 설정
    override fun getItemCount(): Int = Int.MAX_VALUE

    private val START_POSITION = Int.MAX_VALUE / 2
    private val baseYearMonth = YearMonth.now()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month_grid, parent, false)

        return MonthViewHolder(view)
    }

    /** 스크롤 위치를 바탕으로 그 달의 정보를 캘린더와 연결 */
    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        val monthDiff = position - START_POSITION
        val targetMonth = baseYearMonth.plusMonths(monthDiff.toLong())
        val monthData = getMonthData(targetMonth.year, targetMonth.monthValue)

        holder.bind(monthData, activity)
    }

    class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayGrid: RecyclerView = itemView.findViewById(R.id.rv_day_grid)

        fun bind(monthData: MonthData, activity: CalendarActivity) {
            // 날짜 그리드 7열로 설정
            dayGrid.layoutManager = GridLayoutManager(itemView.context, 7)

            // 이 월의 날짜를 표시할 DayAdapter를 연결
            val dayAdapter = DayAdapter(monthData.days, activity)
            dayGrid.adapter = dayAdapter
        }
    }
}