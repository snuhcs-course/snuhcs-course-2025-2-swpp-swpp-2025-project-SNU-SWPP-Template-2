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
class EditAssignmentScreenTest {

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

            assignmentByIdResponse = AssignmentData(
                id = 100,
                title = "기존 과제",
                description = "기존 설명",
                totalQuestions = 5,
                createdAt = "2024-01-01T00:00:00Z",
                dueAt = "2024-12-31 23:59",
                courseClass = CourseClass(
                    id = 1,
                    name = "수학 A반",
                    description = "desc",
                    subject = Subject(id = 1, name = "수학", code = "MATH"),
                    teacherName = "Teacher",
                    studentCount = 20,
                    createdAt = "2024-01-01",
                ),
                materials = emptyList(),
                grade = "중학교 1학년",
            )
        }
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun editAssignment_displaysTitle() {
        composeRule.setContent {
            VoiceTutorTheme {
                EditAssignmentScreen(
                    teacherId = "2",
                    assignmentId = 100
                )
            }
        }

        waitForText("기존 과제")
        composeRule.onAllNodesWithText("기존 과제", substring = true, useUnmergedTree = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun editAssignment_displaysTitleField() {
        composeRule.setContent {
            VoiceTutorTheme {
                EditAssignmentScreen(
                    teacherId = "2",
                    assignmentId = 100
                )
            }
        }

        waitForText("기존 과제")
        composeRule.onAllNodes(hasText("제목", substring = true))
            .assertCountEquals(1)
    }

    @Test
    fun editAssignment_displaysDescriptionField() {
        composeRule.setContent {
            VoiceTutorTheme {
                EditAssignmentScreen(
                    teacherId = "2",
                    assignmentId = 100
                )
            }
        }

        waitForText("기존 과제")
        composeRule.onAllNodes(hasText("설명", substring = true))
            .assertCountEquals(1)
    }

    @Test
    fun editAssignment_displaysSaveButton() {
        composeRule.setContent {
            VoiceTutorTheme {
                EditAssignmentScreen(
                    teacherId = "2",
                    assignmentId = 100
                )
            }
        }

        waitForText("기존 과제")
        composeRule.onNodeWithText("저장", substring = true).assertExists()
    }
}

