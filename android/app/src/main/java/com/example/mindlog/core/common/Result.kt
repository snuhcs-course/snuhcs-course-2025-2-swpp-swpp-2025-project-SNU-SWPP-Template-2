package com.example.mindlog.core.common

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
        onFailure = {
            when (it) {
                is HttpException -> Result.Error(code = it.code(), message = it.message())
                else             -> Result.Error(message = it.message)
            }
        }
    )