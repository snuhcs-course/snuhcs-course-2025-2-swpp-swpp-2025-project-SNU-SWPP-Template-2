package com.example.sumdays.calendar

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.CalendarActivity
import com.example.sumdays.DailyReadActivity
import com.example.sumdays.R
import org.threeten.bp.LocalDate
import org.threeten.bp.DayOfWeek

class DayAdapter(private val days: List<DateCell>, private val activity: CalendarActivity) :
    RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    override fun getItemCount(): Int = days.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_cell, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val cell = days[position]
        holder.bind(cell, activity)
    }

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCircle: TextView = itemView.findViewById(R.id.tv_circle)
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tv_day_number)
        private val tvEmoji: TextView = itemView.findViewById(R.id.tv_emoji)

        fun bind(cell: DateCell, activity: CalendarActivity) {
            // 날짜 계산이 잘못된 경우
            if (cell.day <= 0 || cell.day > 31) {
                tvCircle.background = null
                tvDayNumber.text = ""
                tvEmoji.visibility = View.GONE
                itemView.isClickable = false
                return // 여기서 함수 종료
            }

            tvDayNumber.text = cell.day.toString()
            itemView.isClickable = true

            // 요일에 따른 색 설정(일요일 빨강, 토요일 파랑)
            val dayOfWeek = LocalDate.parse(cell.dateString).dayOfWeek
            val textColor = when {
                dayOfWeek == DayOfWeek.SUNDAY -> Color.RED
                dayOfWeek == DayOfWeek.SATURDAY -> Color.parseColor("#0095FF") // blue
                else -> Color.WHITE
            }
            tvDayNumber.setTextColor(textColor)

            // 오늘 날짜 스타일 설정 (var currentStatusMap: Map<String, Pair<Boolean, String?>> = emptyMap())
            val date = cell.dateString
            val isToday = cell.dateString == LocalDate.now().toString()
            val hasDiary = activity.currentStatusMap[date]?.first ?: false
            val emoji = activity.currentStatusMap[date]?.second ?: null
            if (isToday) {
                tvCircle.background = ContextCompat.getDrawable(itemView.context, R.drawable.shape_fox_today)
                tvDayNumber.setTypeface(null, Typeface.BOLD) // 오늘은 볼드체로
            }
            else if (hasDiary)
            {
                tvCircle.background = ContextCompat.getDrawable(itemView.context, R.drawable.shape_fox_date_gray_completed)
                tvDayNumber.setTypeface(null, Typeface.NORMAL)
            }
            else{
                tvCircle.background = ContextCompat.getDrawable(itemView.context, R.drawable.shape_fox_date_gray)
                tvDayNumber.setTypeface(null, Typeface.NORMAL)
            }

            // 이모지 있을 때 스타일 설정
            if (emoji != null) { // TODO: 이모지가 덮어쓰는 느낌이 안 나고 흐리게 보임
                tvEmoji.text = emoji
                tvEmoji.visibility = View.VISIBLE
            } else {
                tvEmoji.visibility = View.GONE
            }

            // 이전/다음 달 날짜 스타일 설정(흐리게 표시)
            if (!cell.isCurrentMonth) {
                tvEmoji.visibility = View.GONE
                tvDayNumber.alpha = 0.4f
                tvCircle.alpha = 0.4f
                tvDayNumber.setTextColor(Color.GRAY) // TODO: 일단 주말도 회색임. 수정 필요
            } else {
                tvDayNumber.alpha = 1.0f
                tvCircle.alpha = 1.0f
            }

            // 날짜 클릭 이벤트 설정
            if (cell.dateString.isNotEmpty()) {
                itemView.setOnClickListener {
                    val intent = Intent(activity, DailyReadActivity::class.java)
                    intent.putExtra("date", cell.dateString)
                    activity.startActivity(intent)
                }
            }
        }
    }
}