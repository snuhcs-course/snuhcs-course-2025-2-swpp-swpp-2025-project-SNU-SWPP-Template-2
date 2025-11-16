package com.example.sumdays.data.converter

import androidx.room.TypeConverter
import com.example.sumdays.statistics.WeekSummary
import com.google.gson.Gson

class WeekSummaryConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromWeekSummary(value: WeekSummary): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWeekSummary(value: String): WeekSummary {
        return gson.fromJson(value, WeekSummary::class.java)
    }
}
