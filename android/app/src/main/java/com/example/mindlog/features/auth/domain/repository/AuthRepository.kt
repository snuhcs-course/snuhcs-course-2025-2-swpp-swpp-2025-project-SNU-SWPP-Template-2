package com.example.mindlog.features.auth.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(loginId: String, password: String): Boolean
    suspend fun signup(loginId: String, password: String, username: String): Boolean
    suspend fun refresh(refresh: String): Boolean
    suspend fun verify(): Boolean
    suspend fun logout(): Boolean

    fun isLoggedInFlow(): Flow<Boolean>
}