package com.example.sumdays

import android.content.Intent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.auth.SessionManager
import com.example.sumdays.network.AuthApiService
import com.example.sumdays.network.LoginRequest
import com.example.sumdays.network.LoginResponse
import com.example.sumdays.network.RetrofitClient
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LoginActivityTest {

    private lateinit var activity: LoginActivity
    private lateinit var apiMock: AuthApiService
    private lateinit var callMock: Call<LoginResponse>

    @Before
    fun setup() {
        // RetrofitClient mock
        mockkObject(RetrofitClient)
        apiMock = mockk()
        callMock = mockk()
        every { RetrofitClient.authApiService } returns apiMock

        // SessionManager mock
        mockkObject(SessionManager)
        every { SessionManager.saveSession(any(), any()) } just Runs

        // 실제 Activity 로드
        activity = Robolectric.buildActivity(LoginActivity::class.java).setup().get()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * 로그인 성공 시 CalendarActivity로 이동하는지 테스트
     */
    @Test
    fun success_response_triggers_navigation_to_CalendarActivity() {
        val emailField = activity.findViewById<EditText>(R.id.idInput_EditText)
        val passwordField = activity.findViewById<EditText>(R.id.passwordInput_EditText)
        val loginButton = activity.findViewById<ImageButton>(R.id.login_button)

        // 입력 설정
        emailField.setText("user@example.com")
        passwordField.setText("password123")

        // Mock Retrofit call
        every { apiMock.login(any()) } returns callMock

        val response = LoginResponse(
            success = true,
            message = "Login success",  // ✅ null 아님
            userId = 1,
            token = "mockToken"
        )

        every { callMock.enqueue(any()) } answers {
            val cb = arg<Callback<LoginResponse>>(0)
            cb.onResponse(callMock, Response.success(response))
        }

        // 버튼 클릭 실행
        loginButton.performClick()

        val shadowActivity = Shadows.shadowOf(activity)
        val startedIntent: Intent = shadowActivity.nextStartedActivity

        assertNotNull(startedIntent)
        assertEquals(CalendarActivity::class.java.name, startedIntent.component?.className)
        verify { SessionManager.saveSession(1, "mockToken") }
    }

    /**
     * 로그인 실패 (서버 success=false) 시 Toast 메시지 표시
     */
    @Test
    fun failure_response_triggers_toast() {
        val emailField = activity.findViewById<EditText>(R.id.idInput_EditText)
        val passwordField = activity.findViewById<EditText>(R.id.passwordInput_EditText)
        val loginButton = activity.findViewById<ImageButton>(R.id.login_button)

        emailField.setText("wrong@example.com")
        passwordField.setText("badpass")

        every { apiMock.login(any()) } returns callMock

        val response = LoginResponse(
            success = false,
            message = "Invalid credentials", // ✅ null 아님
            userId = null,
            token = null
        )

        every { callMock.enqueue(any()) } answers {
            val cb = arg<Callback<LoginResponse>>(0)
            cb.onResponse(callMock, Response.success(response))
        }

        loginButton.performClick()

        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue(toast.contains("Invalid credentials"))
    }

    /**
     * 네트워크 실패 시 Toast 메시지 표시
     */
    @Test
    fun network_failure_triggers_toast() {
        val emailField = activity.findViewById<EditText>(R.id.idInput_EditText)
        val passwordField = activity.findViewById<EditText>(R.id.passwordInput_EditText)
        val loginButton = activity.findViewById<ImageButton>(R.id.login_button)

        emailField.setText("user@example.com")
        passwordField.setText("password123")

        every { apiMock.login(any()) } returns callMock

        every { callMock.enqueue(any()) } answers {
            val cb = arg<Callback<LoginResponse>>(0)
            cb.onFailure(callMock, Throwable("Network error"))
        }

        loginButton.performClick()

        val toast = ShadowToast.getTextOfLatestToast()
        assertTrue(toast.contains("네트워크 오류"))
    }
}
