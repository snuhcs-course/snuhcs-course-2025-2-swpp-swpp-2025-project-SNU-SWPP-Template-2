package com.example.sumdays.data.style

import android.content.Context
import com.example.sumdays.R
import com.google.gson.Gson
import java.io.InputStreamReader

data class StyleVectorData(
    val style_vector: List<Float>
)

fun loadStyleVector(context: Context, id: Int): List<Float> {
    val resId = getStyleVectorResId(id)

    val inputStream = context.resources.openRawResource(resId)
    val reader = InputStreamReader(inputStream)

    val data = Gson().fromJson(reader, StyleVectorData::class.java)
    return data.style_vector
}

private fun getStyleVectorResId(id: Int): Int {
    return when (id) {
        1 -> R.raw.style_vector1
        2 -> R.raw.style_vector2
        3 -> R.raw.style_vector3
        else -> throw IllegalArgumentException("Invalid style vector id: $id")
    }
}
