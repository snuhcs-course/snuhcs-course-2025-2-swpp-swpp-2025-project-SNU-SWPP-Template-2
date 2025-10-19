package com.aisportspt.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.aisportspt.app.MainActivity
import com.aisportspt.app.databinding.FragmentAchievementBinding
import com.aisportspt.app.model.Achievement
import com.aisportspt.app.model.AchievementType
import com.aisportspt.app.model.UserStats
import com.aisportspt.app.ui.adapters.AchievementAdapter

class UserFragment : Fragment() {

    private var _binding: FragmentAchievementBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var achievementAdapter: AchievementAdapter
    private var allAchievements = listOf<Achievement>()
    private var currentFilter = "all"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFilterChips()
        observeViewModel()
        loadSampleAchievements()
    }

    private fun setupRecyclerView() {
        achievementAdapter = AchievementAdapter()
        
        binding.recyclerViewAchievements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = achievementAdapter
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener {
            filterAchievements("all")
        }
        
        binding.chipUnlocked.setOnClickListener {
            filterAchievements("unlocked")
        }
        
        binding.chipLocked.setOnClickListener {
            filterAchievements("locked")
        }
        
        // Set default selection
        binding.chipAll.isChecked = true
    }

    private fun observeViewModel() {
        val viewModel = (requireActivity() as MainActivity).getViewModel()
        
        viewModel.userStats.observe(viewLifecycleOwner, Observer { stats ->
            updateUserStats(stats)
        })
        
        viewModel.achievements.observe(viewLifecycleOwner, Observer { achievements ->
            allAchievements = achievements
            filterAchievements(currentFilter)
        })
    }

    private fun updateUserStats(stats: UserStats) {
        binding.apply {
            textLevel.text = "레벨 ${stats.level}"
            textXp.text = "${stats.xp} / ${stats.nextLevelXp} XP"
            
            val progressPercentage = ((stats.xp.toFloat() / stats.nextLevelXp) * 100).toInt()
            progressLevel.progress = progressPercentage
            
            textTotalSessions.text = stats.totalSessions.toString()
            textTotalHours.text = "${stats.totalHours}h"
            textStreakDays.text = stats.streakDays.toString()
            textAchievements.text = stats.totalAchievements.toString()
        }
    }

    private fun filterAchievements(filter: String) {
        currentFilter = filter
        
        // Update chip selection
        binding.chipAll.isChecked = filter == "all"
        binding.chipUnlocked.isChecked = filter == "unlocked"
        binding.chipLocked.isChecked = filter == "locked"
        
        val filteredAchievements = when (filter) {
            "unlocked" -> allAchievements.filter { it.unlockedAt != null }
            "locked" -> allAchievements.filter { it.unlockedAt == null }
            else -> allAchievements
        }
        
        updateAchievementsUI(filteredAchievements)
    }

    private fun updateAchievementsUI(achievements: List<Achievement>) {
        if (achievements.isEmpty()) {
            binding.recyclerViewAchievements.visibility = View.GONE
            binding.emptyAchievementsLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerViewAchievements.visibility = View.VISIBLE
            binding.emptyAchievementsLayout.visibility = View.GONE
            achievementAdapter.updateAchievements(achievements)
        }
    }

    private fun loadSampleAchievements() {
        val sampleAchievements = listOf(
            Achievement(
                id = "1",
                title = "첫 번째 골프 세션",
                description = "첫 번째 골프 연습 세션을 완료하세요",
                icon = "ic_golf",
                type = AchievementType.BRONZE,
                progress = 1,
                maxProgress = 1,
                sport = "골프",
                unlockedAt = System.currentTimeMillis() - 86400000, // 1 day ago
                rewardPoints = 50,
                badge = "first_golf"
            ),
            Achievement(
                id = "2",
                title = "주간 목표 달성",
                description = "주간 연습 목표를 달성하세요",
                icon = "ic_target",
                type = AchievementType.SILVER,
                progress = 2,
                maxProgress = 3,
                sport = "모든 스포츠",
                unlockedAt = null,
                rewardPoints = 100,
                badge = "weekly_goal"
            ),
            Achievement(
                id = "3",
                title = "연속 7일 연습",
                description = "7일 연속으로 연습하세요",
                icon = "ic_streak",
                type = AchievementType.GOLD,
                progress = 5,
                maxProgress = 7,
                sport = "모든 스포츠",
                unlockedAt = null,
                rewardPoints = 200,
                badge = "streak_7"
            ),
            Achievement(
                id = "4",
                title = "AI 코치 마스터",
                description = "AI 코치 기능을 50회 사용하세요",
                icon = "ic_ai",
                type = AchievementType.PLATINUM,
                progress = 12,
                maxProgress = 50,
                sport = "모든 스포츠",
                unlockedAt = null,
                rewardPoints = 500,
                badge = "ai_master"
            ),
            Achievement(
                id = "5",
                title = "볼링 초보자",
                description = "첫 번째 볼링 연습 세션을 완료하세요",
                icon = "ic_bowling",
                type = AchievementType.BRONZE,
                progress = 1,
                maxProgress = 1,
                sport = "볼링",
                unlockedAt = System.currentTimeMillis() - 172800000, // 2 days ago
                rewardPoints = 50,
                badge = "first_bowling"
            )
        )
        
        // Update the ViewModel with sample achievements
        val viewModel = (requireActivity() as MainActivity).getViewModel()
        // Since we don't have a method to set achievements directly, we'll store them locally
        allAchievements = sampleAchievements
        filterAchievements(currentFilter)
        
        // Update user stats to reflect achievements
        val unlockedCount = sampleAchievements.count { it.unlockedAt != null }
        // You would typically update this through the ViewModel
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}