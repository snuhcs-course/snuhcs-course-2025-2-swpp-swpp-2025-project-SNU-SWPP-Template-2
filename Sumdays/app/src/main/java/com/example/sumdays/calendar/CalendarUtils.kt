package com.example.sumdays.calendar

import org.threeten.bp.YearMonth

fun getMonthData(year: Int, month: Int): MonthData {
    val targetMonth = YearMonth.of(year, month)
    val firstDayOfMonth = targetMonth.atDay(1)

    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

    val daysList = mutableListOf<DateCell>()

    var previousMonthDate = firstDayOfMonth.minusDays(startDayOfWeek.toLong())
    for (i in 0 until startDayOfWeek) {
        daysList.add(
            DateCell(
                previousMonthDate.dayOfMonth,
                false,
                previousMonthDate.toString()
            )
        )
        previousMonthDate = previousMonthDate.plusDays(1)
    }

    var currentDay = firstDayOfMonth
    while (currentDay.month == targetMonth.month) {
        daysList.add(
            DateCell(
                currentDay.dayOfMonth,
                true,
                currentDay.toString()
            )
        )
        currentDay = currentDay.plusDays(1)
    }

    var nextMonthDate = currentDay
    val currentMonthDays = daysList.size
    val totalCells = if (currentMonthDays > 35) 42 else 35 // 다음 달이 한 줄 이상 나오는 것을 방지
    val remainingCells = totalCells - daysList.size
    for (i in 0 until remainingCells) {
        daysList.add(
            DateCell(
                nextMonthDate.dayOfMonth,
                false,
                nextMonthDate.toString()
            )
        )
        nextMonthDate = nextMonthDate.plusDays(1)
    }

    return MonthData(year, month, daysList)
}