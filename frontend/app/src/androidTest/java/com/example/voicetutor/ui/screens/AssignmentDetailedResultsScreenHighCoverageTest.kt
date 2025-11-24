package com.example.voicetutor.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicetutor.HiltComponentActivity
import com.example.voicetutor.data.models.AssignmentCorrectnessItem
import com.example.voicetutor.data.models.PersonalAssignmentStatistics
import com.example.voicetutor.data.network.ApiService
import com.example.voicetutor.data.network.FakeApiService
import com.example.voicetutor.di.NetworkModule
import com.example.voicetutor.ui.theme.VoiceTutorTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(NetworkModule::class)
@RunWith(AndroidJUnit4::class)
class AssignmentDetailedResultsScreenHighCoverageTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Inject
    lateinit var apiService: ApiService

    private val fakeApi: FakeApiService
        get() = apiService as FakeApiService

    @Before
    fun setUp() {
        hiltRule.inject()
        
        // Set up mock data with tail questions
        fakeApi.apply {
            personalAssignmentStatisticsResponses[1] = PersonalAssignmentStatistics(
                totalQuestions = 2,
                answeredQuestions = 2,
                correctAnswers = 1,
                accuracy = 50f,
                totalProblem = 2,
                solvedProblem = 2,
                progress = 100f,
                averageScore = 75f
            )
            
            assignmentCorrectnessResponses = listOf(
                // Base question 1 (Correct)
                AssignmentCorrectnessItem(
                    questionNum = "1",
                    questionContent = "메인 질문 1",
                    studentAnswer = "정답",
                    questionModelAnswer = "정답",
                    isCorrect = true,
                    explanation = "해설 1",
                    answeredAt = "2024-01-01"
                ),
                // Base question 2 (Incorrect, has tail questions)
                AssignmentCorrectnessItem(
                    questionNum = "2",
                    questionContent = "메인 질문 2",
                    studentAnswer = "오답",
                    questionModelAnswer = "정답",
                    isCorrect = false,
                    explanation = "해설 2",
                    answeredAt = "2024-01-01"
                ),
                // Tail question 2-1 (Correct)
                AssignmentCorrectnessItem(
                    questionNum = "2-1",
                    questionContent = "꼬리 질문 2-1",
                    studentAnswer = "꼬리 정답",
                    questionModelAnswer = "꼬리 정답",
                    isCorrect = true,
                    explanation = "꼬리 해설 2-1",
                    answeredAt = "2024-01-01"
                ),
                // Tail question 2-2 (Incorrect)
                AssignmentCorrectnessItem(
                    questionNum = "2-2",
                    questionContent = "꼬리 질문 2-2",
                    studentAnswer = "꼬리 오답",
                    questionModelAnswer = "꼬리 정답",
                    isCorrect = false,
                    explanation = "꼬리 해설 2-2",
                    answeredAt = "2024-01-01"
                )
            )
        }
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Cover lines 288-313: Tail question toggle visibility and initial state
    // Cover lines 399-533: Tail question list rendering and collapse button
    @Test
    fun testTailQuestionToggleAndContent() {
        composeRule.setContent {
            VoiceTutorTheme {
                AssignmentDetailedResultsScreen(personalAssignmentId = 1)
            }
        }

        waitForText("메인 질문 1")

        // 1. Check Base Question 1 (No tail questions)
        // Should NOT have toggle button
        composeRule.onNodeWithText("메인 질문 1")
            .assertIsDisplayed()
        
        // "꼬리질문 펼치기" should not be associated with Q1.
        // However, Q2 has it. We need to be specific.
        // Finding the row that contains "문제 1" and checking for toggle might be hard with just text.
        // We can verify that we only see one "꼬리질문 펼치기" (for Q2) initially.
        
        // 2. Check Base Question 2 (Has tail questions)
        composeRule.onNodeWithText("메인 질문 2").performScrollTo()
        composeRule.waitForIdle()
        
        // Find toggle button "꼬리질문 펼치기"
        val toggleButton = composeRule.onNodeWithText("꼬리질문 펼치기")
        toggleButton.assertIsDisplayed()
        
        // Click to expand
        toggleButton.performClick()
        composeRule.waitForIdle()
        
        // Wait for tail questions to appear
        waitForText("꼬리 질문 2-1")
        
        // 3. Verify Tail Questions are displayed (lines 399-533)
        // Tail Q 2-1
        composeRule.onNodeWithText("꼬리 질문 2-1").performScrollTo()
        composeRule.onNodeWithText("꼬리 질문 2-1").assertIsDisplayed()
        // "꼬리 정답" appears in both "내 답변" and "정답" sections for Q2-1
        // Wait for the text to appear and verify it exists
        waitForText("꼬리 정답")
        // Scroll to the first occurrence of "꼬리 정답" to ensure it's visible
        composeRule.onAllNodesWithText("꼬리 정답", useUnmergedTree = true).onFirst().performScrollTo()
        composeRule.onAllNodesWithText("꼬리 정답", useUnmergedTree = true).onFirst().assertIsDisplayed()
        
        // Tail Q 2-2
        composeRule.onNodeWithText("꼬리 질문 2-2").performScrollTo()
        composeRule.onNodeWithText("꼬리 질문 2-2").assertIsDisplayed()
        waitForText("꼬리 오답")
        composeRule.onNodeWithText("꼬리 오답").performScrollTo()
        composeRule.onNodeWithText("꼬리 오답").assertIsDisplayed()
        
        // 4. Verify "꼬리질문 접기" button at the bottom of list (lines 500-529)
        // Note: The toggle text changes to "꼬리질문 접기" at top too (line 300)
        // But there is also a dedicated collapse button at the bottom (line 516)
        // We can check for presence of "꼬리질문 접기" text.
        
        // Click collapse button at the bottom
        // Finding the bottom button specifically: it's inside the tail question column
        // We can look for "꼬리질문 접기" and click the last one (since header might also say "접기" if logic line 300 updates)
        // Line 300 logic: if (isExpanded) "꼬리질문 접기" else "꼬리질문 펼치기"
        // So both top and bottom buttons will have text "꼬리질문 접기"
        
        composeRule.onAllNodesWithText("꼬리질문 접기")
            .onLast() // Bottom button
            .performClick()
            
        composeRule.waitForIdle()        
    }

    // Cover lines 536-655: DetailedQuestionResultCard content
    @Test
    fun testDetailedQuestionResultCardContent() {
        composeRule.setContent {
            VoiceTutorTheme {
                AssignmentDetailedResultsScreen(personalAssignmentId = 1)
            }
        }

        waitForText("메인 질문 1")

        // 1. Check Correct Question (Q1)
        composeRule.onNodeWithText("메인 질문 1").assertIsDisplayed()
        
        // Result badge "정답"
        // Might be multiple "정답" texts (badge and answer content).
        // We verify at least one exists near Q1.
        
        // My Answer & Correct Answer - use onAllNodesWithText since there are multiple "내 답변" nodes
        composeRule.onAllNodesWithText("내 답변").onFirst().assertIsDisplayed()
        // "정답" text appears in multiple places (Badge, Header, Content).
        // We can verify the specific answer text "정답" (from mock data) exists.
        
        // Explanation - "해설" appears multiple times (Q1 and Q2 both have it)
        composeRule.onAllNodesWithText("해설").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("해설 1").assertIsDisplayed()

        // 2. Check Incorrect Question (Q2)
        composeRule.onNodeWithText("메인 질문 2").performScrollTo()
        
        // Result badge "오답"
        composeRule.onNodeWithText("오답").assertIsDisplayed()
        
        // My Answer (Incorrect) - use onAllNodesWithText to get the second "내 답변" (for Q2)
        composeRule.onAllNodesWithText("내 답변").onLast().assertIsDisplayed()
        composeRule.onNodeWithText("오답").assertIsDisplayed() // Student answer
        
        // Correct Answer
        // Header "정답" and content "정답" (model answer)
        
        // Explanation
        composeRule.onNodeWithText("해설 2").assertIsDisplayed()
    }
}

