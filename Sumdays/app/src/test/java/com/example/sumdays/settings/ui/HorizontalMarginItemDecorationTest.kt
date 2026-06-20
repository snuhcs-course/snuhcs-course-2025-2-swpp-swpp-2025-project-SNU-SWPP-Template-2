package com.example.sumdays.settings.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HorizontalMarginItemDecorationTest {

    // 테스트에 사용할 고정 마진 값
    private val testMargin = 20

    // 테스트 대상 클래스 (Class Under Test)
    private lateinit var itemDecoration: HorizontalMarginItemDecoration

    // 모의(Mock) 객체들
    @MockK
    private lateinit var parent: RecyclerView
    @MockK
    private lateinit var view: View
    @MockK
    private lateinit var state: RecyclerView.State
    @MockK
    private lateinit var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>

    // 실제 값을 확인할 Rect 객체
    private lateinit var outRect: Rect

    @Before
    fun setUp() {
        // MockK 초기화
        MockKAnnotations.init(this)

        // 테스트 대상 클래스 인스턴스 생성
        itemDecoration = HorizontalMarginItemDecoration(testMargin)

        // 매번 새로운 Rect 객체 사용
        outRect = Rect()

        // 공통 모의 설정: parent가 adapter를 반환하도록 설정
        every { parent.adapter } returns adapter
    }

    /**
     * 시나리오 1: 리스트의 첫 번째 아이템 (총 3개 중)
     * 기대값: left = marginPx, right = marginPx / 2
     */
    @Test
    fun `getItemOffsets - first item in list`() {
        val position = 0
        val itemCount = 3

        // 모의 설정
        every { parent.getChildAdapterPosition(view) } returns position
        every { adapter.itemCount } returns itemCount

        // 실행
        itemDecoration.getItemOffsets(outRect, view, parent, state)

        // 검증
        assertEquals("첫 번째 아이템의 왼쪽 마진", testMargin, outRect.left)
        assertEquals("첫 번째 아이템의 오른쪽 마진", testMargin / 2, outRect.right)
        assertEquals("Top 마진은 0이어야 함", 0, outRect.top)
        assertEquals("Bottom 마진은 0이어야 함", 0, outRect.bottom)
    }

    /**
     * 시나리오 2: 리스트의 중간 아이템 (총 3개 중)
     * 기대값: left = marginPx / 2, right = marginPx / 2
     */
    @Test
    fun `getItemOffsets - middle item in list`() {
        val position = 1
        val itemCount = 3

        // 모의 설정
        every { parent.getChildAdapterPosition(view) } returns position
        every { adapter.itemCount } returns itemCount

        // 실행
        itemDecoration.getItemOffsets(outRect, view, parent, state)

        // 검증
        assertEquals("중간 아이템의 왼쪽 마진", testMargin / 2, outRect.left)
        assertEquals("중간 아이템의 오른쪽 마진", testMargin / 2, outRect.right)
    }

    /**
     * 시나리오 3: 리스트의 마지막 아이템 (총 3개 중)
     * 기대값: left = marginPx / 2, right = marginPx
     */
    @Test
    fun `getItemOffsets - last item in list`() {
        val position = 2
        val itemCount = 3

        // 모의 설정
        every { parent.getChildAdapterPosition(view) } returns position
        every { adapter.itemCount } returns itemCount

        // 실행
        itemDecoration.getItemOffsets(outRect, view, parent, state)

        // 검증
        assertEquals("마지막 아이템의 왼쪽 마진", testMargin / 2, outRect.left)
        assertEquals("마지막 아이템의 오른쪽 마진", testMargin, outRect.right)
    }

    /**
     * 시나리오 4: 리스트에 아이템이 하나만 있는 경우 (Edge Case)
     * 기대값: left = marginPx, right = marginPx
     */
    @Test
    fun `getItemOffsets - single item in list`() {
        val position = 0
        val itemCount = 1

        // 모의 설정
        every { parent.getChildAdapterPosition(view) } returns position
        every { adapter.itemCount } returns itemCount

        // 실행
        itemDecoration.getItemOffsets(outRect, view, parent, state)

        // 검증
        assertEquals("단일 아이템의 왼쪽 마진", testMargin, outRect.left)
        assertEquals("단일 아이템의 오른쪽 마진", testMargin, outRect.right)
    }

    /**
     * 시나리오 5: 어댑터가 null인 경우 (Edge Case)
     * 코드의 '?: 1' 엘비스 연산자 로직을 테스트합니다.
     * 이 경우 itemCount가 1로 간주되어 '단일 아이템'과 동일하게 동작해야 합니다.
     */
    @Test
    fun `getItemOffsets - adapter is null`() {
        val position = 0

        // 모의 설정
        every { parent.adapter } returns null // 어댑터가 null
        every { parent.getChildAdapterPosition(view) } returns position
        // adapter.itemCount는 호출되지 않음

        // 실행
        itemDecoration.getItemOffsets(outRect, view, parent, state)

        // 검증 (itemCount가 1일 때와 동일)
        assertEquals("Null 어댑터 시 왼쪽 마진", testMargin, outRect.left)
        assertEquals("Null 어댑터 시 오른쪽 마진", testMargin, outRect.right)
    }
}