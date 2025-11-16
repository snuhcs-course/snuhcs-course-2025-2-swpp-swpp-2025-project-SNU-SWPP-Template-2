package com.example.sumdays.daily.memo

import android.util.Log
import com.example.sumdays.data.style.StylePrompt
import com.google.gson.Gson
import com.google.gson.JsonObject

object MemoMergeUtils {
    /** StylePrompt 객체를 서버 요청 형식(Map<String, Any>)으로 변환 */
    fun convertStylePromptToMap(prompt: StylePrompt): Map<String, Any> {
        // Gson을 사용하여 객체를 Map으로 변환하는 것이 가장 안전하고 빠릅니다.
        // Gson 라이브러리 사용 가정
        val gson = Gson()
        // Map<String, Any> 타입 토큰 정의
        val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type

        // StylePrompt 객체를 JSON 문자열로 변환 후, 다시 Map으로 변환
        val jsonString = gson.toJson(prompt)
        return gson.fromJson(jsonString, type)
    }

    /** extract merged text from json file */
    fun extractMergedText(json: JsonObject): String {
        Log.d("test", "TEST: " + json.toString())
        // case 1: end_flag = true → diary
        if (json.has("result")) {
            val result = json.getAsJsonObject("result")

            if (result.has("diary") && result.get("diary").isJsonPrimitive) {
                return result.get("diary").asString
            }
        }
        // case 2: end_flag = false → {"merged_content": {"merged_content": "..."}}
        if (json.has("merged_content") && json.get("merged_content").isJsonObject) {
            val inner = json.getAsJsonObject("merged_content")
            if (inner.has("merged_content") && inner.get("merged_content").isJsonPrimitive) {
                return inner.get("merged_content").asString
            }
        }
        return ""
    }
}