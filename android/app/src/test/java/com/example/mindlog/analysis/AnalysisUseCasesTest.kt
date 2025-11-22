package com.example.mindlog.analysis

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import com.example.mindlog.features.analysis.domain.repository.AnalysisRepository
import com.example.mindlog.features.analysis.domain.usecase.GetComprehensiveAnalysisUseCase
import com.example.mindlog.features.analysis.domain.usecase.GetPersonalizedAdviceUseCase
import com.example.mindlog.features.analysis.domain.usecase.GetUserTypeUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.eq

class AnalysisUseCasesTest {

    private lateinit var repo: AnalysisRepository

    private lateinit var getUserTypeUseCase: GetUserTypeUseCase
    private lateinit var getComprehensiveAnalysisUseCase: GetComprehensiveAnalysisUseCase
    private lateinit var getPersonalizedAdviceUseCase: GetPersonalizedAdviceUseCase

    @Before
    fun setup() {
        repo = mock(AnalysisRepository::class.java)

        getUserTypeUseCase = GetUserTypeUseCase(repo)
        getComprehensiveAnalysisUseCase = GetComprehensiveAnalysisUseCase(repo)
        getPersonalizedAdviceUseCase = GetPersonalizedAdviceUseCase(repo)
    }

    // ------------------------------
    // GetUserTypeUseCase
    // ------------------------------
    @Test
    fun `getUserTypeUseCase delegates to repository`() = runTest {
        val dummyUserType = mock(UserType::class.java)
        val expected = Result.Success(dummyUserType)

        `when`(repo.getUserType()).thenReturn(expected)

        val result = getUserTypeUseCase()

        assertSame(expected, result)
        verify(repo).getUserType()
        verifyNoMoreInteractions(repo)
    }

    // ------------------------------
    // GetComprehensiveAnalysisUseCase
    // ------------------------------
    @Test
    fun `getComprehensiveAnalysisUseCase delegates to repository`() = runTest {
        val dummyComp = mock(ComprehensiveAnalysis::class.java)
        val expected = Result.Success(dummyComp)

        `when`(repo.getComprehensiveAnalysis()).thenReturn(expected)

        val result = getComprehensiveAnalysisUseCase()

        assertSame(expected, result)
        verify(repo).getComprehensiveAnalysis()
        verifyNoMoreInteractions(repo)
    }

    // ------------------------------
    // GetPersonalizedAdviceUseCase
    // ------------------------------
    @Test
    fun `getPersonalizedAdviceUseCase delegates to repository`() = runTest {
        val dummyAdvice = mock(PersonalizedAdvice::class.java)
        val expected = Result.Success(dummyAdvice)

        `when`(repo.getPersonalizedAdvice()).thenReturn(expected)

        val result = getPersonalizedAdviceUseCase()

        assertSame(expected, result)
        verify(repo).getPersonalizedAdvice()
        verifyNoMoreInteractions(repo)
    }
}