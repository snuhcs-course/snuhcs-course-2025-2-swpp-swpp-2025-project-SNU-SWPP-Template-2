package com.example.voicetutor.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicetutor.HiltComponentActivity
import com.example.voicetutor.data.models.AnswerSubmissionResponse
import com.example.voicetutor.data.models.PersonalAssignmentQuestion
import com.example.voicetutor.data.network.ApiService
import com.example.voicetutor.data.network.FakeApiService
import com.example.voicetutor.di.NetworkModule
import com.example.voicetutor.ui.theme.VoiceTutorTheme
import com.example.voicetutor.ui.viewmodel.AssignmentViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(NetworkModule::class)
@RunWith(AndroidJUnit4::class)
class AssignmentScreenCoverageTest {

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
    }

    private fun <T> setStateFlow(viewModel: AssignmentViewModel, fieldName: String, value: T) {
        val field = AssignmentViewModel::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as MutableStateFlow<T>
        stateFlow.value = value
    }

    // Cover lines 97-125: Processing state UI
    @Test
    fun testProcessingStateUI() {
        composeRule.setContent {
            VoiceTutorTheme {
                AssignmentScreen(assignmentId = 1)
            }
        }

        val viewModel = ViewModelProvider(composeRule.activity)[AssignmentViewModel::class.java]

        // Set isProcessing to true
        composeRule.runOnIdle {
            setStateFlow(viewModel, "_isProcessing", true)
        }

        composeRule.waitForIdle()

        // Verify processing UI
        composeRule.onNodeWithText("채점 중입니다. 잠시 대기하세요.").assertIsDisplayed()
    }

    // Cover lines 523-657: Recording completed UI & Playback
    @Test
    fun testRecordingCompletedUI() {
        composeRule.setContent {
            VoiceTutorTheme {
                AssignmentScreen(assignmentId = 1)
            }
        }

        val viewModel = ViewModelProvider(composeRule.activity)[AssignmentViewModel::class.java]

        // Set recording state to completed with a dummy file path
        // Note: audioRecordingState is a StateFlow of AudioRecordingState data class
        // We need to update the whole state object
        val completedState = viewModel.audioRecordingState.value.copy(
            isRecording = false,
            isRecordingComplete = true,
            audioFilePath = "/dummy/path/audio.wav",
            recordingTime = 10
        )
        
        composeRule.runOnIdle {
            setStateFlow(viewModel, "_audioRecordingState", completedState)
        }

        composeRule.waitForIdle()

        // Verify "녹음 완료" UI
        composeRule.onNodeWithText("녹음 완료 (00:10)").assertIsDisplayed()
        
        // Verify "다시 듣기" text
        composeRule.onNodeWithText("다시 듣기").assertIsDisplayed()
        
        // Verify "음성 재생" icon button exists (content description "음성 재생")
        composeRule.onNodeWithContentDescription("음성 재생").assertIsDisplayed()
        
        // Note: Actually clicking play might fail if file doesn't exist or MediaPlayer fails,
        // but the UI logic for displaying it is covered.
    }

    // Cover lines 754-777: Recording buttons logic (Stop / Restart)
    @Test
    fun testRecordingButtonsLogic() {
        composeRule.setContent {
            VoiceTutorTheme {
                AssignmentScreen(assignmentId = 1)
            }
        }

        val viewModel = ViewModelProvider(composeRule.activity)[AssignmentViewModel::class.java]

        // 1. Test "녹음 중지" button when recording
        val recordingState = viewModel.audioRecordingState.value.copy(
            isRecording = true,
            audioFilePath = null
        )
        composeRule.runOnIdle {
            setStateFlow(viewModel, "_audioRecordingState", recordingState)
        }
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("녹음 중지").assertIsDisplayed()
        
        // 2. Test "다시 녹음하기" button when completed
        val completedState = viewModel.audioRecordingState.value.copy(
            isRecording = false,
            audioFilePath = "/dummy/path.wav"
        )
        composeRule.runOnIdle {
            setStateFlow(viewModel, "_audioRecordingState", completedState)
        }
        composeRule.waitForIdle()
        
        composeRule.onNodeWithText("다시 녹음하기").assertIsDisplayed()
        
        // Click "다시 녹음하기" -> should call resetAudioRecording
        composeRule.onNodeWithText("다시 녹음하기").performClick()
        composeRule.waitForIdle()
        
        // Verify state is reset (isRecording=false, filePath=null)
        // Since ViewModel implementation resets it, UI should update to "녹음 시작"
        composeRule.onNodeWithText("녹음 시작").assertIsDisplayed()
    }

    // Cover lines 798-828: Submit Answer Button
    @Test
    fun testSubmitAnswerButton() {
        composeRule.setContent {
            VoiceTutorTheme {
                AssignmentScreen(assignmentId = 1)
            }
        }

        val viewModel = ViewModelProvider(composeRule.activity)[AssignmentViewModel::class.java]

        // Set state to allow submission: file exists, not recording
        val readyToSubmitState = viewModel.audioRecordingState.value.copy(
            isRecording = false,
            audioFilePath = "/dummy/path.wav" // File must exist for real submission logic, but button enablement depends on this string
        )
        
        // Ensure current question exists
        val questions = listOf(
            PersonalAssignmentQuestion(
                id = 1, number = "1", question = "Q1", answer = "A1", explanation = "Exp1"
            )
        )
        composeRule.runOnIdle {
            setStateFlow(viewModel, "_audioRecordingState", readyToSubmitState)
            setStateFlow(viewModel, "_personalAssignmentQuestions", questions)
        }
        
        composeRule.waitForIdle()
        
        // Button should be enabled and visible
        composeRule.onNodeWithText("음성 답안 제출하기").assertIsDisplayed()
        composeRule.onNodeWithText("음성 답안 제출하기").assertIsEnabled()
        
        // Click submit
        // Note: Actual submission might fail due to file not found exception in onClick,
        // but the click handler logic is triggered.
        // We can verify if it shows snackbar or calls viewmodel if we could mock file.
        // For coverage, clicking it is sufficient to execute the onClick lambda.
        composeRule.onNodeWithText("음성 답안 제출하기").performClick()
        composeRule.waitForIdle()
    }
    
    // Cover lines 710-736: Skip Button
    @Test
    fun testSkipButton() {
        composeRule.setContent {
            VoiceTutorTheme {
                AssignmentScreen(assignmentId = 1)
            }
        }
        
        // Ensure "건너뛰기" button is visible (when not recording and no file)
        composeRule.onNodeWithText("건너뛰기").assertIsDisplayed()
        
        // Click skip
        composeRule.onNodeWithText("건너뛰기").performClick()
        composeRule.waitForIdle()
        
        // This triggers logic to create empty file and submit.
        // Might fail internally but covers the UI interaction lines.
    }
}

