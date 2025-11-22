package com.example.mindlog.journal

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.model.Emotion
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.core.model.Keyword
import com.example.mindlog.features.journal.domain.usecase.*
import com.example.mindlog.features.journal.presentation.write.JournalEditViewModel
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
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*
import java.io.ByteArrayInputStream
import java.util.Date

@ExperimentalCoroutinesApi
class JournalEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var getJournalByIdUseCase: GetJournalByIdUseCase
    private lateinit var updateJournalUseCase: UpdateJournalUseCase
    private lateinit var deleteJournalUseCase: DeleteJournalUseCase
    private lateinit var uploadJournalImageUseCase: UploadJournalImageUseCase
    private lateinit var generateImageUseCase: GenerateImageUseCase
    private lateinit var extractKeywordsUseCase: ExtractKeywordsUseCase
    private lateinit var mockContext: Context

    private lateinit var viewModel: JournalEditViewModel

    private val dummyKeywords = listOf(Keyword("키워드1", "happy", "요약", 0.8f))
    private val dummyEmotions = listOf(Emotion("happy", 4))
    private val dummyDate = Date()
    private val dummyJournalEntry = JournalEntry(
        id = 1,
        title = "제목",
        content = "내용",
        createdAt = dummyDate,
        imageUrl = "https://example.com/image.jpg",
        keywords = dummyKeywords,
        emotions = dummyEmotions,
        gratitude = "감사"
    )

    @Before
    fun setup() {
        getJournalByIdUseCase = mock()
        updateJournalUseCase = mock()
        deleteJournalUseCase = mock()
        uploadJournalImageUseCase = mock()
        generateImageUseCase = mock()
        extractKeywordsUseCase = mock()
        mockContext = mock()

        viewModel = JournalEditViewModel(
            getJournalByIdUseCase = getJournalByIdUseCase,
            updateJournalUseCase = updateJournalUseCase,
            deleteJournalUseCase = deleteJournalUseCase,
            uploadJournalImageUseCase = uploadJournalImageUseCase,
            generateImageUseCase = generateImageUseCase,
            extractKeywordsUseCase = extractKeywordsUseCase,
            context = mockContext
        )
    }

    // ------------------------------
    // loadJournalDetails
    // ------------------------------

    @Test
    fun `loadJournalDetails - usecase 예외 시 Error 상태가 설정된다`() = runTest {
        whenever(getJournalByIdUseCase.invoke(1)).thenThrow(RuntimeException("불러오기 실패"))
        viewModel.loadJournalDetails(1)
        advanceUntilIdle()
        val state = viewModel.journalState.value
        assertTrue(state is Result.Error)
        assertEquals("불러오기 실패", (state as Result.Error).message)
    }

    @Test
    fun `loadJournalDetails - 성공 시 상태와 LiveData 필드가 올바르게 세팅된다`() = runTest {
        whenever(getJournalByIdUseCase.invoke(1)).thenReturn(dummyJournalEntry)
        viewModel.loadJournalDetails(1)
        advanceUntilIdle()
        val state = viewModel.journalState.value
        assertTrue(state is Result.Success)
        assertEquals(dummyJournalEntry, (state as Result.Success).data)
        assertEquals("제목", viewModel.title.value)
        assertEquals("내용", viewModel.content.value)
        assertEquals("감사", viewModel.gratitude.value)
        assertEquals("https://example.com/image.jpg", viewModel.existingImageUrl.value)
        assertEquals(dummyEmotions, viewModel.emotions.value)
        assertEquals(dummyKeywords, viewModel.keywords.value)
        verify(extractKeywordsUseCase, never()).invoke(any())
    }

    @Test
    fun `loadJournalDetails - 키워드가 비어있으면 extractKeywords가 호출된다`() = runTest {
        val journalWithoutKeywords = dummyJournalEntry.copy(keywords = emptyList())
        whenever(getJournalByIdUseCase.invoke(1))
            .thenReturn(journalWithoutKeywords)
            .thenReturn(dummyJournalEntry)
        viewModel.loadJournalDetails(1)
        advanceUntilIdle()
        verify(getJournalByIdUseCase, times(2)).invoke(1)
        verify(extractKeywordsUseCase, times(1)).invoke(1)
        val finalState = viewModel.journalState.value
        assertTrue(finalState is Result.Success)
        assertEquals(dummyJournalEntry, (finalState as Result.Success).data)
        assertEquals(dummyKeywords, viewModel.keywords.value)
    }


    // ------------------------------
    // updateJournal
    // ------------------------------

    @Test
    fun `updateJournal - 제목이 비어있으면 에러를 방출하고 usecase를 호출하지 않는다`() = runTest {
        // ✨[수정] DTO Mock 대신 실제 UI 모델 객체 사용
        setPrivateField(viewModel, "journalId", 1)
        setPrivateField(viewModel, "originalJournal", dummyJournalEntry)
        viewModel.title.value = ""
        viewModel.content.value = "새 내용"

        val results = mutableListOf<Result<String>>()
        val job = launch { results.add(viewModel.editResult.first()) }
        viewModel.updateJournal()
        advanceUntilIdle()
        job.cancel()

        verifyNoInteractions(updateJournalUseCase)
        verifyNoInteractions(uploadJournalImageUseCase)
        assertTrue(results.first() is Result.Error)
        assertEquals("제목을 입력해주세요.", (results.first() as Result.Error).message)
    }

    @Test
    fun `updateJournal - 내용이 비어있으면 에러를 방출하고 usecase를 호출하지 않는다`() = runTest {
        // ✨[수정] DTO Mock 대신 실제 UI 모델 객체 사용
        setPrivateField(viewModel, "journalId", 1)
        setPrivateField(viewModel, "originalJournal", dummyJournalEntry)
        viewModel.title.value = "새 제목"
        viewModel.content.value = ""

        val results = mutableListOf<Result<String>>()
        val job = launch { results.add(viewModel.editResult.first()) }
        viewModel.updateJournal()
        advanceUntilIdle()
        job.cancel()

        verifyNoInteractions(updateJournalUseCase)
        verifyNoInteractions(uploadJournalImageUseCase)
        assertTrue(results.first() is Result.Error)
        assertEquals("오늘의 하루를 입력해주세요.", (results.first() as Result.Error).message)
    }

    @Test
    fun `updateJournal - 텍스트와 이미지 모두 변경되지 않으면 즉시 성공을 방출한다`() = runTest {
        setPrivateField(viewModel, "journalId", 1)
        setPrivateField(viewModel, "originalJournal", dummyJournalEntry)
        viewModel.title.value = dummyJournalEntry.title
        viewModel.content.value = dummyJournalEntry.content
        viewModel.gratitude.value = dummyJournalEntry.gratitude
        viewModel.existingImageUrl.value = dummyJournalEntry.imageUrl

        val results = mutableListOf<Result<String>>()
        val job = launch { results.add(viewModel.editResult.first()) }
        viewModel.updateJournal()
        advanceUntilIdle()
        job.cancel()

        verifyNoInteractions(updateJournalUseCase)
        verifyNoInteractions(uploadJournalImageUseCase)
        assertTrue(results.first() is Result.Success)
        assertEquals("수정 완료", (results.first() as Result.Success).data)
    }

    @Test
    fun `updateJournal - 텍스트만 변경되었을 때 updateJournalUseCase만 호출된다`() = runTest {
        // Given: 실제 시나리오처럼 loadJournalDetails를 먼저 호출하여 ViewModel의 모든 상태를 초기화합니다.
        whenever(getJournalByIdUseCase.invoke(1)).thenReturn(dummyJournalEntry)
        viewModel.loadJournalDetails(1)
        advanceUntilIdle()

        // When: 초기화된 상태에서 제목 LiveData의 값만 변경합니다.
        viewModel.title.value = "새로운 제목"
        viewModel.updateJournal()
        advanceUntilIdle()

        // Then: ViewModel이 변경을 정확히 감지하고 UseCase를 올바른 인자로 호출하는지 검증합니다.
        verify(updateJournalUseCase, times(1)).invoke(
            journalId = eq(1),
            originalTitle = eq(dummyJournalEntry.title), // "제목"
            originalContent = eq(dummyJournalEntry.content), // "내용"
            originalGratitude = eq(dummyJournalEntry.gratitude ?: ""), // "감사"
            newTitle = eq("새로운 제목"),
            newContent = eq(dummyJournalEntry.content), // 변경되지 않은 "내용"
            newGratitude = eq(dummyJournalEntry.gratitude ?: "")  // 변경되지 않은 "감사"
        )
        // 이미지는 변경되지 않았으므로 upload UseCase는 호출되지 않아야 합니다.
        verifyNoInteractions(uploadJournalImageUseCase)
    }

    @Test
    fun `updateJournal - 갤러리 이미지가 변경되면 uploadJournalImageUseCase가 호출된다`() = runTest {
        // Given: 실제 시나리오처럼 loadJournalDetails를 먼저 호출하여 ViewModel의 모든 상태를 초기화합니다.
        whenever(getJournalByIdUseCase.invoke(1)).thenReturn(dummyJournalEntry)
        viewModel.loadJournalDetails(1)
        advanceUntilIdle()

        // Given: 이미지 업로드를 위한 Mocking 설정
        val mockResolver: ContentResolver = mock()
        whenever(mockContext.contentResolver).thenReturn(mockResolver)
        val dummyBytes = "image_data".toByteArray()
        val inputStream = ByteArrayInputStream(dummyBytes)
        val uri: Uri = mock()
        whenever(mockResolver.openInputStream(uri)).thenReturn(inputStream)
        whenever(mockResolver.getType(uri)).thenReturn("image/jpeg")

        // When: 초기화된 상태에서 갤러리 이미지만 변경하고 update를 호출합니다.
        viewModel.setGalleryImageUri(uri)
        viewModel.updateJournal()
        advanceUntilIdle()

        // Then: ViewModel 로직에 따라 텍스트는 변경되지 않았으므로 updateJournalUseCase는 호출되지 않아야 합니다.
        verify(updateJournalUseCase, never()).invoke(any(), any(), any(), any(), any(), any(), any())

        // Then: 이미지는 변경되었으므로 uploadJournalImageUseCase는 호출되어야 합니다.
        verify(uploadJournalImageUseCase, times(1)).invoke(
            journalId = eq(1),
            imageBytes = eq(dummyBytes),
            contentType = eq("image/jpeg"),
            fileName = any()
        )
    }

    // ------------------------------
    // deleteJournal
    // ------------------------------
    @Test
    fun `deleteJournal - 성공 시 Success를 방출한다`() = runTest {
        setPrivateField(viewModel, "journalId", 1)
        val results = mutableListOf<Result<String>>()
        val job = launch { results.add(viewModel.editResult.first()) }
        viewModel.deleteJournal()
        advanceUntilIdle()
        job.cancel()
        verify(deleteJournalUseCase, times(1)).invoke(1)
        assertTrue(results.first() is Result.Success)
        assertEquals("삭제 완료", (results.first() as Result.Success).data)
    }

    // ------------------------------
    // 나머지 테스트 (generateImage, setGalleryImageUri 등)
    // ------------------------------
    // ... 이전 답변에서 제공된 다른 테스트들은 이미 올바르게 수정되었으므로 생략 ...

    private fun setPrivateField(obj: Any, fieldName: String, value: Any?) {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }

    @Test
    fun `clearSelectedImage - 모든 이미지 상태를 null로 만들고 noImage 이벤트를 방출한다`() = runTest {
        // Given: 뷰모델에 다양한 종류의 이미지가 설정되어 있다고 가정
        viewModel.selectedImageUri.value = mock()
        viewModel.existingImageUrl.value = "http://example.com/image.jpg"
        viewModel.generatedImageBitmap.value = mock()

        // 이벤트를 수집할 리스트 준비
        val noImageEvents = mutableListOf<Boolean>()
        val job = launch {
            viewModel.noImage.collect { event ->
                noImageEvents.add(event)
            }
        }

        // When: 이미지 클리어 함수 호출
        viewModel.clearSelectedImage()
        advanceUntilIdle() // 코루틴 작업이 완료될 때까지 대기

        // Then: 모든 이미지 관련 상태값이 null로 초기화되었는지 검증
        assertNull(viewModel.selectedImageUri.value)
        assertNull(viewModel.existingImageUrl.value)
        assertNull(viewModel.generatedImageBitmap.value)

        // Then: noImage 이벤트가 true로 한 번 방출되었는지 검증
        assertEquals(1, noImageEvents.size)
        assertTrue(noImageEvents.first())

        job.cancel() // 테스트 후 코루틴 정리
    }

    // ... JournalEditViewModelTest.kt의 다른 테스트들 ...

    @Test
    fun `generateImage - 성공 시 - Base64를 디코딩하여 Bitmap을 생성하고 상태를 업데이트한다`() = runTest {
        // Given: UseCase가 가짜 Base64 문자열을 반환하도록 설정
        val fakeBase64Image = "fake-base64-string-for-edit"
        whenever(generateImageUseCase.invoke(any(), any())).thenReturn(fakeBase64Image)

        // Given: 안드로이드 프레임워크 Mocking
        val mockBitmap: Bitmap = mock()
        val fakeImageBytes = "image-bytes-for-edit".toByteArray()

        // mockStatic 블록 안에서만 static 메서드 mocking이 유효함
        mockStatic(android.util.Base64::class.java).use { base64 ->
            mockStatic(android.graphics.BitmapFactory::class.java).use { bitmapFactory ->
                // Base64.decode가 호출되면 fakeImageBytes를 반환하도록 설정
                base64.`when`<ByteArray> { android.util.Base64.decode(fakeBase64Image, android.util.Base64.DEFAULT) }.thenReturn(fakeImageBytes)
                // BitmapFactory.decodeByteArray가 호출되면 mockBitmap을 반환하도록 설정
                bitmapFactory.`when`<Bitmap> { android.graphics.BitmapFactory.decodeByteArray(fakeImageBytes, 0, fakeImageBytes.size) }.thenReturn(mockBitmap)

                // Given: 이미지 생성을 위한 텍스트 내용 및 기존 이미지 상태 설정
                viewModel.content.value = "AI 그림 생성 테스트 (수정)"
                viewModel.existingImageUrl.value = "http://example.com/original.jpg" // 기존 이미지가 있었다고 가정

                // When: 이미지 생성 함수 호출
                viewModel.generateImage(style = "pixel-art")
                advanceUntilIdle()

                // Then: UseCase가 올바른 인자로 호출되었는지 검증
                verify(generateImageUseCase, times(1)).invoke("pixel-art", "AI 그림 생성 테스트 (수정)")

                // Then: AI 이미지가 생성되고 다른 이미지 상태는 null로 초기화되었는지 검증
                assertEquals(mockBitmap, viewModel.generatedImageBitmap.value) // 새 Bitmap이 할당됨
                assertNull(viewModel.selectedImageUri.value)
                assertNull(viewModel.existingImageUrl.value) // 기존 이미지 URL이 null로 초기화됨

                // Then: 로딩 상태가 시작했다가 종료되었는지 검증
                assertFalse(viewModel.isLoading.value)
            }
        }
    }

    @Test
    fun `generateImage - 내용이 비어있으면 에러를 방출한다`() = runTest {
        // Given
        viewModel.title.value = ""
        viewModel.content.value = ""

        val errors = mutableListOf<String>()
        val job = launch {
            viewModel.aiGenerationError.collect { errors.add(it) }
        }

        // When
        viewModel.generateImage("any-style")
        advanceUntilIdle()

        // Then
        verify(generateImageUseCase, never()).invoke(any(), any())
        assertEquals(1, errors.size)
        assertEquals("이미지를 생성하려면 내용이나 제목을 먼저 입력해주세요.", errors.first())

        job.cancel()
    }

    @Test
    fun `updateJournal - AI 생성 이미지가 있을 때 - uploadJournalImageUseCase가 호출된다`() = runTest {
        // Given: ViewModel 상태 초기화
        whenever(getJournalByIdUseCase.invoke(1)).thenReturn(dummyJournalEntry)
        viewModel.loadJournalDetails(1)
        advanceUntilIdle()

        // Given: AI 이미지 비트맵 설정
        val mockBitmap: Bitmap = mock()
        // bitmap.compress가 항상 true를 반환하도록 설정
        whenever(mockBitmap.compress(any(), any(), any())).thenReturn(true)
        viewModel.generatedImageBitmap.value = mockBitmap

        // When
        viewModel.updateJournal()
        advanceUntilIdle()

        // Then: 이미지가 변경되었으므로 uploadJournalImageUseCase가 호출되어야 함
        verify(uploadJournalImageUseCase, times(1)).invoke(
            journalId = eq(1),
            imageBytes = any(), // ByteArrayOutputStream을 통해 생성된 byte 배열
            contentType = eq("image/jpeg"),
            fileName = any()
        )
        // 텍스트는 변경되지 않았으므로 updateJournalUseCase는 호출되지 않아야 함
        verify(updateJournalUseCase, never()).invoke(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `generateImage - 실패 시 - 에러 메시지와 noImage 이벤트를 방출한다`() = runTest {
        // Given: UseCase가 예외를 던지도록 설정
        val errorMessage = "AI 서버 연결 실패"
        whenever(generateImageUseCase.invoke(any(), any())).thenThrow(RuntimeException(errorMessage))

        viewModel.content.value = "AI 그림 생성 테스트"

        // 이벤트를 수집할 리스트 준비
        val errors = mutableListOf<String>()
        val noImageEvents = mutableListOf<Boolean>()
        val errorJob = launch { viewModel.aiGenerationError.collect { errors.add(it) } }
        val noImageJob = launch { viewModel.noImage.collect { noImageEvents.add(it) } }

        // When: 이미지 생성 함수 호출
        viewModel.generateImage("any-style")
        advanceUntilIdle()

        // Then: 에러와 noImage 이벤트가 올바르게 방출되었는지 검증
        assertEquals(1, errors.size)
        assertEquals(errorMessage, errors.first())
        assertEquals(1, noImageEvents.size)
        assertTrue(noImageEvents.first())

        // Then: 로딩 상태가 false로 돌아왔는지 검증
        assertFalse(viewModel.isLoading.value)

        // 사용한 코루틴 잡 정리
        errorJob.cancel()
        noImageJob.cancel()
    }
}
