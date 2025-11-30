package com.example.mindlog.auth


import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.auth.data.api.AuthApi
import com.example.mindlog.features.auth.data.api.RefreshApi
import com.example.mindlog.features.auth.data.repository.AuthRepositoryImpl
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.auth.util.TokenManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AuthIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dispatcher: TestAuthDispatcher
    private lateinit var repo: AuthRepository
    private lateinit var tokenManager: TokenManager

    @Before
    fun setup() {
        hiltRule.inject()

        dispatcher = TestAuthDispatcher()
        mockWebServer = MockWebServer().apply {
            start(8000)
        }
        mockWebServer.dispatcher = dispatcher

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val authApi = retrofit.create(AuthApi::class.java)
        val refreshApi = retrofit.create(RefreshApi::class.java)
        val context = ApplicationProvider.getApplicationContext<Context>()

        tokenManager = TokenManager(context).also {
            it.clearTokens()
        }

        repo = AuthRepositoryImpl(
            authApi,
            refreshApi,
            tokenManager
        )
    }

    @After
    fun tearDown() {
        tokenManager.clearTokens()
        mockWebServer.shutdown()
    }

    @Test
    fun signup_saves_tokens_to_tokenManager() = runBlocking {
        dispatcher.shouldSignupSucceed = true

        val result = repo.signup(
            loginId = "test_id",
            password = "pw1234",
            username = "tester",
            gender = "male",
            birthDate = LocalDate.parse("2000-01-01")
        )

        // 실제 SharedPreferences에 저장됐는지 통합테스트
        assertThat(result is Result.Success<*>, `is`(true))
        assertThat(tokenManager.getAccessToken(), `is`("signup_access_token"))
        assertThat(tokenManager.getRefreshToken(), `is`("signup_refresh_token"))
    }

    /**
     * login 성공 시 서버에서 내려준 access/refresh 토큰이
     * 실제 TokenManager에 저장되는지 검증.
     */
    @Test
    fun login_saves_tokens_to_tokenManager() = runBlocking {
        // given: 로그인 성공 응답 사용
        dispatcher.shouldLoginSucceed = true

        val result = repo.login(
            loginId = "login_id",
            password = "pw1234"
        )

        // then: Result가 성공 타입인지, 토큰이 저장됐는지 확인
        assertThat(result is Result.Success<*>, `is`(true))
        assertThat(tokenManager.getAccessToken(), `is`("login_access_token"))
        assertThat(tokenManager.getRefreshToken(), `is`("login_refresh_token"))
    }

    /**
     * login 실패 시 Result.Error 타입을 반환하는지 검증.
     */
    @Test
    fun login_failure_returns_error_result() = runBlocking {
        // given
        dispatcher.shouldLoginSucceed = false

        val result = repo.login(
            loginId = "wrong_id",
            password = "wrong_pw"
        )

        // then
        assertThat(result is Result.Error, `is`(true))
    }

    /**
     * refresh 성공 시, 새로운 토큰으로 갱신되고 Result.Success가 반환되는지 검증.
     */
    @Test
    fun refresh_updates_tokens_when_refresh_token_exists() = runBlocking {
        // given: 먼저 signup으로 refresh 토큰을 저장해 둔다.
        dispatcher.shouldSignupSucceed = true
        repo.signup(
            loginId = "test_id",
            password = "pw1234",
            username = "tester",
            gender = "male",
            birthDate = LocalDate.parse("2000-01-01")
        )

        dispatcher.shouldRefreshSucceed = true

        // when
        val result = repo.refresh()

        // then
        assertThat(result is Result.Success<*>, `is`(true))
        assertThat(tokenManager.getAccessToken(), `is`("refreshed_access_token"))
        assertThat(tokenManager.getRefreshToken(), `is`("refreshed_refresh_token"))
    }

    /**
     * refresh 시 refresh 토큰이 없으면 false 또는 Error를 반환하는지 검증.
     * (구체 동작은 구현에 따라 다르므로 최소한 "성공이 아님"만 체크한다.)
     */
    @Test
    fun refresh_without_token_does_not_succeed() = runBlocking {
        // given: 토큰을 모두 비운 상태
        tokenManager.clearTokens()

        val result = repo.refresh()

        // then: Result.Success(true)가 아닌지만 확인
        val successTrue = (result is Result.Success<*>) &&
                ((result as Result.Success<*>).data == true)
        assertThat(successTrue, `is`(false))
    }

    /**
     * verify 성공 시 Result.Success(true)를 반환하는지 검증.
     */
    @Test
    fun verify_success_when_access_token_exists() = runBlocking {
        // given: access 토큰만 임의로 저장
        tokenManager.saveTokens(
            access = "some_access_token",
            refresh = "some_refresh"
        )

        val result = repo.verify()

        // then
        assertThat(result is Result.Success<*>, `is`(true))
    }

    /**
     * access 토큰이 없을 때 verify는 성공으로 간주되지 않는지 검증.
     */
    @Test
    fun verify_without_token_does_not_succeed() = runBlocking {
        // given
        tokenManager.clearTokens()

        val result = repo.verify()

        // then: Result.Success(true)가 아닌지만 확인
        val successTrue = (result is Result.Success<*>) &&
                ((result as Result.Success<*>).data == true)
        assertThat(successTrue, `is`(false))
    }

    /**
     * logout 호출 시 서버에 요청을 보내고, TokenManager의 토큰이 모두 비워지는지 검증.
     */
    @Test
    fun logout_clears_tokens() = runBlocking {
        // given: 토큰을 저장한 상태
        tokenManager.saveTokens(
            access = "existing_access",
            refresh = "existing_refresh"
        )

        val result = repo.logout()

        // then
        assertThat(result is Result.Success<*>, `is`(true))
        assertThat(tokenManager.getAccessToken(), `is`(null as String?))
        assertThat(tokenManager.getRefreshToken(), `is`(null as String?))
    }


}