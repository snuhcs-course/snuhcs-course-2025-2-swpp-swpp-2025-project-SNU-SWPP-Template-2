package com.example.sumdays.daily.memo

import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.example.sumdays.network.ApiClient
import com.example.sumdays.network.ApiService
import com.google.gson.JsonObject
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class MemoMergeAdapterTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(ApiClient)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    /** 테스트용 더미 메모 리스트 */
    private fun makeMemoList(): MutableList<Memo> = mutableListOf(
        Memo(id = 1, content = "hello", date = "11-01", order = 0, timestamp = "10:00"),
        Memo(id = 2, content = "world", date = "11-01", order = 1, timestamp = "10:01"),
        Memo(id = 3, content = "fox", date = "11-01", order = 2, timestamp = "10:02")
    )

    // mergeTextByIds 테스트
    @Test
    fun mergeTextByIds_returnsMergedText() = runTest {
        // given
        val fakeJson = JsonObject().apply {
            add("merged_content", JsonObject().apply {
                addProperty("merged_content", "merged-result")
            })
        }

        val fakeApi = mockk<ApiService>()
        coEvery { fakeApi.mergeMemos(any()) } returns Response.success(fakeJson)
        every { ApiClient.api } returns fakeApi

        val adapter = MemoMergeAdapter(makeMemoList(), this, useStableIds = false)

        // when
        val result = adapter.mergeTextByIds(listOf(1, 2))

        // then
        assertEquals("merged-result", result)
        coVerify(exactly = 1) { fakeApi.mergeMemos(any()) }
    }

    // skipMerge 테스트
    @Test
    fun skipMerge_callsMergeTextByIdsOnce() = runTest {
        val adapter = spyk(MemoMergeAdapter(makeMemoList(), this, useStableIds = false))
        coEvery { adapter.mergeTextByIds(any()) } returns "merged-all"

        val result = adapter.mergeAllMemo()

        assertEquals("merged-all", result)
        coVerify(exactly = 1) { adapter.mergeTextByIds(any()) }
    }

    // updateIdMap 테스트
    @Test
    fun updateIdMap_correctlyUpdatesMapping() {
        val adapter = MemoMergeAdapter(makeMemoList(), TestScope(testDispatcher), useStableIds = false)
        val mergedIds = listOf(1, 2, 3)

        adapter.updateIdMap(1, mergedIds)

        val field = MemoMergeAdapter::class.java.getDeclaredField("idToMergedIds")
        field.isAccessible = true
        val map = field.get(adapter) as MutableMap<*, *>

        assertEquals(mergedIds, map[1])
    }

    // undoLastMerge 동작 테스트
    @Test
    fun undoLastMerge_restoresPreviousState() {
        val list = makeMemoList()
        val adapter = MemoMergeAdapter(list, TestScope(testDispatcher), useStableIds = false)

        // private inner class MergeRecord 접근
        val recordClass = MemoMergeAdapter::class.java.declaredClasses
            .first { it.simpleName == "MergeRecord" }
        val constructor = recordClass.getDeclaredConstructor(
            Int::class.java, Int::class.java, Memo::class.java, Memo::class.java, MutableList::class.java
        )
        constructor.isAccessible = true
        val record = constructor.newInstance(0, 1, list[0], list[1], mutableListOf<Int>())

        val undoField = MemoMergeAdapter::class.java.getDeclaredField("undoStack")
        undoField.isAccessible = true
        val stack = undoField.get(adapter) as ArrayDeque<Any>
        stack.addLast(record)

        val result = adapter.undoLastMerge()
        assertTrue(result)
    }

    // extractMergedText 케이스별 테스트
    @Test
    fun extractMergedText_returnsMergedOrDiary() {
        val adapter = MemoMergeAdapter(makeMemoList(), TestScope(testDispatcher), useStableIds = false)
        val extractFn = MemoMergeAdapter::class.java.getDeclaredMethod("extractMergedText", JsonObject::class.java)
        extractFn.isAccessible = true

        // case 1: diary
        val json1 = JsonObject().apply { addProperty("diary", "final-diary") }
        val result1 = extractFn.invoke(adapter, json1)
        assertEquals("final-diary", result1)

        // case 2: merged_content
        val json2 = JsonObject().apply {
            add("merged_content", JsonObject().apply {
                addProperty("merged_content", "merged-content")
            })
        }
        val result2 = extractFn.invoke(adapter, json2)
        assertEquals("merged-content", result2)

        // case 3: unknown
        val result3 = extractFn.invoke(adapter, JsonObject())
        assertEquals("", result3)
    }

    // mergeByIndex가 coroutine launch 후 UI 업데이트 로직 호출하는지 테스트
    @Test
    fun mergeByIndex_launchesCoroutineWithoutCrash() = runTest {
        val adapter = spyk(MemoMergeAdapter(makeMemoList(), this, useStableIds = false))
        coEvery { adapter.mergeTextByIds(any()) } returns "merged"

        // private 함수 직접 접근
        val method = MemoMergeAdapter::class.java.getDeclaredMethod(
            "mergeByIndex", Int::class.java, Int::class.java, List::class.java
        )
        method.isAccessible = true

        method.invoke(adapter, 0, 1, listOf(1, 2))
        assertTrue(true) // 예외 없이 끝나면 성공
    }

    @Test
    fun getItemCountAndId_areCorrect() {
        val list = makeMemoList()
        val adapter = MemoMergeAdapter(list, TestScope(), useStableIds = false)

        assertEquals(3, adapter.itemCount)
        val id = adapter.getItemId(0)
        assertTrue(id is Long)
    }

    @Test
    fun constructor_initializesIdMapProperly() {
        val adapter = MemoMergeAdapter(makeMemoList(), TestScope(), useStableIds = false)
        val field = MemoMergeAdapter::class.java.getDeclaredField("idToMergedIds")
        field.isAccessible = true
        val map = field.get(adapter) as MutableMap<*, *>

        assertEquals(listOf(1), map[1])
        assertEquals(listOf(2), map[2])
    }

    @Test
    fun onCreateViewHolder_and_onBindViewHolder_workCorrectly() {
        val adapter = MemoMergeAdapter(makeMemoList(), TestScope(), useStableIds = false)
        val parent = FrameLayout(ApplicationProvider.getApplicationContext())

        val vh = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(vh, 0)

        assertTrue(vh.itemView is View)
    }

    @Test
    fun onBindViewHolder_setsDragListenerProperly() {
        val adapter = MemoMergeAdapter(makeMemoList(), TestScope(), useStableIds = false)
        val parent = FrameLayout(ApplicationProvider.getApplicationContext())
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val view = holder.itemView
        assertTrue(view.hasOnClickListeners() || view.isLongClickable)
    }

    @Test(expected = IllegalStateException::class)
    fun mergeTextByIds_throwsWhenResponseEmpty() = runTest {
        val fakeApi = mockk<ApiService>()
        coEvery { fakeApi.mergeMemos(any()) } returns Response.success(null)
        every { ApiClient.api } returns fakeApi

        val adapter = MemoMergeAdapter(makeMemoList(), this, useStableIds = false)
        adapter.mergeTextByIds(listOf(1, 2)) // body == null → IllegalStateException
    }

}
