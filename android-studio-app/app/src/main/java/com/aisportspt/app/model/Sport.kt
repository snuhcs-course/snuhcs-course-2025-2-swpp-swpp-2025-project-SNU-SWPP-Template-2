package com.aisportspt.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Sport(
    val id: String,
    val name: String,
    val imageUrl: String,
    val totalSessions: Int,
    val weeklyGoal: Int,
    val currentWeekSessions: Int,
    val lastSession: String,
    val skillLevel: String,
    val nextGoal: String
) : Parcelable

@Parcelize
data class Session(
    val id: String,
    val sportId: String,
    val date: String,
    val duration: Int, // 분 단위
    val satisfaction: Int, // 1-10
    val focus: String,
    val week:Int,
    val intensity: String, // light, moderate, high
    val notes: String
) : Parcelable

@Parcelize
data class Schedule(
    val id: String,
    val sportId:String,
    val date:String,
    val startTime:String, // hh:mm
    val finishTime:String,
    val session: Session,
    var isFinished:Boolean
): Parcelable
@Parcelize
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val type: AchievementType,
    val progress: Int,
    val maxProgress: Int,
    val sport: String,
    val unlockedAt: Long? = null,
    val rewardPoints: Int,
    val badge: String? = null
) : Parcelable

enum class AchievementType {
    BRONZE, SILVER, GOLD, PLATINUM
}

@Parcelize
data class UserStats(
    val totalSessions: Int,
    val totalHours: Int,
    val streakDays: Int,
    val level: Int,
    val xp: Int,
    val nextLevelXp: Int,
    val totalAchievements: Int
) : Parcelable

@Parcelize
data class User(
    val id:String,
    val selectedSport:Sport,
    val schedules:HashSet<Schedule>,
    val userStat: UserStats,
    val mySports: HashSet<Sport>,
    val workDates:HashSet<String>,
    var selectedDate: String,
    val currentWeek: Int
): Parcelable

@Parcelize
data class TrainingPlan(
    val id: String,
    val sport: String,
    val name: String,
    val duration: String,
    val difficulty: Difficulty,
    val focus: String,
    val exercises: List<Exercise>,
    val aiGenerated: Boolean
) : Parcelable

@Parcelize
data class Exercise(
    val id: String,
    val name: String,
    val duration: Int,
    val sets: Int? = null,
    val reps: Int? = null,
    val description: String,
    val videoUrl: String? = null
) : Parcelable

enum class Difficulty {
    BEGINNER, INTERMEDIATE, ADVANCED
}

@Parcelize
data class AIPoseFeedback(
    val message: String,
    val confidence: Int,
    val type: String, // "good", "warning", "error"
    val timestamp: Long
) : Parcelable

@Parcelize
data class PoseAnalysisResult(
    val accuracy: Int,
    val phase: String,
    val suggestions: List<String>,
    val keyPoints: List<PoseKeyPoint>
) : Parcelable

@Parcelize
data class PoseKeyPoint(
    val name: String,
    val x: Float,
    val y: Float,
    val confidence: Float
) : Parcelable

@Parcelize
data class SportEquipment(
    val id: String,
    val name: String,
    val category: String,
    val brand: String,
    val price: String,
    val imageUrl: String,
    val rating: Float,
    val description: String,
    val sportId: String
) : Parcelable

@Parcelize
data class SportsMate(
    val id: String,
    val name: String,
    val profileImageUrl: String?,
    val sport: String,
    val skillLevel: String,
    val location: String,
    val distance: String,
    val lastActive: String,
    val commonInterests: List<String>
) : Parcelable