package com.example.voicetutor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicetutor.ui.components.*
import com.example.voicetutor.ui.theme.*
import com.example.voicetutor.ui.viewmodel.AssignmentViewModel

/**
 * 선생님 과제 결과 화면
 *
 * @param assignmentViewModel 과제 관련 ViewModel (테스트용으로 주입 가능)
 * @param assignmentId 과제 ID
 * @param assignmentTitle 과제 제목 (ID가 없을 때 사용, 하위 호환성을 위해 유지)
 * @param onNavigateToStudentDetail 학생 상세 화면으로 이동하는 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAssignmentResultsScreen(
    assignmentViewModel: AssignmentViewModel? = null,
    assignmentId: Int = 0,
    assignmentTitle: String? = null,
    onNavigateToStudentDetail: (studentId: String, assignmentId: Int, assignmentTitle: String) -> Unit = { _, _, _ -> },
) {
    val viewModel: AssignmentViewModel = assignmentViewModel ?: hiltViewModel()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val students by viewModel.assignmentResults.collectAsStateWithLifecycle()
    val currentAssignment by viewModel.currentAssignment.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val targetAssignment = remember(assignments, assignmentId, assignmentTitle) {
        if (assignmentId > 0) {
            assignments.find { it.id == assignmentId }
        } else if (assignmentTitle != null) {
            assignments.find {
                it.title == assignmentTitle ||
                    "${it.courseClass.subject.name} - ${it.title}" == assignmentTitle ||
                    assignmentTitle.contains(it.title)
            }
        } else {
            null
        }
    }

    val dynamicAssignmentTitle = currentAssignment?.title ?: (targetAssignment?.title ?: assignmentTitle ?: "과제")
    val resolvedAssignmentId = targetAssignment?.id ?: currentAssignment?.id ?: assignmentId

    LaunchedEffect(assignmentId, targetAssignment?.id) {
        if (assignmentId > 0) {
            println("TeacherAssignmentResults - Loading assignment by ID: $assignmentId")
            viewModel.loadAssignmentById(assignmentId)
            viewModel.loadAssignmentStudentResults(assignmentId)
        } else {
            targetAssignment?.let { target ->
                println("TeacherAssignmentResults - Loading assignment: ${target.title} (ID: ${target.id})")
                viewModel.loadAssignmentById(target.id)
                viewModel.loadAssignmentStudentResults(target.id)
            }
        }
    }

    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = PrimaryIndigo.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(16.dp),
        ) {
            Column {
                Text(
                    text = dynamicAssignmentTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray800,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "학생별 과제 결과를 확인하고 피드백을 제공하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray600,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VTStatsCard(
                title = "제출 학생",
                value = "${students.count { it.status == "완료" }}명",
                icon = Icons.Filled.CheckCircle,
                iconColor = Success,
                modifier = Modifier.weight(1f),
                variant = CardVariant.Gradient,
            )

            VTStatsCard(
                title = "평균 점수",
                value = if (students.isNotEmpty() && students.any { it.status == "완료" }) {
                    students.filter { it.status == "완료" }.map { it.score }.average().toInt().toString()
                } else {
                    "-"
                },
                icon = Icons.Filled.Star,
                iconColor = Warning,
                modifier = Modifier.weight(1f),
                variant = CardVariant.Gradient,
            )
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "학생별 결과",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray800,
                )

                Text(
                    text = "총 ${students.size}명",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    LoadingIndicator()
                }
                students.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Person,
                    )
                }
                else -> {
                    students.forEachIndexed { index, student ->
                        TeacherAssignmentResultCard(
                            student = student,
                            onStudentClick = {
                                val destinationAssignmentId = resolvedAssignmentId
                                if (destinationAssignmentId != 0) {
                                    onNavigateToStudentDetail(student.studentId, destinationAssignmentId, dynamicAssignmentTitle)
                                }
                            },
                        )

                        if (index < students.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = PrimaryIndigo,
        )
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Gray400,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "제출된 과제가 없습니다",
                style = MaterialTheme.typography.bodyLarge,
                color = Gray600,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TeacherAssignmentResultsScreenPreview() {
    VoiceTutorTheme {
        TeacherAssignmentResultsScreen()
    }
}
