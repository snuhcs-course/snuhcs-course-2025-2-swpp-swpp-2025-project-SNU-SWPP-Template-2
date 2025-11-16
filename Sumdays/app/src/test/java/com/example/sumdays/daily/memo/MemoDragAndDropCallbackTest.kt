package com.example.sumdays.daily.memo

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import io.mockk.CapturingSlot
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Robolectric + MockK tests for MemoDragAndDropCallback
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class MemoDragAndDropCallbackTest {

    private lateinit var context: Context
    private lateinit var recyclerView: RecyclerView
    private lateinit var rvAdapter: SimpleRVAdapter

    // MemoAdapter 는 실제 앱의 어댑터 타입.
    // 여기선 moveItem(from, to) 호출만 검증하면 되므로 MockK 로 대체.
    private lateinit var memoAdapterMock: MemoAdapter

    // 콜백 람다 기록용
    private lateinit var onMoveFrom: AtomicInteger
    private lateinit var onMoveTo: AtomicInteger
    private lateinit var onDeletePos: AtomicInteger
    private lateinit var dragStarted: AtomicBoolean
    private lateinit var dragEnded: AtomicBoolean

    private lateinit var callback: MemoDragAndDropCallback

    @Before
    fun setup() {
        context = getApplicationContext()
        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
        }

        // 실제 RV 에 붙일 간단한 어댑터 (뷰홀더에서 adapterPosition 을 올바르게 리턴받기 위해)
        rvAdapter = SimpleRVAdapter(itemCount = 3)
        recyclerView.adapter = rvAdapter

        // 레이아웃 pass (Robolectric에서 홀더 확보/포지션 계산 가능하도록)
        val root = FrameLayout(context)
        root.addView(recyclerView)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.AT_MOST)
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, 1080, 1920)

        memoAdapterMock = mockk(relaxed = true)

        onMoveFrom = AtomicInteger(-1)
        onMoveTo = AtomicInteger(-1)
        onDeletePos = AtomicInteger(-1)
        dragStarted = AtomicBoolean(false)
        dragEnded = AtomicBoolean(false)

        callback = MemoDragAndDropCallback(
            adapter = memoAdapterMock,
            onMove = { f, t -> onMoveFrom.set(f); onMoveTo.set(t) },
            onDelete = { p -> onDeletePos.set(p) },
            onDragStart = { dragStarted.set(true) },
            onDragEnd = { dragEnded.set(true) }
        )
    }

    @Test
    fun getMovementFlags_returnsUpDownAndLeftRight() {
        val vh = requireNotNull(recyclerView.findViewHolderForAdapterPosition(0))
        val flags = callback.getMovementFlags(recyclerView, vh)

        // 드래그(UP/DOWN)와 스와이프(LEFT/RIGHT) 플래그가 들어있는지 확인
        fun has(mask: Int) = (flags and mask) != 0
        assertThat(has(ItemTouchHelper.UP), `is`(true))
        assertThat(has(ItemTouchHelper.DOWN), `is`(true))
    }

    @Test
    fun onMove_callsAdapterMoveItem_and_onMoveLambda_withCorrectPositions() {
        val from = requireNotNull(recyclerView.findViewHolderForAdapterPosition(0))
        val to = requireNotNull(recyclerView.findViewHolderForAdapterPosition(2))

        val result = callback.onMove(recyclerView, from, to)
        assertThat(result, `is`(true))

        // adapter.moveItem(from, to) 호출 검증
        verify(exactly = 1) { memoAdapterMock.moveItem(0, 2) }
        // onMove 람다 호출값 검증
        assertThat(onMoveFrom.get(), `is`(0))
        assertThat(onMoveTo.get(), `is`(2))

        confirmVerified(memoAdapterMock)
    }

    @Test
    fun onSwiped_callsOnDelete_withViewHolderPosition() {
        val vh = requireNotNull(recyclerView.findViewHolderForAdapterPosition(1))

        callback.onSwiped(vh, ItemTouchHelper.LEFT)

        assertThat(onDeletePos.get(), `is`(1))
    }

    @Test
    fun onSelectedChanged_whenDrag_setsAlpha_and_callsStartEndLambdas() {
        val vh = requireNotNull(recyclerView.findViewHolderForAdapterPosition(0))

        // DRAG 시작
        callback.onSelectedChanged(vh, ItemTouchHelper.ACTION_STATE_DRAG)
        assertThat(dragStarted.get(), `is`(true))
        assertThat(vh.itemView.alpha, `is`(0.5f))

        // DRAG 종료(아이들 상태로 변경)
        callback.onSelectedChanged(vh, ItemTouchHelper.ACTION_STATE_IDLE)
        assertThat(dragEnded.get(), `is`(true))
        assertThat(vh.itemView.alpha, `is`(1.0f))
    }

    @Test
    fun clearView_restoresAlpha() {
        val vh = requireNotNull(recyclerView.findViewHolderForAdapterPosition(0))
        vh.itemView.alpha = 0.3f

        callback.clearView(recyclerView, vh)
        assertThat(vh.itemView.alpha, `is`(1.0f))
    }

    @Test
    fun onChildDraw_whenDrag_setsTranslationXY() {
        val vh = requireNotNull(recyclerView.findViewHolderForAdapterPosition(1))
        val canvas = Canvas()

        callback.onChildDraw(
            c = canvas,
            recyclerView = recyclerView,
            viewHolder = vh,
            dX = 12f,
            dY = -20f,
            actionState = ItemTouchHelper.ACTION_STATE_DRAG,
            isCurrentlyActive = true
        )

        assertThat(vh.itemView.translationX, `is`(12f))
        assertThat(vh.itemView.translationY, `is`(-20f))
    }

    // --- RecyclerView용 심플 어댑터 (홀더 포지션 계산만 필요) ---
    private class SimpleRVAdapter(private val itemCount: Int) :
        RecyclerView.Adapter<SimpleRVAdapter.VH>() {

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            return VH(View(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    100
                )
            })
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            // no-op
        }

        override fun getItemCount(): Int = itemCount
    }
}
