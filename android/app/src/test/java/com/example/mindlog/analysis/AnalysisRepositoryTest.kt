package com.example.mindlog.analysis

import com.example.mindlog.core.common.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.analysis.data.api.AnalysisApi
import com.example.mindlog.features.analysis.data.dto.ComprehensiveAnalysisResponse
import com.example.mindlog.features.analysis.data.dto.PersonalizedAdviceResponse
import com.example.mindlog.features.analysis.data.dto.UserTypeResponse
import com.example.mindlog.features.analysis.data.mapper.AnalysisMapper
import com.example.mindlog.features.analysis.data.repository.AnalysisRepositoryImpl
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.kotlin.verifyNoInteractions

class AnalysisRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var api: AnalysisApi
    private lateinit var mapper: AnalysisMapper
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var repo: AnalysisRepositoryImpl

    @Before
    fun setup() {
        api = mock(AnalysisApi::class.java)
        mapper = mock(AnalysisMapper::class.java)
        dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher)
        repo = AnalysisRepositoryImpl(api, mapper, dispatcherProvider)
    }

    // ----------------------------------------------------
    // getUserType
    // ----------------------------------------------------
    @Test
    fun `getUserType success maps to domain`() = runTest {
        // given
        val dto = mock(UserTypeResponse::class.java)
        val domain = mock(UserType::class.java)

        `when`(api.getUserType()).thenReturn(dto)
        `when`(mapper.toUserType(dto)).thenReturn(domain)

        // when
        val res = repo.getUserType()

        // then
        assertTrue(res is Result.Success)
        res as Result.Success
        assertEquals(domain, res.data)

        verify(api).getUserType()
        verify(mapper).toUserType(dto)
    }

    @Test
    fun `getUserType error wraps as Result_Error and mapper not called`() = runTest {
        // given
        `when`(api.getUserType()).thenThrow(RuntimeException("boom"))

        // when
        val res = repo.getUserType()

        // then
        assertTrue(res is Result.Error)
        res as Result.Error
        assertTrue(res.message?.contains("boom") == true)

        verify(api).getUserType()
        verifyNoInteractions(mapper)
    }

    // ----------------------------------------------------
    // getComprehensiveAnalysis
    // ----------------------------------------------------
    @Test
    fun `getComprehensiveAnalysis success maps to domain`() = runTest {
        // given
        val dto = mock(ComprehensiveAnalysisResponse::class.java)
        val domain = mock(ComprehensiveAnalysis::class.java)

        `when`(api.getComprehensiveAnalysis()).thenReturn(dto)
        `when`(mapper.toComprehensive(dto)).thenReturn(domain)

        // when
        val res = repo.getComprehensiveAnalysis()

        // then
        assertTrue(res is Result.Success)
        res as Result.Success
        assertEquals(domain, res.data)

        verify(api).getComprehensiveAnalysis()
        verify(mapper).toComprehensive(dto)
    }

    @Test
    fun `getComprehensiveAnalysis error wraps as Result_Error and mapper not called`() = runTest {
        // given
        `when`(api.getComprehensiveAnalysis()).thenThrow(IllegalStateException("fail"))

        // when
        val res = repo.getComprehensiveAnalysis()

        // then
        assertTrue(res is Result.Error)
        res as Result.Error
        assertTrue(res.message?.contains("fail") == true)

        verify(api).getComprehensiveAnalysis()
        verifyNoInteractions(mapper)
    }

    // ----------------------------------------------------
    // getPersonalizedAdvice
    // ----------------------------------------------------
    @Test
    fun `getPersonalizedAdvice success maps to domain`() = runTest {
        // given
        val dto = mock(PersonalizedAdviceResponse::class.java)
        val domain = mock(PersonalizedAdvice::class.java)

        `when`(api.getPersonalizedAdvice()).thenReturn(dto)
        `when`(mapper.toAdvice(dto)).thenReturn(domain)

        // when
        val res = repo.getPersonalizedAdvice()

        // then
        assertTrue(res is Result.Success)
        res as Result.Success
        assertEquals(domain, res.data)

        verify(api).getPersonalizedAdvice()
        verify(mapper).toAdvice(dto)
    }

    @Test
    fun `getPersonalizedAdvice error wraps as Result_Error and mapper not called`() = runTest {
        // given
        `when`(api.getPersonalizedAdvice()).thenThrow(RuntimeException("network"))

        // when
        val res = repo.getPersonalizedAdvice()

        // then
        assertTrue(res is Result.Error)
        res as Result.Error
        assertTrue(res.message?.contains("network") == true)

        verify(api).getPersonalizedAdvice()
        verifyNoInteractions(mapper)
    }
}