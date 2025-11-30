package com.example.mindlog.settings

import com.example.mindlog.core.model.UserInfo
import com.example.mindlog.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestSettingsRepository @Inject constructor() : SettingsRepository {

    // 메모리 DB 역할
    private var currentUserInfo: UserInfo = UserInfo(
        id = 1,
        loginId = "test_login_id",
        username = "기존 사용자",
        gender = "M",
        birthdate = "1990-01-01",
        appearance = "기존 외모"
    )

    // 비밀번호 업데이트 기록용 (검증을 위해 저장)
    var lastUpdatedPassword: String? = null
        private set

    override suspend fun getUserInfo(): UserInfo {
        return currentUserInfo
    }

    override suspend fun updateUserInfo(
        password: String?,
        username: String?,
        gender: String?,
        birthdate: String?,
        appearance: String?
    ) {
        // 비밀번호 변경 요청이 오면 기록
        if (password != null) {
            lastUpdatedPassword = password
        }

        // 나머지 정보 업데이트
        currentUserInfo = currentUserInfo.copy(
            username = username ?: currentUserInfo.username,
            gender = gender ?: currentUserInfo.gender,
            birthdate = birthdate ?: currentUserInfo.birthdate,
            appearance = appearance ?: currentUserInfo.appearance
        )
    }

    // 테스트 상태 초기화
    fun reset() {
        currentUserInfo = UserInfo(
            id = 1,
            loginId = "test_login_id",
            username = "기존 사용자",
            gender = "M",
            birthdate = "1990-01-01",
            appearance = "기존 외모"
        )
        lastUpdatedPassword = null
    }

    // 동기적으로 현재 유저 정보 조회 (테스트 검증용)
    fun getCurrentUserInfoSync(): UserInfo {
        return currentUserInfo
    }
}
