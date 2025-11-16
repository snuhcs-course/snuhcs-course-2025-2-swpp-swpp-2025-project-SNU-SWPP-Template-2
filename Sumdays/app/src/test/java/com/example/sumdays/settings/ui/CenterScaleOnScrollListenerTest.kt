package com.example.sumdays.settings.ui

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk // <- 이 import가 추가되었습니다.
import io.mockk.slot  // <- 이 import가 추가되었습니다.
import io.mockk.verify
import org.junit.Assert.assertEquals // <- 이 import가 추가되었습니다.
import org.junit.Before
import org.junit.Test

class CenterScaleOnScrollListenerTest {

    // 테스트 대상 클래스
    private lateinit var listener: CenterScaleOnScrollListener

    // 모의(Mock) 객체
    @MockK
    private lateinit var recyclerView: RecyclerView
    @MockK
    private lateinit var layoutManager: LinearLayoutManager

    // relaxUnitFun = true: scaleX/scaleY setter가 Unit을 반환하므로 모의 처리 간소화
    @MockK(relaxUnitFun = true)
    private lateinit var child1: View

    // 테스트용 상수
    private val RV_WIDTH = 1000 // RecyclerView 너비 (가운데: 500)
    private val SCALE_MAX = 1.0f
    private val SCALE_MIN = 0.92f
    // 부동소수점 비교를 위한 허용 오차
    private val DELTA = 0.0001f

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        listener = CenterScaleOnScrollListener()

