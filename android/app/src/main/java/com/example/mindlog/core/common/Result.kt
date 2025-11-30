package com.example.mindlog.core.common

import org.json.JSONObject
import retrofit2.HttpException

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(
        val code: Int? = null,          // HTTP 코드 또는 커스텀 에러 코드
        val message: String? = null,    // 에러 메시지
    ) : Result<Nothing>()
}
fun <T> kotlin.Result<T>.toResult(): Result<T> =
    fold(
        onSuccess = { Result.Success(it) },
        onFailure = { throwable ->
            when (throwable) {
                is HttpException -> {
                    val code = throwable.code()
                    val errorBody = throwable.response()?.errorBody()?.string()

                    val detailMessage = errorBody?.let { body ->
                        try {
                            val json = JSONObject(body)
                            json.optString("detail", null)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    Result.Error(
                        code = code,
                        message = detailMessage ?: throwable.message()
                    )
                }

                else -> Result.Error(message = throwable.message)
            }
        }
    )