package com.example.voicetutor.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicetutor.HiltComponentActivity
import com.example.voicetutor.di.NetworkModule
import com.example.voicetutor.ui.theme.VoiceTutorTheme
import com.example.voicetutor.ui.viewmodel.AuthViewModel
import com.example.voicetutor.ui.viewmodel.SignupError
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(NetworkModule::class)
@RunWith(AndroidJUnit4::class)
class SignupScreenCoverageTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun <T> setStateFlow(viewModel: AuthViewModel, fieldName: String, value: T) {
        val field = AuthViewModel::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as MutableStateFlow<T>
        stateFlow.value = value
    }

    // Cover lines 443-494: General Error Handling
    @Test
    fun testGeneralErrorHandling() {
        composeRule.setContent {
            VoiceTutorTheme {
                SignupScreen()
            }
        }

        val viewModel = ViewModelProvider(composeRule.activity)[AuthViewModel::class.java]

        // 1. Test Duplicate Email Error
        composeRule.runOnIdle {
            setStateFlow(viewModel, "_signupError", SignupError.General.DuplicateEmail)
        }
        composeRule.waitForIdle()

        // Verify error message and button
        composeRule.onNodeWithText("이미 가입된 이메일입니다.").assertIsDisplayed()
        composeRule.onNodeWithText("로그인하기").assertIsDisplayed()
        composeRule.onNodeWithText("로그인하기").performClick()

        // 2. Test Network Error
        composeRule.runOnIdle {
            setStateFlow(viewModel, "_signupError", SignupError.General.Network)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("네트워크 연결을 확인해주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").performClick()
        
        // 3. Test Server Error
        composeRule.runOnIdle {
            setStateFlow(viewModel, "_signupError", SignupError.General.Server)
        }
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("서버 오류가 발생했습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").assertIsDisplayed()
        
        // 4. Test Unknown Error
        composeRule.runOnIdle {
            setStateFlow(viewModel, "_signupError", SignupError.General.Unknown)
        }
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("알 수 없는 오류가 발생했습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").assertIsDisplayed()
    }
}

