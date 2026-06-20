package com.example.sumdays.statistics

import com.example.sumdays.R

/**
 * 통계 나무(가지) 배경의 단일 소스.
 * - StatisticsActivity: 스크롤하며 리프 인덱스 기반으로 크로스페이드
 * - StatisticsWidgetActivity: 현재 리프 개수에 맞는 배경 1장 표시
 * 두 화면이 같은 정의를 공유해 배경이 어긋나지 않도록 한다.
 */
object FoxTreeBackground {

    val backgrounds: List<Int> = listOf(
        R.drawable.statistics_background_morning,
        R.drawable.statistics_background_evening,
        R.drawable.statistics_background_stratosphere,
        R.drawable.statistics_background_space
    )

    // 배경 경계(리프 인덱스): ~10 아침, ~25 저녁, ~45 대기권, 45+ 우주
    // (개수 = backgrounds.size - 1 이어야 함)
    val boundaries: List<Float> = listOf(10f, 25f, 45f)

    /** 주어진 리프 개수가 속한 배경 인덱스(이산). 위젯 요약 카드용. */
    fun segmentIndexForLeaf(leafCount: Int): Int {
        var index = 0
        for (boundary in boundaries) {
            if (leafCount >= boundary) index++ else break
        }
        return index.coerceIn(0, backgrounds.lastIndex)
    }

    /** 다음 배경까지 남은 층수. 마지막(우주)에 도달했으면 null. */
    fun leavesToNextBackground(leafCount: Int): Int? {
        for (boundary in boundaries) {
            if (leafCount < boundary) return (boundary - leafCount).toInt()
        }
        return null
    }
}
