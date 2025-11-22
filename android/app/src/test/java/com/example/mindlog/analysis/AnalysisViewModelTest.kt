package com.example.mindlog.analysis

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import com.example.mindlog.features.analysis.domain.usecase.GetComprehensiveAnalysisUseCase
import com.example.mindlog.features.analysis.domain.usecase.GetPersonalizedAdviceUseCase
import com.example.mindlog.features.analysis.domain.usecase.GetUserTypeUseCase
import com.example.mindlog.features.analysis.presentation.AnalysisViewModel
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.times
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getUserTypeUseCase: GetUserTypeUseCase
    private lateinit var getComprehensiveAnalysisUseCase: GetComprehensiveAnalysisUseCase
    private lateinit var getPersonalizedAdviceUseCase: GetPersonalizedAdviceUseCase

    private lateinit var vm: AnalysisViewModel

    @Before
    fun setup() {
        val dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher)

        getUserTypeUseCase = mock()
        getComprehensiveAnalysisUseCase = mock()
        getPersonalizedAdviceUseCase = mock()

        vm = AnalysisViewModel(
            getUserTypeUseCase = getUserTypeUseCase,
            getComprehensiveAnalysisUseCase = getComprehensiveAnalysisUseCase,
            getPersonalizedAdviceUseCase = getPersonalizedAdviceUseCase,
            dispatchers = dispatcherProvider
        )
    }

    // ---------------------------
    // TEST 1: load() 성공 시 전체 상태 업데이트
    // ---------------------------
    @Test
    fun `load success updates all fields and clears error`() = runTest {
        // given
        val userType: UserType = mock()
        val comp: ComprehensiveAnalysis = mock()
        val advice: PersonalizedAdvice = mock()

        whenever(getUserTypeUseCase.invoke()).thenReturn(Result.Success(userType))
        whenever(getComprehensiveAnalysisUseCase.invoke()).thenReturn(Result.Success(comp))
        whenever(getPersonalizedAdviceUseCase.invoke()).thenReturn(Result.Success(advice))

        // when
        vm.load()
        advanceUntilIdle()

        // then
        val s = vm.state.value
        assertEquals(false, s.isLoading)
        assertNull(s.error)
        assertEquals(userType, s.userType)
        assertEquals(comp, s.comprehensiveAnalysis)
        assertEquals(advice, s.advice)

        // usecase가 각각 한 번씩 호출되었는지 검증
        verify(getUserTypeUseCase, times(1)).invoke()
        verify(getComprehensiveAnalysisUseCase, times(1)).invoke()
        verify(getPersonalizedAdviceUseCase, times(1)).invoke()
    }

    // ---------------------------
    // TEST 2: userType만 실패하는 경우
    // ---------------------------
    @Test
    fun `load error on userType keeps others and sets error`() = runTest {
        // given
        val comp: ComprehensiveAnalysis = mock()
        val advice: PersonalizedAdvice = mock()

        whenever(getUserTypeUseCase.invoke()).thenReturn(
            Result.Error(code = 500, message = "userType error")
        )
        whenever(getComprehensiveAnalysisUseCase.invoke()).thenReturn(Result.Success(comp))
        whenever(getPersonalizedAdviceUseCase.invoke()).thenReturn(Result.Success(advice))

        // when
        vm.load()
        advanceUntilIdle()

        // then
        val s = vm.state.value

        assertEquals(false, s.isLoading)
        // setError(message)에서 그대로 들어감
        assertEquals("userType error", s.error)

        // userType은 실패했기 때문에 null
        assertNull(s.userType)
        // 나머지는 성공 값 세팅
        assertEquals(comp, s.comprehensiveAnalysis)
        assertEquals(advice, s.advice)
    }

    // ---------------------------
    // TEST 3: 모든 UseCase 실패 시
    // ---------------------------
    @Test
    fun `load all error sets error and keeps all models null`() = runTest {
        // given
        whenever(getUserTypeUseCase.invoke()).thenReturn(
            Result.Error(code = 500, message = "userType error")
        )
        whenever(getComprehensiveAnalysisUseCase.invoke()).thenReturn(
            Result.Error(code = 500, message = "comp error")
        )
        whenever(getPersonalizedAdviceUseCase.invoke()).thenReturn(
            Result.Error(code = 500, message = "advice error")
        )

        // when
        vm.load()
        advanceUntilIdle()

        // then
        val s = vm.state.value

        assertEquals(false, s.isLoading)
        // 마지막 setError 호출(advice) 기준
        assertEquals("advice error", s.error)

        assertNull(s.userType)
        assertNull(s.comprehensiveAnalysis)
        assertNull(s.advice)
    }

    // ---------------------------
    // TEST 4: 초기 load() 시작 시 isLoading 플래그 true → 완료 시 false
    // (중간 상태까지 보장하진 않지만, 최종 값은 체크)
    // ---------------------------
    @Test
    fun `load toggles loading flag`() = runTest {
        val userType: UserType = mock()
        val comp: ComprehensiveAnalysis = mock()
        val advice: PersonalizedAdvice = mock()

        whenever(getUserTypeUseCase.invoke()).thenReturn(Result.Success(userType))
        whenever(getComprehensiveAnalysisUseCase.invoke()).thenReturn(Result.Success(comp))
        whenever(getPersonalizedAdviceUseCase.invoke()).thenReturn(Result.Success(advice))

        // when
        vm.load()

        // runTest + TestDispatcher라서 즉시 끝날 수도 있지만,
        // 최소한 최종 상태에서 isLoading=false인지만 확인
        advanceUntilIdle()

        val s = vm.state.value
        assertTrue(!s.isLoading)
    }
}