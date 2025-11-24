package com.example.voicetutor.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicetutor.HiltComponentActivity
import com.example.voicetutor.data.models.AssignmentData
import com.example.voicetutor.data.models.ClassData
import com.example.voicetutor.data.models.CourseClass
import com.example.voicetutor.data.models.Subject
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
class EditAssignmentScreenHighCoverageTest {

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
        resetFakeApi()
    }

    private fun resetFakeApi() {
        fakeApi.apply {
            shouldFailCreateAssignment = false
            shouldFailCreateClass = false
            shouldFailClasses = false
            classesResponse = listOf(
                ClassData(
                    id = 1,
                    name = "수학 A반",
                    subject = Subject(id = 1, name = "수학", code = "MATH"),
                    description = "기초 수학 수업",
                    teacherId = 2,
                    teacherName = "김선생님",
                    studentCount = 25,
                    studentCountAlt = 25,
                    createdAt = "2024-01-01T00:00:00Z",
                ),
            )
            // Mock assignment for editing
            assignmentByIdResponse = AssignmentData(
                id = 100,
                title = "기존 과제",
                description = "기존 설명",
                totalQuestions = 5,
                createdAt = "2024-01-01T00:00:00Z",
                dueAt = "2024-12-31 23:59", // Use format that normalizeDateTime handles
                courseClass = CourseClass(
                    id = 1,
                    name = "수학 A반",
                    description = "desc",
                    subject = Subject(id = 1, name = "수학", code = "MATH"),
                    teacherName = "Teacher",
                    studentCount = 20,
                    createdAt = "2024-01-01"
                ),
                materials = emptyList(),
                grade = "중학교 1학년"
            )
        }
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Cover lines 254-277: Class Selection Dropdown items
    @Test
    fun testClassSelectionDropdown() {
        composeRule.setContent {
            VoiceTutorTheme {
                EditAssignmentScreen(teacherId = "2", assignmentId = 100)
            }
        }

        // Wait for data load
        waitForText("기존 과제")

        // Click class dropdown
        composeRule.onNodeWithText("반 선택").performClick()
        composeRule.waitForIdle()

        // Verify dropdown items are displayed (lines 254-277 loop)
        waitForText("수학 A반")
        
        // TextField value is "수학 A반" (index 0) and DropdownItem is "수학 A반" (index 1)
        // We want to check if the dropdown item is displayed
        composeRule.onAllNodesWithText("수학 A반", useUnmergedTree = true)
            .onLast() // Should be the dropdown item
            .assertIsDisplayed()
            .performClick() // Select it
            
        composeRule.waitForIdle()
    }

    // Cover lines 496-529: Save Button Logic & Validation
    @Test
    fun testSaveButtonValidationAndSuccess() {
        composeRule.setContent {
            VoiceTutorTheme {
                // Start with an assignment that needs editing
                EditAssignmentScreen(teacherId = "2", assignmentId = 100)
            }
        }

        waitForText("기존 과제")

        // 1. Clear title to trigger validation error
        // Use performTextReplacement with empty string instead of performTextClearance
        // to avoid potential node reference issues
        val titleField = composeRule.onNodeWithText("기존 과제")
        titleField.performTextReplacement("")
        
        composeRule.waitForIdle()
        
        // Click Save (lines 496-529)
        composeRule.onNodeWithText("저장").performClick()
        composeRule.waitForIdle()
        
        // Should show validation dialog (lines 753-778, triggered by line 527)
        waitForText("입력 오류")
        waitForText("필수 항목을 모두 입력하고")
        
        // Close dialog
        composeRule.onNodeWithText("확인").performClick()
        composeRule.waitForIdle()
        
        // 2. Fix title and save successfully
        // Find the empty text field again by label or other means since text is gone
        composeRule.onNodeWithTag("AssignmentTitleInput") // Ideally use tags, but here we rely on structure or empty state
        // Finding empty text field is tricky with just text matcher.
        // Let's use onNode(hasSetTextAction()) and filter or pick first one (Title is first)
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("수정된 과제")
        
        composeRule.onNodeWithText("저장").performClick()
        composeRule.waitForIdle()
        
        // Should verify update call - difficult to verify directly without spying ViewModel,
        // but we can verify no error dialog appears and maybe check if toast showed (hard in compose test).
        // Since we provided assignmentId=100 and fakeApi has it, it should proceed to line 523-525.
    }

    // Cover lines 593-632: Delete Dialog Flow
    @Test
    fun testDeleteFlow() {
        composeRule.setContent {
            VoiceTutorTheme {
                EditAssignmentScreen(teacherId = "2", assignmentId = 100)
            }
        }
        
        waitForText("기존 과제")
        
        // Scroll to bottom to ensure Delete button is visible
        composeRule.onNodeWithText("과제 삭제").performScrollTo()
        composeRule.waitForIdle()
        
        // Click Delete button (in Danger Zone)
        composeRule.onNodeWithText("과제 삭제").performClick()
        composeRule.waitForIdle()
        
        // Check Dialog (lines 593-632)
        waitForText("과제 삭제") // Title
        waitForText("정말로 이 과제를 삭제하시겠습니까?") // Body
        
        // Click Cancel first
        composeRule.onNodeWithText("취소").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("정말로 이 과제를 삭제하시겠습니까?").assertDoesNotExist()
        
        // Open again and Confirm
        composeRule.onNodeWithText("과제 삭제").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("삭제").performClick()
        
        // Verify delete called (screen might close or trigger onDeleteAssignment)
        // Since fakeApi doesn't track delete calls easily without spying, we assume flow works if no crash.
    }

    // Cover lines 635-751: Date and Time Picker Flow
    @Test
    fun testDateTimePickerFlow() {
        composeRule.setContent {
            VoiceTutorTheme {
                EditAssignmentScreen(teacherId = "2", assignmentId = 100)
            }
        }
        
        waitForText("기존 과제")
        
        // Scroll down to ensure DatePicker is visible
        composeRule.onNodeWithText("마감일").performScrollTo()
        composeRule.waitForIdle()
        
        // Click Due Date to open DatePicker
        composeRule.onNodeWithText("마감일").performClick()
        composeRule.waitForIdle()
        
        // DatePicker Dialog should show
        // Verify "시간 선택" button exists (confirm button for date picker, line 663)
        waitForText("시간 선택")
        
        // Click "시간 선택" to move to TimePicker
        // Note: verify date is selected first? Default might be selected.
        // If "시간 선택" is enabled, we can click it.
        composeRule.onNodeWithText("시간 선택").performClick()
        composeRule.waitForIdle()
        
        // TimePicker Dialog should show (lines 695-751)
        // Title "시간 선택" (line 742)
        waitForText("시간 선택")
        
        // Click Confirm ("확인", line 726)
        composeRule.onNodeWithText("확인").performClick()
        composeRule.waitForIdle()
        
        // Dialogs should be closed
        composeRule.onNodeWithText("시간 선택").assertDoesNotExist()
    }

    // Cover validation dialog (753-778) explicitly
    @Test
    fun testValidationDialog() {
        composeRule.setContent {
            VoiceTutorTheme {
                EditAssignmentScreen(teacherId = "2", assignmentId = 100)
            }
        }

        waitForText("기존 과제")
        
        // Clear a required field to force validation error
        // Use performTextReplacement for stability
        val titleField = composeRule.onNodeWithText("기존 과제")
        titleField.performTextReplacement("")
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("저장").performClick()
        composeRule.waitForIdle()
        
        // Dialog check
        waitForText("입력 오류")
        waitForText("필수 항목을 모두 입력하고")
        
        // Click OK
        composeRule.onNodeWithText("확인").performClick()
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("입력 오류").assertDoesNotExist()
    }
}
