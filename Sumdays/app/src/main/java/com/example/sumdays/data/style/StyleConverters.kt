package com.example.sumdays.data.style

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StyleConverters {
    private val gson = Gson()

    //  List<Float> (Style Vector) 변환
    @TypeConverter
    fun fromFloatList(list: List<Float>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toFloatList(data: String?): List<Float>? {
        val type = object : TypeToken<List<Float>>() {}.type
        return gson.fromJson(data, type)
    }

    //  List<String> (styleExamples 및 StylePrompt 내부 List) 변환
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(data: String?): List<String>? {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(data, type)
    }

    // StylePrompt 객체 변환
    @TypeConverter
    fun fromStylePrompt(prompt: StylePrompt?): String? {
        return gson.toJson(prompt)
    }

    @TypeConverter
    fun toStylePrompt(data: String?): StylePrompt? {
        return gson.fromJson(data, StylePrompt::class.java)
    }
}