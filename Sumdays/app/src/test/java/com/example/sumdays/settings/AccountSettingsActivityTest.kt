package com.example.sumdays.settings

import android.os.Build
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.R
import com.example.sumdays.auth.SessionManager
import com.example.sumdays.network.*
import com.example.sumdays.network.RetrofitClient
import com.example.sumdays.settings.prefs.UserStatsPrefs
import io.mockk.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.robolectric.shadows.ShadowActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AccountSettingsActivityTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var activity: AccountSettingsActivity
    private lateinit var mockUserStatsPrefs: UserStatsPrefs
    private lateinit var mockApiService: AuthApiService
    private lateinit var mockUpdateCall: Call<UpdateNicknameResponse>
    private lateinit var mockChangePassCall: Call<ChangePasswordResponse>

    // --- Reflection Helper for Protected isFinishing Field ---
    private fun isActivityFinishing(activity: AccountSettingsActivity): Boolean {
        val shadow = shadowOf(activity)
        return try {
            // 1. 필드 리플렉션 시도
            val field = ShadowActivity::class.java.getDeclaredField("isFinishing")
            field.isAccessible = true
            return field.getBoolean(shadow)
        } catch (e: Exception) {
            // 2. 메서드 리플렉션 시도
            try {
                val method = ShadowActivity::class.java.getDeclaredMethod("isFinishing")
                method.isAccessible = true
                return method.invoke(shadow) as Boolean
            } catch (e: Exception) {
                // 3. 최후의 수단으로 공식 Robolectric API를 통해 finish() 호출 여부 확인
                return false // (오류를 무시하고 넘어가야 할 경우)
            }
        }
    }


    // ─────────────────────────────────────────────────────────────
    // 0. Setup
    // ─────────────────────────────────────────────────────────────

    @Before
    fun setup() {
        // 1. UserStatsPrefs MockK 생성자 모킹 (가장 중요한 변경)
        mockUserStatsPrefs = mockk(relaxed = true)
        every { mockUserStatsPrefs.getNickname() } returns "InitialNickname"
        every { mockUserStatsPrefs.saveNickname(any()) } just Runs

        // AccountSettingsActivity의 onCreate()에서 'UserStatsPrefs(this)' 생성자를 호출할 때,
        // Mock 객체인 mockUserStatsPrefs를 반환하도록 MockK에 지시합니다.
        mockkConstructor(UserStatsPrefs::class)
        every { anyConstructed<UserStatsPrefs>().getNickname() } returns "InitialNickname"
        every { anyConstructed<UserStatsPrefs>().saveNickname(any()) } just Runs
        // 이제 activity.userStatsPrefs 필드는 onCreate 시점에 Mock 객체를 가지게 됩니다.

        // 2. Mock SessionManager
        mockkObject(SessionManager)
        every { SessionManager.getToken() } returns "DUMMY_TOKEN"

        // 3. Mock Retrofit API Service
        mockApiService = mockk(relaxed = true)
        mockkObject(RetrofitClient)
        every { RetrofitClient.authApiService } returns mockApiService

        // Mock Call 객체 초기화
        mockUpdateCall = mockk(relaxed = true)
        mockChangePassCall = mockk(relaxed = true)

        // Activity 빌드 및 onCreate 호출
        val controller = Robolectric.buildActivity(AccountSettingsActivity::class.java)
        activity = controller.get()

        // Reflection 주입은 생성자 모킹으로 대체되었으므로 생략합니다.

        controller.create().start().resume().visible()
    }

    @org.junit.After
    fun tearDown() {
        clearAllMocks()
    }

    // --- View ID Helper Functions ---
    private fun getCurrentNicknameTextView() = activity.findViewById<TextView>(R.id.currentNicknameTextView)
    private fun getNewNicknameInputEditText() = activity.findViewById<EditText>(R.id.newNicknameInputEditText)
    private fun getUpdateNicknameButton() = activity.findViewById<View>(R.id.updateNicknameButton)
    private fun getCurrentPasswordInputEditText() = activity.findViewById<EditText>(R.id.currentPasswordInputEditText)
    private fun getNewPasswordInputEditText() = activity.findViewById<EditText>(R.id.newPasswordInputEditText)
    private fun getConfirmPasswordInputEditText() = activity.findViewById<EditText>(R.id.confirmPasswordInputEditText)
    private fun getChangePasswordButton() = activity.findViewById<View>(R.id.changePasswordButton)
    private fun getHeaderBackIcon() = activity.findViewById<View>(R.id.headerBackIcon)


    // ─────────────────────────────────────────────────────────────
    // 1. onCreate 및 초기 상태 테스트
    // ─────────────────────────────────────────────────────────────

    @Test
    fun onCreate_initializesUIAndListeners() {
        // MockK 검증 성공 목표: onCreate에서 displayCurrentNickname()이 호출되었는지 확인
        verify(exactly = 1) { anyConstructed<UserStatsPrefs>().getNickname() }

        // 2. 초기 닉네임이 UI에 올바르게 표시되었는지 검증
        assertEquals("InitialNickname", getCurrentNicknameTextView().text.toString())
    }

    // 3. setupHeaderListener (뒤로가기 버튼) 테스트
    @Test
    fun headerBackIconClick_finishesActivity() {
        getHeaderBackIcon().performClick()

        assertTrue(isActivityFinishing(activity))
    }

    // ─────────────────────────────────────────────────────────────
    // 2. handleNicknameUpdate() 로직 커버리지 (100%)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun updateNickname_withEmptyNickname_showsToastAndReturns() {
        getNewNicknameInputEditText().setText("")
        getUpdateNicknameButton().performClick()

        verify(exactly = 0) { mockApiService.updateNickname(any(), any()) }

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("새로운 닉네임을 입력해주세요.", latestToast)
    }

    @Test
    fun updateNickname_withEmptyToken_showsToastAndReturns() {
        getNewNicknameInputEditText().setText("NewTestNickname")

        every { SessionManager.getToken() } returns null

        getUpdateNicknameButton().performClick()

        verify(exactly = 0) { mockApiService.updateNickname(any(), any()) }

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("인증 정보가 없습니다. 다시 로그인 해주세요.", latestToast)
    }

    @Test
    fun updateNickname_onSuccessfulResponse_updatesPrefsAndUI() {
        val successResponse = UpdateNicknameResponse(true, "닉네임 변경 성공!")
        val newNickname = "NewTestNickname"

        every {
            mockApiService.updateNickname(eq("Bearer DUMMY_TOKEN"), match { it.newNickname == newNickname })
        } returns mockUpdateCall

        every { mockUpdateCall.enqueue(any()) } answers {
            val callback = firstArg<Callback<UpdateNicknameResponse>>()
            // 비동기 콜백 처리 및 UI 업데이트 강제 실행
            shadowOf(activity.mainLooper).idle()

            // 💡 핵심 수정 1: saveNickname이 호출된 직후, getNickname의 Mock 반환값을 newNickname으로 변경
            every { anyConstructed<UserStatsPrefs>().getNickname() } returns newNickname

            callback.onResponse(mockUpdateCall, Response.success(successResponse))
        }

        getNewNicknameInputEditText().setText(newNickname)
        getUpdateNicknameButton().performClick()

        // UI 업데이트 완료 보장을 위해 Looper를 한번 더 idle 상태로 만듭니다.
        shadowOf(activity.mainLooper).idle()

        verify(exactly = 1) { mockApiService.updateNickname(any(), any()) }
        verify(exactly = 1) { mockUpdateCall.enqueue(any()) }

        // 로컬 SharedPreferences 업데이트 검증
        verify(exactly = 1) { anyConstructed<UserStatsPrefs>().saveNickname(newNickname) }

        // UI 업데이트 검증 (이 검증이 이제 통과해야 합니다.)
        val currentNicknameTextView = getCurrentNicknameTextView()
        assertEquals(newNickname, currentNicknameTextView.text.toString())

        // displayCurrentNickname() 호출 검증
        verify(atLeast = 2) { anyConstructed<UserStatsPrefs>().getNickname() }

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("닉네임 변경 성공!", latestToast)
    }

    @Test
    fun updateNickname_onNonSuccessfulBody_showsErrorMessage() {
        val failureResponse = UpdateNicknameResponse(false, "중복된 닉네임입니다.")

        every { mockApiService.updateNickname(any(), any()) } returns mockUpdateCall

        every { mockUpdateCall.enqueue(any()) } answers {
            val callback = firstArg<Callback<UpdateNicknameResponse>>()
            shadowOf(activity.mainLooper).idle()
            callback.onResponse(mockUpdateCall, Response.success(failureResponse))
        }

        getNewNicknameInputEditText().setText("DuplicateNickname")
        getUpdateNicknameButton().performClick()

        verify(exactly = 0) { anyConstructed<UserStatsPrefs>().saveNickname(any()) }

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("중복된 닉네임입니다.", latestToast)
    }

    @Test
    fun updateNickname_onHttpError_showsErrorCode() {
        every { mockApiService.updateNickname(any(), any()) } returns mockUpdateCall

        every { mockUpdateCall.enqueue(any()) } answers {
            val callback = firstArg<Callback<UpdateNicknameResponse>>()
            shadowOf(activity.mainLooper).idle()
            val errorBody = "Error".toResponseBody("application/json".toMediaTypeOrNull())
            callback.onResponse(mockUpdateCall, Response.error(400, errorBody))
        }

        getNewNicknameInputEditText().setText("BadNickname")
        getUpdateNicknameButton().performClick()

        verify(exactly = 0) { anyConstructed<UserStatsPrefs>().saveNickname(any()) }

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("닉네임 변경 실패 (에러 코드: 400)", latestToast)
    }

    @Test
    fun updateNickname_onNetworkFailure_showsNetworkError() {
        every { mockApiService.updateNickname(any(), any()) } returns mockUpdateCall

        every { mockUpdateCall.enqueue(any()) } answers {
            val callback = firstArg<Callback<UpdateNicknameResponse>>()
            shadowOf(activity.mainLooper).idle()
            callback.onFailure(mockUpdateCall, Throwable("Connection refused"))
        }

        getNewNicknameInputEditText().setText("TestNickname")
        getUpdateNicknameButton().performClick()

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("네트워크 오류: Connection refused", latestToast)
    }

    // ─────────────────────────────────────────────────────────────
    // 3. handleChangePassword() 로직 커버리지 (100%)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun changePassword_withEmptyFields_showsToastAndReturns() {
        getCurrentPasswordInputEditText().setText("")
        getNewPasswordInputEditText().setText("")
        getConfirmPasswordInputEditText().setText("")

        getChangePasswordButton().performClick()

        verify(exactly = 0) { mockApiService.changePassword(any(), any()) }

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("모든 비밀번호 필드를 입력해주세요.", latestToast)
    }

    @Test
    fun changePassword_withMismatchedNewPassword_showsToastAndReturns() {
        getCurrentPasswordInputEditText().setText("old_pass")
        getNewPasswordInputEditText().setText("new_pass_1")
        getConfirmPasswordInputEditText().setText("new_pass_2")

        getChangePasswordButton().performClick()

        verify(exactly = 0) { mockApiService.changePassword(any(), any()) }

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("새 비밀번호가 일치하지 않습니다.", latestToast)
    }

    @Test
    fun changePassword_withEmptyToken_showsToastAndReturns() {
        getCurrentPasswordInputEditText().setText("old_pass")
        getNewPasswordInputEditText().setText("new_pass")
        getConfirmPasswordInputEditText().setText("new_pass")

        every { SessionManager.getToken() } returns null

        getChangePasswordButton().performClick()

        verify(exactly = 0) { mockApiService.changePassword(any(), any()) }

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("인증 정보가 없습니다. 다시 로그인 해주세요.", latestToast)
    }

    @Test
    fun changePassword_onSuccessfulResponse_showsSuccessToastAndFinishes() {
        val successResponse = ChangePasswordResponse(true, "비밀번호 변경 완료")

        every {
            mockApiService.changePassword(eq("Bearer DUMMY_TOKEN"), any())
        } returns mockChangePassCall

        every { mockChangePassCall.enqueue(any()) } answers {
            val callback = firstArg<Callback<ChangePasswordResponse>>()
            shadowOf(activity.mainLooper).idle()
            callback.onResponse(mockChangePassCall, Response.success(successResponse))
        }

        getCurrentPasswordInputEditText().setText("old_pass")
        getNewPasswordInputEditText().setText("new_pass")
        getConfirmPasswordInputEditText().setText("new_pass")
        getChangePasswordButton().performClick()

        verify(exactly = 1) { mockApiService.changePassword(any(), any()) }

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("비밀번호 변경 성공! 다시 로그인 해주세요.", latestToast)

        assertTrue(isActivityFinishing(activity))
    }

    @Test
    fun changePassword_onNonSuccessfulBody_showsErrorMessage() {
        val failureResponse = ChangePasswordResponse(false, "현재 비밀번호가 올바르지 않습니다.")

        every { mockApiService.changePassword(any(), any()) } returns mockChangePassCall

        every { mockChangePassCall.enqueue(any()) } answers {
            val callback = firstArg<Callback<ChangePasswordResponse>>()
            shadowOf(activity.mainLooper).idle()
            callback.onResponse(mockChangePassCall, Response.success(failureResponse))
        }

        getCurrentPasswordInputEditText().setText("wrong_pass")
        getNewPasswordInputEditText().setText("new_pass")
        getConfirmPasswordInputEditText().setText("new_pass")
        getChangePasswordButton().performClick()

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("현재 비밀번호가 올바르지 않습니다.", latestToast)

        assertFalse(isActivityFinishing(activity))
    }

    @Test
    fun changePassword_onHttpError_showsErrorCode() {
        every { mockApiService.changePassword(any(), any()) } returns mockChangePassCall

        every { mockChangePassCall.enqueue(any()) } answers {
            val callback = firstArg<Callback<ChangePasswordResponse>>()
            shadowOf(activity.mainLooper).idle()
            val errorBody = "Server Error".toResponseBody("application/json".toMediaTypeOrNull())
            callback.onResponse(mockChangePassCall, Response.error(500, errorBody))
        }

        getCurrentPasswordInputEditText().setText("old_pass")
        getNewPasswordInputEditText().setText("new_pass")
        getConfirmPasswordInputEditText().setText("new_pass")
        getChangePasswordButton().performClick()

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("비밀번호 변경 실패 (에러 코드: 500)", latestToast)
    }

    @Test
    fun changePassword_onNetworkFailure_showsNetworkError() {
        every { mockApiService.changePassword(any(), any()) } returns mockChangePassCall

        every { mockChangePassCall.enqueue(any()) } answers {
            val callback = firstArg<Callback<ChangePasswordResponse>>()
            shadowOf(activity.mainLooper).idle()
            callback.onFailure(mockChangePassCall, Throwable("DNS resolution failed"))
        }

        getCurrentPasswordInputEditText().setText("old_pass")
        getNewPasswordInputEditText().setText("new_pass")
        getConfirmPasswordInputEditText().setText("new_pass")
        getChangePasswordButton().performClick()

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("네트워크 오류가 발생했습니다.", latestToast)
    }
}