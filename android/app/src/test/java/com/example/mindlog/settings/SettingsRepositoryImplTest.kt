package com.example.mindlog.settings

import com.example.mindlog.features.settings.data.api.SettingsApi
import com.example.mindlog.features.settings.data.dto.UserUpdateRequest
import com.example.mindlog.features.settings.data.repository.SettingsRepositoryImpl
import com.google.gson.GsonBuilder
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SettingsRepositoryImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var settingsApi: SettingsApi
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val gson = GsonBuilder().setLenient().create()
        settingsApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(SettingsApi::class.java)

        repository = SettingsRepositoryImpl(settingsApi)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getUserInfo - API 호출 후 UserInfo 모델로 올바르게 매핑하여 반환한다`() = runTest {
        // Given
        val jsonResponse = """
            {
                "id": 1,
                "login_id": "test_login_id",
                "username": "테스트유저",
                "gender": "M",
                "birthdate": "2025-01-01",
                "appearance": "Handsome"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        // When
        val result = repository.getUserInfo()

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/user/me", request.path)

        assertEquals(1, result.id)
        assertEquals("test_login_id", result.loginId)
        assertEquals("테스트유저", result.username)
        assertEquals("M", result.gender)
        assertEquals("2025-01-01", result.birthdate)
        assertEquals("Handsome", result.appearance)
    }

    @Test(expected = RuntimeException::class)
    fun `updateUserInfo - 실패 응답(4xx, 5xx) 시 예외를 던진다`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))

        // When
        repository.updateUserInfo(username = "NewName")
    }

    @Test
    fun `updateUserInfo - API에 올바른 파라미터로 요청을 보낸다`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("\"Update Success\""))

        // When
        repository.updateUserInfo(
            username = "NewName",
            gender = "F"
        )

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/user/me", request.path)

        val body = request.body.readUtf8()
        // Gson으로 직렬화된 JSON 문자열에 해당 필드가 포함되어 있는지 확인
        assert(body.contains(""""username":"NewName""""))
        assert(body.contains(""""gender":"F""""))
    }

}