        // 대부분의 테스트에서 공통적으로 사용될 모의 설정
        every { recyclerView.layoutManager } returns layoutManager
        every { recyclerView.width } returns RV_WIDTH
    }

    /**
     * 시나리오 1: 아이템이 RecyclerView의 정가운데에 위치할 때
     * 기대값: 스케일이 최대 (1.0f)가 되어야 함
     */
    @Test
    fun `onScrolled - child at exact center - scales to MAX`() {
        // given
        every { child1.left } returns 450
        every { child1.right } returns 550

        every { recyclerView.childCount } returns 1
        every { recyclerView.getChildAt(0) } returns child1

        // when
        listener.onScrolled(recyclerView, 0, 0)

        // then
        // 1.0f는 정확하므로 eq() 사용 가능
        verify { child1.scaleX = SCALE_MAX }
        verify { child1.scaleY = SCALE_MAX }
    }

    /**
     * 시나리오 2: 아이템이 왼쪽 가장자리에 위치할 때
     * (수정됨): eq() 대신 slot()과 assertEquals(..., delta) 사용
     */
    @Test
    fun `onScrolled - child at left edge - scales to MIN`() {
        // given
        every { child1.left } returns 0
        every { child1.right } returns 100

        every { recyclerView.childCount } returns 1
        every { recyclerView.getChildAt(0) } returns child1

        // 값을 캡처할 'slot' 생성
        val scaleXSlot = slot<Float>()
        val scaleYSlot = slot<Float>()

        // when
        listener.onScrolled(recyclerView, 0, 0)

        // then
        // d = 0.9, scale = 1.0 - (0.08 * 0.9) = 1.0 - 0.072 = 0.928
        val expectedScale = 0.928f

        // verify가 호출될 때 값을 캡처
        verify { child1.scaleX = capture(scaleXSlot) }
        verify { child1.scaleY = capture(scaleYSlot) }

        // 캡처된 값을 assertEquals와 delta(오차범위)로 비교
        assertEquals(expectedScale, scaleXSlot.captured, DELTA)
        assertEquals(expectedScale, scaleYSlot.captured, DELTA)
    }

    /**
     * 시나리오 3: 아이템이 오른쪽 가장자리에 위치할 때
     * (수정됨): eq() 대신 slot()과 assertEquals(..., delta) 사용
     */
    @Test
    fun `onScrolled - child at right edge - scales to MIN`() {
        // given
        every { child1.left } returns 900
        every { child1.right } returns 1000

        every { recyclerView.childCount } returns 1
        every { recyclerView.getChildAt(0) } returns child1

        val scaleXSlot = slot<Float>()
        val scaleYSlot = slot<Float>()

        // when
        listener.onScrolled(recyclerView, 0, 0)

        // then
        val expectedScale = 0.928f

        verify { child1.scaleX = capture(scaleXSlot) }
        verify { child1.scaleY = capture(scaleYSlot) }

        assertEquals(expectedScale, scaleXSlot.captured, DELTA)
        assertEquals(expectedScale, scaleYSlot.captured, DELTA)
    }

    /**
     * 시나리오 3-2: 아이템이 화면 밖 (coerceIn 테스트)
     */
    @Test
    fun `onScrolled - child way off screen - scales to MIN`() {
        // given
        every { child1.left } returns 1450
        every { child1.right } returns 1550

        every { recyclerView.childCount } returns 1
        every { recyclerView.getChildAt(0) } returns child1

        // when
        listener.onScrolled(recyclerView, 0, 0)

        // then
        // 0.92f는 정확하므로 eq() 사용 가능
        val expectedScale = SCALE_MIN
        verify { child1.scaleX = expectedScale }
        verify { child1.scaleY = expectedScale }
    }

    /**
     * 시나리오 4: 여러 아이템이 있을 때 각각 독립적으로 스케일링
     * (수정됨): 로컬 mock을 mockk()로 직접 생성
     */
    @Test
    fun `onScrolled - multiple children - scale independently`() {
        // given
        // (수정) @MockK lateinit 대신 mockk()로 직접 생성
        val childCenter: View = mockk(relaxUnitFun = true)
        val childEdge: View = mockk(relaxUnitFun = true)

        // childCenter: 중앙 (스케일 1.0)
        every { childCenter.left } returns 450
        every { childCenter.right } returns 550 // mid = 500

        // childEdge: 가장자리 (스케일 0.928)
        every { childEdge.left } returns 0
        every { childEdge.right } returns 100 // mid = 50

        every { recyclerView.childCount } returns 2
        every { recyclerView.getChildAt(0) } returns childCenter
        every { recyclerView.getChildAt(1) } returns childEdge

        // (수정) childEdge의 값을 캡처할 slot
        val edgeScaleSlot = slot<Float>()

        // when
        listener.onScrolled(recyclerView, 0, 0)

        // then
        // 중앙 아이템 검증
        verify { childCenter.scaleX = SCALE_MAX }
        verify { childCenter.scaleY = SCALE_MAX }

        // 가장자리 아이템 검증 (시나리오 2와 동일)
        val expectedEdgeScale = 0.928f
        verify { childEdge.scaleX = capture(edgeScaleSlot) }
        verify { childEdge.scaleY = capture(edgeScaleSlot) }
        assertEquals(expectedEdgeScale, edgeScaleSlot.captured, DELTA)
    }

    /**
     * 시나리오 5: LayoutManager가 LinearLayoutManager가 아닐 때 (가드 로직)
     */
    @Test
    fun `onScrolled - with non LinearLayoutManager - does nothing`() {
        // given
        // (수정) @MockK 대신 mockk()로 생성 (이 테스트에서는 상관없지만 일관성을 위해)
        val otherLayoutManager: RecyclerView.LayoutManager = mockk()
        every { recyclerView.layoutManager } returns otherLayoutManager

        // when
        listener.onScrolled(recyclerView, 0, 0)

        // then
        // childCount나 getChildAt 등이 아예 호출되지 않았는지 검증
        verify(exactly = 0) { recyclerView.childCount }
        verify(exactly = 0) { recyclerView.getChildAt(any()) }
    }

    /**
     * 시나리오 6: getChildAt이 null을 반환할 때 (가드 로직)
     */
    @Test
    fun `onScrolled - with null child - does not crash`() {
        // given
        every { recyclerView.childCount } returns 2
        every { recyclerView.getChildAt(0) } returns null // 0번 아이템이 null
        every { recyclerView.getChildAt(1) } returns child1 // 1번 아이템은 유효

        // 1번 아이템(child1)은 중앙으로 설정
        every { child1.left } returns 450
        every { child1.right } returns 550

        // when
        listener.onScrolled(recyclerView, 0, 0)

        // then
        // 1번 아이템(child1)은 정상적으로 스케일링되어야 함
        verify { child1.scaleX = SCALE_MAX }
        verify { child1.scaleY = SCALE_MAX }
        // 에러 없이 실행 완료
    }
}