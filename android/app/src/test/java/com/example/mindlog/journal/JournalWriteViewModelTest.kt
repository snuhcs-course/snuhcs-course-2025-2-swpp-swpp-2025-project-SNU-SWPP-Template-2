package com.example.mindlog.journal

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import com.example.mindlog.features.journal.domain.usecase.CreateJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.GenerateImageUseCase
import com.example.mindlog.features.journal.presentation.write.JournalWriteViewModel
import com.example.mindlog.utils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.io.ByteArrayInputStream

@ExperimentalCoroutinesApi
class JournalWriteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var createJournalUseCase: CreateJournalUseCase
    private lateinit var journalRepository: JournalRepository
    private lateinit var generateImageUseCase: GenerateImageUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var mockContext: Context

    private lateinit var viewModel: JournalWriteViewModel

    @Before
    fun setup() {
        createJournalUseCase = mock()
        journalRepository = mock()
        generateImageUseCase = mock()
        savedStateHandle = SavedStateHandle()
        mockContext = mock()

        viewModel = JournalWriteViewModel(
            createJournalUseCase = createJournalUseCase,
            journalRepository = journalRepository,
            generateImageUseCase = generateImageUseCase,
            savedStateHandle = savedStateHandle,
            context = mockContext
        )
    }

    // ----------------------------------------------------------------------
    // emotionScores 관련
    // ----------------------------------------------------------------------

    @Test
    fun `updateEmotionScore - 상태와 SavedStateHandle 모두 갱신된다`() = runTest {
        // 초기값은 null
        assertNull(viewModel.emotionScores.value["happy"])

        // When
        viewModel.updateEmotionScore("happy", 4)

        // Then
        assertEquals(4, viewModel.emotionScores.value["happy"])
        val stored = savedStateHandle.get<Map<String, Int?>>("emotionScores")
        assertNotNull(stored)
        assertEquals(4, stored!!["happy"])
    }

    @Test
    fun `saveJournal - 감정 점수 매핑 - null은 2로, 0은 제외된다`() = runTest {
        // Given
        viewModel.title.value = "제목"
        viewModel.content.value = "내용"

        viewModel.updateEmotionScore("happy", 3)
        viewModel.updateEmotionScore("sad", 0) // 0은 필터링 대상

        // ✨ [수정] UseCase가 DTO가 아닌 Int(id)를 반환하도록 Mocking
        whenever(createJournalUseCase.invoke(any(), any(), any(), any())).thenReturn(1)

        val results = mutableListOf<Result<Unit>>()
        val collectJob = launch {
            results.add(viewModel.saveResult.first())
        }

        // When
        viewModel.saveJournal()
        advanceUntilIdle()
        collectJob.cancel()

        // Then
        val emotionsCaptor = argumentCaptor<Map<String, Int>>()
        verify(createJournalUseCase).invoke(
            title = eq("제목"),
            content = eq("내용"),
            emotions = emotionsCaptor.capture(),
            gratitude = eq("")
        )

        val emotions = emotionsCaptor.firstValue
        assertFalse(emotions.containsKey("sad"))
        assertEquals(3, emotions["happy"])
        // 나머지 null이었던 값들은 2로 대체되어 들어감 (몇 개만 샘플로)
        assertEquals(2, emotions["calm"])

        assertTrue(results.first() is Result.Success)
    }

    // ----------------------------------------------------------------------
    // saveJournal - 정상/이미지/검증/예외
    // ----------------------------------------------------------------------

    @Test
    fun `saveJournal - 제목과 내용이 있을 때 - 이미지 없이 성공하고 extractKeywords 호출한다`() = runTest {
        // Given
        viewModel.title.value = "테스트 제목"
        viewModel.content.value = "테스트 내용"

        // ✨ [수정] UseCase가 DTO가 아닌 Int(id)를 반환하도록 Mocking
        whenever(createJournalUseCase.invoke(any(), any(), any(), any())).thenReturn(1)

        val results = mutableListOf<Result<Unit>>()
        val collectJob = launch {
            results.add(viewModel.saveResult.first())
        }

        // When
        viewModel.saveJournal()
        advanceUntilIdle()
        collectJob.cancel()

        // Then
        verify(createJournalUseCase, times(1)).invoke(
            title = eq("테스트 제목"),
            content = eq("테스트 내용"),
            emotions = any(),
            gratitude = eq("")
        )
        verify(journalRepository, never()).uploadJournalImage(any(), any(), any(), any())
        verify(journalRepository, times(1)).extractKeywords(1)

        assertTrue(results.first() is Result.Success)
    }

    @Test
    fun `saveJournal - 갤러리 이미지가 있을 때 - uploadJournalImage가 호출된다`() = runTest {
        // Given
        viewModel.title.value = "갤러리 제목"
        viewModel.content.value = "갤러리 내용"

        // ✨ [수정] UseCase가 DTO가 아닌 Int(id)를 반환하도록 Mocking
        whenever(createJournalUseCase.invoke(any(), any(), any(), any())).thenReturn(42)

        val mockResolver: ContentResolver = mock()
        whenever(mockContext.contentResolver).thenReturn(mockResolver)

        val dummyBytes = byteArrayOf(1, 2, 3, 4)
        val inputStream = ByteArrayInputStream(dummyBytes)

        val uri: Uri = mock()
        whenever(mockResolver.openInputStream(uri)).thenReturn(inputStream)
        whenever(mockResolver.getType(uri)).thenReturn("image/png")

        viewModel.setGalleryImageUri(uri)

        val results = mutableListOf<Result<Unit>>()
        val collectJob = launch {
            results.add(viewModel.saveResult.first())
        }

        // When
        viewModel.saveJournal()
        advanceUntilIdle()
        collectJob.cancel()

        // Then
        verify(createJournalUseCase, times(1)).invoke(any(), any(), any(), any())

        verify(journalRepository, times(1)).uploadJournalImage(
            journalId = eq(42),
            imageBytes = eq(dummyBytes),
            contentType = eq("image/png"),
            fileName = any()
        )
        verify(journalRepository, times(1)).extractKeywords(42)

        assertTrue(results.first() is Result.Success)
    }

    @Test
    fun `saveJournal - AI 이미지 비트맵이 있을 때 - JPEG로 uploadJournalImage가 호출된다`() = runTest {
        // Given
        viewModel.title.value = "AI 제목"
        viewModel.content.value = "AI 내용"

        // ✨ [수정] UseCase가 DTO가 아닌 Int(id)를 반환하도록 Mocking
        whenever(createJournalUseCase.invoke(any(), any(), any(), any())).thenReturn(7)

        val mockBitmap: Bitmap = mock()
        whenever(mockBitmap.compress(any(), any(), any())).thenReturn(true)

        viewModel.clearSelectedImage()
        viewModel.generatedImageBitmap.value = mockBitmap

        val results = mutableListOf<Result<Unit>>()
        val collectJob = launch {
            results.add(viewModel.saveResult.first())
        }

        // When
        viewModel.saveJournal()
        advanceUntilIdle()
        collectJob.cancel()

        // Then
        verify(createJournalUseCase, times(1)).invoke(any(), any(), any(), any())

        verify(journalRepository, times(1)).uploadJournalImage(
            journalId = eq(7),
            imageBytes = any(),
            contentType = eq("image/jpeg"),
            fileName = any()
        )
        verify(journalRepository, times(1)).extractKeywords(7)

        assertTrue(results.first() is Result.Success)
    }

    @Test
    fun `saveJournal - 제목이 비어있을 때 - usecase 호출 없이 에러 방출`() = runTest {
        // Given
        viewModel.title.value = ""
        viewModel.content.value = "내용"

        val results = mutableListOf<Result<Unit>>()
        val collectJob = launch {
            results.add(viewModel.saveResult.first())
        }

        // When
        viewModel.saveJournal()
        advanceUntilIdle()
        collectJob.cancel()

        // Then
        verifyNoInteractions(createJournalUseCase)
        verifyNoInteractions(journalRepository)

        assertTrue(results.first() is Result.Error)
        assertEquals("제목을 입력해주세요.", (results.first() as Result.Error).message)
    }

    @Test
    fun `saveJournal - 내용이 비어있을 때 - usecase 호출 없이 에러 방출`() = runTest {
        // Given
        viewModel.title.value = "제목"
        viewModel.content.value = ""

        val results = mutableListOf<Result<Unit>>()
        val collectJob = launch {
            results.add(viewModel.saveResult.first())
        }

        // When
        viewModel.saveJournal()
        advanceUntilIdle()
        collectJob.cancel()

        // Then
        verifyNoInteractions(createJournalUseCase)
        verifyNoInteractions(journalRepository)

        assertTrue(results.first() is Result.Error)
        assertEquals("오늘의 하루를 입력해주세요.", (results.first() as Result.Error).message)
    }

    @Test
    fun `saveJournal - createJournalUseCase가 예외를 던지면 후속 작업이 호출되지 않는다`() = runTest {
        // Given
        viewModel.title.value = "제목"
        viewModel.content.value = "내용"

        whenever(createJournalUseCase.invoke(any(), any(), any(), any()))
            .thenThrow(RuntimeException("서버 오류"))

        viewModel.saveJournal()
        advanceUntilIdle()

        // Then
        verify(createJournalUseCase, times(1)).invoke(any(), any(), any(), any())
        verify(journalRepository, never()).uploadJournalImage(any(), any(), any(), any())
        verify(journalRepository, never()).extractKeywords(any())
    }

    // ----------------------------------------------------------------------
    // setGalleryImageUri, clearSelectedImage
    // ----------------------------------------------------------------------

    @Test
    fun `setGalleryImageUri - Uri 설정 및 AI 비트맵 초기화`() = runTest {
        // Given
        val dummyBitmap: Bitmap = mock()
        viewModel.generatedImageBitmap.value = dummyBitmap
        assertNotNull(viewModel.generatedImageBitmap.value)
        assertNull(viewModel.selectedImageUri.value)

        val uri: Uri = mock()

        // When
        viewModel.setGalleryImageUri(uri)

        // Then
        assertEquals(uri, viewModel.selectedImageUri.value)
        assertNull(viewModel.generatedImageBitmap.value)
    }

    @Test
    fun `clearSelectedImage - 이미지 상태를 모두 초기화하고 noImage를 방출한다`() = runTest {
        // Given
        val dummyBitmap: Bitmap = mock()
        val uri: Uri = mock()
        viewModel.selectedImageUri.value = uri
        viewModel.generatedImageBitmap.value = dummyBitmap

        val noImageEvents = mutableListOf<Boolean>()
        val collectJob = launch {
            noImageEvents.add(viewModel.noImage.first())
        }

        // When
        viewModel.clearSelectedImage()
        advanceUntilIdle()
        collectJob.cancel()

        // Then
        assertNull(viewModel.selectedImageUri.value)
        assertNull(viewModel.generatedImageBitmap.value)
        assertEquals(listOf(true), noImageEvents)
    }

    // ----------------------------------------------------------------------
    // generateImage
    // ----------------------------------------------------------------------

    @Test
    fun `generateImage - 제목과 내용이 모두 비어있으면 에러 메시지를 방출한다`() = runTest {
        // Given
        viewModel.title.value = ""
        viewModel.content.value = ""

        val errors = mutableListOf<String>()
        val collectJob = launch {
            errors.add(viewModel.aiGenerationError.first())
        }

        // When
        viewModel.generateImage(style = "simple")
        advanceUntilIdle()
        collectJob.cancel()

        // Then
        verifyNoInteractions(generateImageUseCase)
        assertEquals(
            listOf("이미지를 생성하려면 내용이나 제목을 먼저 입력해주세요."),
            errors
        )
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `generateImage - usecase가 예외를 던지면 에러와 noImage를 방출하고 isLoading false`() = runTest {
        // Given
        viewModel.title.value = "제목"
        viewModel.content.value = "내용"

        whenever(generateImageUseCase.invoke(any(), any()))
            .thenThrow(RuntimeException("이미지 실패"))

        val errors = mutableListOf<String>()
        val noImageEvents = mutableListOf<Boolean>()

        val errorJob = launch {
            errors.add(viewModel.aiGenerationError.first())
        }
        val noImageJob = launch {
            noImageEvents.add(viewModel.noImage.first())
        }

        // When
        viewModel.generateImage(style = "fancy")
        advanceUntilIdle()
        errorJob.cancel()
        noImageJob.cancel()

        // Then
        verify(generateImageUseCase, times(1)).invoke(eq("fancy"), eq("내용"))

        assertEquals(listOf("이미지 실패"), errors)
        assertEquals(listOf(true), noImageEvents)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.selectedImageUri.value)
        assertNull(viewModel.generatedImageBitmap.value)
    }
}
