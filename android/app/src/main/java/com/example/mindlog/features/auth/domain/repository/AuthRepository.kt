package com.example.mindlog.features.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import com.example.mindlog.core.common.Result

interface AuthRepository {
    suspend fun login(loginId: String, password: String): Result<Boolean>
    suspend fun signup(loginId: String, password: String, username: String, gender: String, birthDate: LocalDate): Result<Boolean>
    suspend fun refresh(): Result<Boolean>
    suspend fun verify(): Result<Boolean>
    suspend fun logout(): Result<Boolean>

    fun isLoggedInFlow(): Flow<Boolean>
}