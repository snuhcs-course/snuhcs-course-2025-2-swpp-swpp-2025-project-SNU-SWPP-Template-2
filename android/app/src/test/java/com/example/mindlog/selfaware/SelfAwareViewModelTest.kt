package com.example.mindlog.selfaware

import app.cash.turbine.test
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.selfaware.domain.model.Answer
import com.example.mindlog.features.selfaware.domain.model.CategoryScore
import com.example.mindlog.features.selfaware.domain.model.QAItem
import com.example.mindlog.features.selfaware.domain.model.Question
import com.example.mindlog.features.selfaware.domain.model.TopValueScores
import com.example.mindlog.features.selfaware.domain.model.ValueMap
import com.example.mindlog.features.selfaware.domain.model.ValueScore
import com.example.mindlog.features.selfaware.domain.usecase.GetTodayQAUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetTopValueScoresUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetValueMapUseCase
import com.example.mindlog.features.selfaware.domain.usecase.SubmitAnswerUseCase
import com.example.mindlog.features.selfaware.presentation.viewmodel.SelfAwareViewModel
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SelfAwareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getTodayQAUseCase: GetTodayQAUseCase
    private lateinit var submitAnswerUseCase: SubmitAnswerUseCase
    private lateinit var getValueMapUseCase: GetValueMapUseCase
    private lateinit var getTopValueScoresUseCase: GetTopValueScoresUseCase
    private lateinit var vm: SelfAwareViewModel

    @Before
    fun setup() {
        val testDispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher)

        getTodayQAUseCase = mock()
        submitAnswerUseCase = mock()
        getValueMapUseCase = mock()
        getTopValueScoresUseCase = mock()

        vm = SelfAwareViewModel(
            getTodayQAUseCase,
            submitAnswerUseCase,
            getValueMapUseCase,
            getTopValueScoresUseCase,
            testDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------
    // TEST 1: load() 성공 시 상태 확인
    // ---------------------------
    @Test
    fun `load success updates state (not submit answer)`() = runTest {
        // givens
        val testQA = QAItem(
            question = Question(
                id = 1,
                type = "single",
                text = "무엇이 중요했나요?",
                createdAt = LocalDate.now()
            ),
            answer = null
        )

        whenever(getValueMapUseCase.invoke(Unit)).thenReturn(Result.Success(testValueMap()))
        whenever(getTopValueScoresUseCase.invoke(Unit)).thenReturn(Result.Success(testTopValueScores()))
        whenever(getTodayQAUseCase.invoke(any())).thenReturn(Result.Success(testQA))

        // when
        vm.load()
        advanceUntilIdle()

        // thenss
        val s = vm.state.value
        assertFalse(s.isLoadingQuestion)
        assertFalse(s.isLoading)
        assertFalse(s.isAnsweredToday)
        assertEquals(5, s.valueMap.size)
        assertEquals(5, s.topValueScores.size)
    }

    @Test
    fun `load success updates state (submit answer)`() = runTest {
        // givens
        val testQA = QAItem(
            question = Question(
                id = 1,
                type = "single",
                text = "무엇이 중요했나요?",
                createdAt = LocalDate.now()
            ),
            answer = Answer(
                id = 10,
                questionId = 1,
                type = "type",
                text = "배움",
                createdAt = LocalDate.now(),
                updatedAt = LocalDate.now()
            )
        )

        whenever(getValueMapUseCase.invoke(Unit)).thenReturn(Result.Success(testValueMap()))
        whenever(getTopValueScoresUseCase.invoke(Unit)).thenReturn(Result.Success(testTopValueScores()))
        whenever(getTodayQAUseCase.invoke(any())).thenReturn(Result.Success(testQA))

        // when
        vm.load()
        advanceUntilIdle()

        // then
        val s = vm.state.value
        assertFalse(s.isLoadingQuestion)
        assertFalse(s.isLoading)
        assertTrue(s.isAnsweredToday)
        assertEquals(5, s.valueMap.size)
        assertEquals(5, s.topValueScores.size)
    }

    // ---------------------------
    // TEST 2: updateAnswerText()
    // ---------------------------
    @Test
    fun `updateAnswerText updates state`() = runTest {
        vm.updateAnswerText("테스트")
        assertEquals("테스트", vm.state.value.answerText)
    }

    // ---------------------------
    // TEST 3: submit() 성공 시
    // ---------------------------
    @Test
    fun `submit success sets answeredToday`() = runTest {
        // given
        // 질문 id 와 answerText 가 있어야 submit 통과
        vm.updateAnswerText("오늘의 답변")
        // questionId 주입을 위해 load 스텁 없이 직접 state 수정 (간단화)
        val field = SelfAwareViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(vm) as MutableStateFlow<SelfAwareViewModel.UiState>
        stateFlow.update { it.copy(questionId = 1) }

        whenever(submitAnswerUseCase.invoke(1, "오늘의 답변"))
            .thenReturn(Result.Success(Answer(
                id = 1,
                questionId = 1,
                type = null,
                text = "오늘의 답변",
                createdAt = LocalDate.now(),
                updatedAt = LocalDate.now()
            )))

        // when
        vm.submit()
        advanceUntilIdle()

        // then
        val s = vm.state.value
        assertTrue(s.isAnsweredToday)
        assertFalse(s.isSubmitting)
    }

    // ---------------------------
    // TEST 4: submit() 실패 시
    // ---------------------------
    @Test
    fun `submit error sets error message`() = runTest {
        vm.updateAnswerText("답변")
        // questionId 세팅
        val field = SelfAwareViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        val stateFlow = field.get(vm) as MutableStateFlow<SelfAwareViewModel.UiState>
        stateFlow.update { it.copy(questionId = 1) }

        whenever(submitAnswerUseCase.invoke(1, "답변")).thenReturn(
            Result.Error(404, "network error")
        )

        vm.submit()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals("network error", s.error)
        assertFalse(s.isAnsweredToday)
    }

    @Test
    fun `load() starts polling when today question is not ready, and stops when it arrives`() = runTest(mainDispatcherRule.testScheduler) {


        whenever(getValueMapUseCase.invoke(Unit)).thenReturn(Result.Success(testValueMap()))
        whenever(getTopValueScoresUseCase.invoke(Unit)).thenReturn(Result.Success(testTopValueScores()))
        // MP: SelfAwareViewModel의 pollTodayQuestion 기본 타이밍
        // initialDelayMs = 1500ms, interval = 2000ms (다음은 3000ms ...)

        // ① 첫 호출(load 내 동기 호출)은 "아직 없음"으로 응답 → 폴링 시작
        whenever(getTodayQAUseCase.invoke(any()))
            .thenReturn(Result.Error(code = null, message="timeout")) // load() 시점 반환
            .thenReturn(Result.Error(code = null, message="timeout")) // 폴링 1회차 (1.5s 후)
            .thenReturn(                     // 폴링 2회차 (추가 2.0s 후)
                Result.Success(
                    QAItem(
                        question = Question(
                            id = 1,
                            type = "single",
                            text = "무엇이 중요했나요?",
                            createdAt = LocalDate.now()
                        ),
                        answer = null
                    )
                )
            )


        // when: 로드 호출
        vm.load()
        advanceTimeBy(100L)
        verify(getTodayQAUseCase, times(1)).invoke(any())

        // then: 현재는 질문이 없어서 isLoadingQuestion = true, 폴링 대기 상태여야 함
        var s = vm.state.value
        assertTrue(s.isLoadingQuestion)
        assertNull(s.questionId)
        assertNull(s.questionText)

        // 가상 시간 1.5초 진행 → 폴링 1회차 트리거
        advanceTimeBy(1_500L)

        // 여전히 질문이 없다고 응답했으므로 계속 로딩 상태
        s = vm.state.value
        assertTrue(s.isLoadingQuestion)
        assertNull(s.questionId)

        // 추가 2.0초 진행 → 폴링 2회차 트리거(이때 질문 도착)
        advanceUntilIdle()

        // 이제 질문이 세팅되고 로딩 해제
        s = vm.state.value
        assertFalse(s.isLoadingQuestion)
        assertEquals(1, s.questionId)
        assertEquals("무엇이 중요했나요?", s.questionText)
        assertFalse(s.isAnsweredToday)

        // 검증: 총 3회 호출(로드 1 + 폴링 2)
        verify(getTodayQAUseCase, times(3)).invoke(any())
    }

    @Test
    fun `polling times out when question never arrives`() = runTest {
        whenever(getValueMapUseCase.invoke(Unit)).thenReturn(Result.Success(testValueMap()))
        whenever(getTopValueScoresUseCase.invoke(Unit)).thenReturn(Result.Success(testTopValueScores()))
         // 모든 호출이 "없음"
        whenever(getTodayQAUseCase.invoke(any()))
            .thenReturn(Result.Error(code = null, message="timeout"))

        vm.load()
        advanceUntilIdle()

        // pollTodayQuestion의 최대 대기시간은 60_000ms.
        // 초기 1.5초 + (2.0, 3.0, 4.5, 6.0, 6.0, ... <= 60s) 식으로 진행.
        // 여기서는 타임아웃까지 한 번에 진행:
        advanceTimeBy(60_000)
        advanceUntilIdle()

        val s = vm.state.value
        // 타임아웃 후 isLoadingQuestion=false, 에러 메시지 세팅
        assertFalse(s.isLoadingQuestion)
        assertTrue((s.error ?: "").contains("질문 생성이 지연되고 있어요."))
    }

    @Test
    fun `load() and fail to get today's question by some error`() = runTest {
        whenever(getValueMapUseCase.invoke(Unit)).thenReturn(Result.Success(testValueMap()))
        whenever(getTopValueScoresUseCase.invoke(Unit)).thenReturn(Result.Success(testTopValueScores()))
        // 모든 호출이 "없음"
        whenever(getTodayQAUseCase.invoke(any()))
            .thenReturn(Result.Error(code = 500, message="network error"))

        vm.load()
        advanceUntilIdle()

        val s = vm.state.value
        // 타임아웃 후 isLoadingQuestion=false, 에러 메시지 세팅
        assertFalse(s.isLoadingQuestion)
        assertEquals(s.questionId, null)
        assertEquals(s.questionText, "질문 생성에 문제가 있습니다. 잠시 후 다시 시도해주세요.")
        assertEquals(s.error, "network error")
    }

    @Test
    fun `load() and polling and fail to get today's question by some error`() = runTest {
        whenever(getValueMapUseCase.invoke(Unit)).thenReturn(Result.Success(testValueMap()))
        whenever(getTopValueScoresUseCase.invoke(Unit)).thenReturn(Result.Success(testTopValueScores()))
        // 모든 호출이 "없음"
        whenever(getTodayQAUseCase.invoke(any()))
            .thenReturn(Result.Error(code = null, message="timeout"))
            .thenReturn(Result.Error(code = 500, message="network error"))

        vm.load()
        advanceTimeBy(100L)
        verify(getTodayQAUseCase, times(1)).invoke(any())

        // then: 현재는 질문이 없어서 isLoadingQuestion = true, 폴링 대기 상태여야 함
        var s = vm.state.value
        assertTrue(s.isLoadingQuestion)
        assertNull(s.questionId)
        assertNull(s.questionText)

        // 가상 시간 1.5초 진행 → 폴링 1회차 트리거
        advanceTimeBy(1_500L)

        s = vm.state.value
        // 타임아웃 후 isLoadingQuestion=false, 에러 메시지 세팅
        assertFalse(s.isLoadingQuestion)
        assertEquals(s.questionId, null)
        assertEquals(s.questionText, "질문 생성에 문제가 있습니다. 잠시 후 다시 시도해주세요.")
        assertEquals(s.error, "network error")
    }

    private fun testValueMap() = ValueMap(
        categoryScores = listOf(
            CategoryScore("Neuroticism", "불안정성", 80),
            CategoryScore("Extroversion", "외향성", 60),
            CategoryScore("Openness","개방성", 50),
            CategoryScore("Agreeableness","수용성", 70),
            CategoryScore("Conscientiousness","성실성", 55),
        ),
        updatedAt = LocalDate.now()
    )

    private fun testTopValueScores() = TopValueScores(
        valueScores = listOf(
            ValueScore("성장", 80f),
            ValueScore("관계", 60f),
            ValueScore("가족", 90f),
            ValueScore("휴식", 60f),
            ValueScore("결혼",70f )
        )
    )
}