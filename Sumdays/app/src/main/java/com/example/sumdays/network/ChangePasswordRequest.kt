package com.example.sumdays.network

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)