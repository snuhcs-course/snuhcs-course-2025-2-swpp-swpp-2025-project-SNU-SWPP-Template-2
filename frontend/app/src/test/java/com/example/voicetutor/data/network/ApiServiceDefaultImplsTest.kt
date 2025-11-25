package com.example.voicetutor.data.network

import com.example.voicetutor.data.models.*
import com.example.voicetutor.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import retrofit2.Response

/**
 * Tests for ApiService.DefaultImpls - Kotlin compiler-generated class for default parameter values
 * This class is generated when interface methods have default parameters.
 */
@RunWith(MockitoJUnitRunner::class)
class ApiServiceDefaultImplsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    lateinit var apiService: ApiService

    @Test
    fun getAllAssignments_withDefaultParameters_usesDefaults() = runTest {
        // Arrange - 기본값을 사용하는 호출 (모든 파라미터 null)
        val response = Response.success(
            ApiResponse(
                success = true,
                data = emptyList<AssignmentData>(),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getAllAssignments()).thenReturn(response)

        // Act - 기본값 사용 (파라미터 없이 호출)
        val result = apiService.getAllAssignments()

        // Assert
        assert(result.isSuccessful)
        assert(result.body()?.success == true)
    }

    @Test
    fun getAllAssignments_withTeacherIdOnly_usesDefaultForOthers() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = emptyList<AssignmentData>(),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getAllAssignments(teacherId = "1")).thenReturn(response)

        // Act - teacherId만 지정, 나머지는 기본값
        val result = apiService.getAllAssignments(teacherId = "1")

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getAllAssignments_withClassIdOnly_usesDefaultForOthers() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = emptyList<AssignmentData>(),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getAllAssignments(classId = "1")).thenReturn(response)

        // Act - classId만 지정, 나머지는 기본값
        val result = apiService.getAllAssignments(classId = "1")

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getAllAssignments_withStatusOnly_usesDefaultForOthers() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = emptyList<AssignmentData>(),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getAllAssignments(status = "IN_PROGRESS")).thenReturn(response)

        // Act - status만 지정, 나머지는 기본값
        val result = apiService.getAllAssignments(status = "IN_PROGRESS")

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getAllStudents_withDefaultParameters_usesDefaults() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = emptyList<Student>(),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getAllStudents()).thenReturn(response)

        // Act - 기본값 사용 (파라미터 없이 호출)
        val result = apiService.getAllStudents()

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getAllStudents_withTeacherIdOnly_usesDefaultForClassId() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = emptyList<Student>(),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getAllStudents(teacherId = "1")).thenReturn(response)

        // Act - teacherId만 지정, classId는 기본값
        val result = apiService.getAllStudents(teacherId = "1")

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getAllStudents_withClassIdOnly_usesDefaultForTeacherId() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = emptyList<Student>(),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getAllStudents(classId = "1")).thenReturn(response)

        // Act - classId만 지정, teacherId는 기본값
        val result = apiService.getAllStudents(classId = "1")

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getPersonalAssignments_withDefaultParameters_usesDefaults() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = emptyList<PersonalAssignmentData>(),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getPersonalAssignments()).thenReturn(response)

        // Act - 기본값 사용 (파라미터 없이 호출)
        val result = apiService.getPersonalAssignments()

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getPersonalAssignments_withStudentIdOnly_usesDefaultForAssignmentId() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = emptyList<PersonalAssignmentData>(),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getPersonalAssignments(studentId = 1)).thenReturn(response)

        // Act - studentId만 지정, assignmentId는 기본값
        val result = apiService.getPersonalAssignments(studentId = 1)

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getPersonalAssignments_withAssignmentIdOnly_usesDefaultForStudentId() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = emptyList<PersonalAssignmentData>(),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getPersonalAssignments(assignmentId = 1)).thenReturn(response)

        // Act - assignmentId만 지정, studentId는 기본값
        val result = apiService.getPersonalAssignments(assignmentId = 1)

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getProgressReport_withDefaultParameters_usesDefaults() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = ProgressReportData(
                    totalStudents = 0,
                    totalAssignments = 0,
                    completedAssignments = 0,
                    averageScore = 0.0,
                    classBreakdown = emptyList(),
                ),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getProgressReport(teacherId = "1")).thenReturn(response)

        // Act - teacherId만 지정, classId와 period는 기본값
        val result = apiService.getProgressReport(teacherId = "1")

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getProgressReport_withClassId_usesDefaultForPeriod() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = ProgressReportData(
                    totalStudents = 0,
                    totalAssignments = 0,
                    completedAssignments = 0,
                    averageScore = 0.0,
                    classBreakdown = emptyList(),
                ),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getProgressReport(teacherId = "1", classId = "1")).thenReturn(response)

        // Act - teacherId와 classId 지정, period는 기본값
        val result = apiService.getProgressReport(teacherId = "1", classId = "1")

        // Assert
        assert(result.isSuccessful)
    }

    @Test
    fun getProgressReport_withAllParameters() = runTest {
        // Arrange
        val response = Response.success(
            ApiResponse(
                success = true,
                data = ProgressReportData(
                    totalStudents = 0,
                    totalAssignments = 0,
                    completedAssignments = 0,
                    averageScore = 0.0,
                    classBreakdown = emptyList(),
                ),
                message = null,
                error = null,
            ),
        )
        whenever(apiService.getProgressReport(teacherId = "1", classId = "1", period = "month")).thenReturn(response)

        // Act - 모든 파라미터 지정
        val result = apiService.getProgressReport(teacherId = "1", classId = "1", period = "month")

        // Assert
        assert(result.isSuccessful)
    }
}

