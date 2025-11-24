package com.example.voicetutor.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicetutor.HiltComponentActivity
import com.example.voicetutor.data.models.AssignmentStatus
import com.example.voicetutor.ui.theme.VoiceTutorTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import com.example.voicetutor.di.NetworkModule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.foundation.layout.Column

@HiltAndroidTest
@UninstallModules(NetworkModule::class)
@RunWith(AndroidJUnit4::class)
class AllAssignmentsScreenCoverageTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // Cover TypeBadge(String, Composer, int) - line 239 (actually line 385)
    @Test
    fun testTypeBadge() {
        composeRule.setContent {
            VoiceTutorTheme {
                // Test all TypeBadge variants
                Column {
                    TypeBadge("Quiz")
                    TypeBadge("Continuous")
                    TypeBadge("Discussion")
                    TypeBadge("Unknown")
                }
            }
        }

        // Verify Quiz badge
        composeRule.onNodeWithText("퀴즈").assertIsDisplayed()
        
        // Verify Continuous badge
        composeRule.onNodeWithText("연속").assertIsDisplayed()
        
        // Verify Discussion badge
        composeRule.onNodeWithText("토론").assertIsDisplayed()
        
        // Verify Unknown badge
        composeRule.onNodeWithText("알 수 없음").assertIsDisplayed()
    }

    // Cover CustomStatusBadge(String, Composer, int) - line 171 (actually line 338)
    @Test
    fun testCustomStatusBadge() {
        composeRule.setContent {
            VoiceTutorTheme {
                // Test all CustomStatusBadge variants
                Column {
                    CustomStatusBadge("시작 안함")
                    CustomStatusBadge("진행 중")
                    CustomStatusBadge("완료")
                    CustomStatusBadge("기타")
                }
            }
        }

        // Verify all status badges are displayed
        composeRule.onNodeWithText("시작 안함").assertIsDisplayed()
        composeRule.onNodeWithText("진행 중").assertIsDisplayed()
        composeRule.onNodeWithText("완료").assertIsDisplayed()
        composeRule.onNodeWithText("기타").assertIsDisplayed()
    }

    // Cover StatusBadge(AssignmentStatus, Composer, int) - line 362
    @Test
    fun testStatusBadge() {
        composeRule.setContent {
            VoiceTutorTheme {
                // Test all StatusBadge variants
                Column {
                    StatusBadge(AssignmentStatus.IN_PROGRESS)
                    StatusBadge(AssignmentStatus.COMPLETED)
                    StatusBadge(AssignmentStatus.DRAFT)
                }
            }
        }

        // Verify all status badges are displayed
        composeRule.onNodeWithText("진행중").assertIsDisplayed()
        composeRule.onNodeWithText("완료").assertIsDisplayed()
        composeRule.onNodeWithText("임시저장").assertIsDisplayed()
    }
}

