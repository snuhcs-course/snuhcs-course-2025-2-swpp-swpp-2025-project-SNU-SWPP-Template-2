package com.example.mindlog.features.statistics.domain.usecase

import com.example.mindlog.features.statistics.domain.repository.StatisticsRepository
import javax.inject.Inject

class GetEmotionRatesUseCase @Inject constructor(private val repo: StatisticsRepository) {
    suspend operator fun invoke(startDate: String, endDate: String) =
        repo.getEmotionRates(startDate, endDate)
}

class GetJournalStatisticsUseCase @Inject constructor(private val repo: StatisticsRepository) {
    suspend operator fun invoke(startDate: String, endDate: String) =
        repo.getJournalStatisics(startDate, endDate)
}