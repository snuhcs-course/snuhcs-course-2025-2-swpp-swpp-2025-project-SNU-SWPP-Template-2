package com.example.sumdays.daily.memo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.R // 실제 R.id 접근을 위해 필요
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import android.os.Looper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule
import androidx.recyclerview.widget.RecyclerView

// 실제 Android 런타임 환경에서 실행되도록 @RunWith(AndroidJUnit4::class) 사용
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MemoAdapterTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    // 테스트에 필요한 Context
    private lateinit var context: Context
    private lateinit var adapter: MemoAdapter
    private lateinit var initialList: List<Memo>

    // 목업 뷰와 뷰 홀더를 생성하기 위해 필요한 레이아웃 리소스 ID (실제 프로젝트에 맞게 수정 필요)
    // R.id.memo_time 및 R.id.memo_text ID가 포함된 item_memo 레이아웃이 필요합니다.
    private val ITEM_MEMO_LAYOUT_ID = R.layout.item_memo // 가정

    @Before
    fun setup() {
        // Application Context 가져오기
        context = ApplicationProvider.getApplicationContext()
        adapter = MemoAdapter()

        // 테스트를 위한 초기 데이터 설정
        initialList = listOf(
            Memo(id = 1, content = "첫 번째 메모 내용", timestamp = "오전 10:00", date = "2023-01-01", order = 1),
            Memo(id = 2, content = "두 번째 메모 내용", timestamp = "오전 11:00", date = "2023-01-01", order = 2),
            Memo(id = 3, content = "세 번째 메모 내용", timestamp = "오후 12:00", date = "2023-01-01", order = 3)
        )
        // ListAdapter에 데이터 제출
        adapter.submitList(initialList)
        // submitList가 비동기적으로 처리될 수 있으므로, 테스트에서는 즉시 currentList를 확인합니다.
        // ListAdapter의 테스트 시나리오에서는 submitList(null) 후 submitList(list)를 호출하여 DiffUtil 작동을 유도하기도 합니다.
        shadowOf(Looper.getMainLooper()).idle()
    }

    // 테스트를 위한 목업 ViewGroup 및 View 생성
    private fun createParentViewGroup(): ViewGroup {
        // 간단한 FrameLayout 목업을 사용하거나 실제 ViewGroup을 생성합니다.
        // 여기서는 Context만으로 ViewGroup을 생성합니다.
        return android.widget.FrameLayout(context)
    }

    // onCreateViewHolder 테스트
    @Test
    fun onCreateViewHolder_createsCorrectViewHolder() {
        val parent = createParentViewGroup()
        val viewHolder = adapter.onCreateViewHolder(parent, 0)

        // ViewHolder가 null이 아닌지 확인
        assertNotNull(viewHolder)
        // MemoViewHolder 타입인지 확인
        assertTrue(viewHolder is MemoAdapter.MemoViewHolder)

        // *참고: 실제 TextViews가 제대로 바인딩되었는지 확인하려면
        // LayoutInflater.from(context).inflate(ITEM_MEMO_LAYOUT_ID, parent, false)와 같은
        // 실제 View 인플레이션이 필요하며, 이는 복잡해질 수 있습니다.
        // 이 테스트는 ViewHolder 인스턴스 생성까지만 확인합니다.
    }

    // onBindViewHolder 테스트 (데이터 바인딩 및 클릭 리스너)
    @Test
    fun onBindViewHolder_bindsDataAndSetsClickListener() {
        // 1. Mock View 및 ViewHolder 생성 (실제 View 생성)
        val parent = createParentViewGroup()
        // item_memo 레이아웃을 인플레이트
        val view = LayoutInflater.from(context).inflate(ITEM_MEMO_LAYOUT_ID, parent, false)

        // ViewHolder 생성 (이때, R.id에 해당하는 TextView를 찾을 수 있어야 합니다)
        val viewHolder = MemoAdapter.MemoViewHolder(view)

        // 2. 클릭 리스너 목(Mock) 생성 및 설정
        val mockListener = mock(MemoAdapter.OnItemClickListener::class.java)
        adapter.setOnItemClickListener(mockListener)

        // 3. onBindViewHolder 호출 (첫 번째 아이템)
        val positionToBind = 0
        val expectedMemo = initialList[positionToBind]
        adapter.onBindViewHolder(viewHolder, positionToBind)

        // 4. 데이터 바인딩 확인
        assertEquals(expectedMemo.timestamp, viewHolder.timestamp.text.toString())
        assertEquals(expectedMemo.content, viewHolder.content.text.toString())

        // 5. 아이템 클릭 리스너 호출 확인
        viewHolder.itemView.performClick()
        // 클릭이 발생했을 때, mockListener의 onItemClick이 호출되었는지 확인
        verify(mockListener).onItemClick(expectedMemo)
    }
    @Test
    fun moveItem_doesNotLoseOrDuplicateItems() {
        // 초기 리스트 제출
        adapter.submitList(initialList)
        shadowOf(Looper.getMainLooper()).idle()

        adapter.moveItem(fromPosition = 0, toPosition = 2)
        shadowOf(Looper.getMainLooper()).idle()

        val result = adapter.currentList

        assertEquals(3, result.size)

        val sortedIds = result.map { it.id }.sorted()
        assertEquals(listOf(1, 2, 3), sortedIds)
    }



}