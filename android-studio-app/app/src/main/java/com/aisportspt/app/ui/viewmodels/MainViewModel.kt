package com.aisportspt.app.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aisportspt.app.model.Sport
import com.aisportspt.app.model.Session
import com.aisportspt.app.model.Achievement
import com.aisportspt.app.model.Schedule
import com.aisportspt.app.model.TrainingPlan
import com.aisportspt.app.model.User
import com.aisportspt.app.model.UserStats
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {
    
    // Sports
    private val _sports = MutableLiveData<List<Sport>>()
    val sports: LiveData<List<Sport>> = _sports
    
    // Sessions
    private val _sessions = MutableLiveData<List<Session>>()
    val sessions: LiveData<List<Session>> = _sessions
    
    // Achievements
    private val _achievements = MutableLiveData<List<Achievement>>()
    val achievements: LiveData<List<Achievement>> = _achievements
    
    // Training Plans
    private val _trainingPlans = MutableLiveData<List<TrainingPlan>>()
    val trainingPlans: LiveData<List<TrainingPlan>> = _trainingPlans
    
    // User Stats
    private val _userStats = MutableLiveData<UserStats>()
    val userStats: LiveData<UserStats> = _userStats

    private val _user= MutableLiveData<User>()

    val user: LiveData<User> = _user
    
    init {
        loadSampleData()
    }
    
    private fun loadSampleData() {
        // Sample sports data

        val sampleSports = listOf(
            Sport(
                id = "1",
                name = "골프",
                imageUrl = "https://images.unsplash.com/photo-1703293024102-44224053a305",
                totalSessions = 12,
                weeklyGoal = 3,
                currentWeekSessions = 1,
                lastSession = "2일 전",
                skillLevel = "중급",
                nextGoal = "핸디캡 15 달성"
            ),
            Sport(
                id = "2",
                name = "볼링",
                imageUrl = "https://images.unsplash.com/photo-1628139417027-c356ba05fe4f",
                totalSessions = 8,
                weeklyGoal = 2,
                currentWeekSessions = 2,
                lastSession = "4일 전",
                skillLevel = "초중급",
                nextGoal = "평균 140점 달성"
            )
        )
        _sports.value = sampleSports
        
        // Sample sessions data
        val sampleSessions = listOf(
            Session(
                id = "1",
                sportId = "1",
                date = getDateString(-2),
                duration = 90,
                satisfaction = 8,
                week=1,
                focus = "드라이버 연습",
                intensity = "moderate",
                notes = "백스윙 개선 필요"
            ),
            Session(
                id = "2",
                sportId = "2",
                date = getDateString(-4),
                duration = 60,
                satisfaction = 7,
                focus = "스페어 연습",
                week=1,
                intensity = "light",
                notes = "좋은 진전 있음"
            ),
            Session(
                id = "3",
                sportId = "1",
                date = getDateString(-1),
                duration = 75,
                satisfaction = 9,
                focus = "퍼팅 연습",
                week= 2,
                intensity = "moderate",
                notes = "컨디션 좋았음"
            ),
            Session(
                id = "4",
                sportId = "1",
                date = getDateString(-1),
                duration = 75,
                satisfaction = 9,
                focus = "스윙 활용",
                week= 3,
                intensity = "moderate",
                notes = "컨디션 좋았음"
            ),
            Session(
                id = "5",
                sportId = "1",
                date = getDateString(-1),
                duration = 75,
                satisfaction = 9,
                focus = "퍼팅 실전",
                week= 4,
                intensity = "moderate",
                notes = "컨디션 좋았음"
            )
        )
        _sessions.value = sampleSessions
        
        // Sample user stats
        _userStats.value = UserStats(
            totalSessions = sampleSessions.size,
            totalHours = sampleSessions.sumOf { it.duration } / 60,
            streakDays = 5,
            level = 8,
            xp = 2340,
            nextLevelXp = 3000,
            totalAchievements = 3
        )
        val calendar=Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateStr = sdf.format(calendar.time)
        _user.value=User("admin", sampleSports[0],HashSet(),_userStats.value!!,HashSet(), HashSet(),selectedDateStr,0)
        _user.value!!.mySports.add(sampleSports[0])
        
        // Sample achievements
        loadSampleAchievements()
        
        // Empty training plans initially
        _trainingPlans.value = emptyList()
    }
    
    private fun loadSampleAchievements() {
        // TODO: Load achievements from repository
        _achievements.value = emptyList()
    }
    
    private fun getDateString(daysAgo: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysAgo)
        return calendar.time.toString()
    }
    
    fun addSport(sport: Sport) {
        val currentSports = _sports.value?.toMutableList() ?: mutableListOf()
        currentSports.add(sport)
        _sports.value = currentSports
    }
    
    fun addSession(session: Session) {
        val currentSessions = _sessions.value?.toMutableList() ?: mutableListOf()
        currentSessions.add(0, session) // Add at beginning
        _sessions.value = currentSessions
        
        // Update sport stats
        updateSportStats(session)
        
        // Update user stats
        updateUserStats()
    }
    
    private fun updateSportStats(newSession: Session) {
        val currentSports = _sports.value?.toMutableList() ?: return
        val sportIndex = currentSports.indexOfFirst { it.id == newSession.sportId }
        
        if (sportIndex != -1) {
            val sport = currentSports[sportIndex]
            val updatedSport = sport.copy(
                totalSessions = sport.totalSessions + 1,
                currentWeekSessions = sport.currentWeekSessions + 1,
                lastSession = "방금 전"
            )
            currentSports[sportIndex] = updatedSport
            _sports.value = currentSports
        }
    }
    
    private fun updateUserStats() {
        val currentSessions = _sessions.value ?: return
        val currentStats = _userStats.value ?: return
        
        val updatedStats = currentStats.copy(
            totalSessions = currentSessions.size,
            totalHours = currentSessions.sumOf { it.duration } / 60
        )
        _userStats.value = updatedStats
    }
    
    fun getSportById(sportId: String): Sport? {
        return _sports.value?.find { it.id == sportId }
    }
    
    fun getSessionsForSport(sportId: String): List<Session> {
        return _sessions.value?.filter { it.sportId == sportId } ?: emptyList()
    }
    
    fun addTrainingPlan(plan: TrainingPlan) {
        val currentPlans = _trainingPlans.value?.toMutableList() ?: mutableListOf()
        currentPlans.add(plan)
        _trainingPlans.value = currentPlans
    }
    fun getSessionForUser(week:Int):Session{
        //TODO: fetch
        return getSessionsForSport(
            _user.value!!.selectedSport.id)[
            _user.value!!.selectedSport.currentWeekSessions+week-1]
    }
    fun createTrainingPlan(selectedDays:List<String>,selectedTimeSlot:String,selectedFinishTime:String){
        for(date in selectedDays){
            val calendar= android.icu.util.Calendar.getInstance()
            val today=calendar.get(android.icu.util.Calendar.DAY_OF_WEEK)
            val dayOfWeek= koreanToWeekday(date)
            for(i in 0..3) {
                val cal = calendar.clone() as android.icu.util.Calendar
                cal.add(android.icu.util.Calendar.DAY_OF_MONTH, i * 7 + (dayOfWeek-today + 7) % 7)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDate = sdf.format(cal.time)
                _user.value!!.schedules.add(
                    Schedule(
                        _user.value!!.id,
                        _user.value!!.selectedSport.id,
                        selectedDate,
                        selectedTimeSlot,
                        selectedFinishTime,
                        getSessionForUser(i),
                        false
                    )
                )
                //TODO: add schedule in server
                Log.d("schedule","schedule create,$selectedDate")
                _user.value!!.workDates.add(selectedDate)
            }
        }
    }
    fun modifyTrainingPlan(selectedDateFinal:String,selectedTimeSlot:String){
        //TODO: add new schedule in server and delete existing schedule

        val schedule=_user.value!!.schedules.find{ it.date == _user.value!!.selectedDate }
        val week=schedule!!.session.week
        _user.value!!.schedules.remove(schedule)
        _user.value!!.schedules.add(Schedule(_user.value!!.id,_user.value!!.selectedSport.id,selectedDateFinal,selectedTimeSlot,selectedTimeSlot, getSessionForUser(week),false))
        _user.value!!.workDates.add(selectedDateFinal)
        _user.value!!.workDates.remove(_user.value!!.selectedDate)
    }
    fun koreanToWeekday(text:String):Int{
        return when (text) {
            "일요일" -> android.icu.util.Calendar.SUNDAY    // 1
            "월요일" -> android.icu.util.Calendar.MONDAY    // 2
            "화요일" -> android.icu.util.Calendar.TUESDAY   // 3
            "수요일" -> android.icu.util.Calendar.WEDNESDAY // 4
            "목요일" -> android.icu.util.Calendar.THURSDAY  // 5
            "금요일" -> android.icu.util.Calendar.FRIDAY    // 6
            "토요일" -> android.icu.util.Calendar.SATURDAY  // 7
            else -> 0
        }
    }

}