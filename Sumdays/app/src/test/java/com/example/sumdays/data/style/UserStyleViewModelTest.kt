package com.example.sumdays.data.style

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class FakeUserStyleDao : UserStyleDao {

    private val list = mutableListOf<UserStyle>()
    private val live = MutableLiveData<List<UserStyle>>()

    private fun notifyChange() {
        // 테스트에서는 동기 업데이트가 편하니 value 사용
        live.value = list.toList()
    }

    override suspend fun insertStyle(style: UserStyle): Long {
        val id = if (style.styleId == 0L) (list.size + 1).toLong() else style.styleId
        val newStyle = style.copy(styleId = id)
        list.add(newStyle)
        notifyChange()
        return id
    }

    override fun getAllStyles(): LiveData<List<UserStyle>> {
        notifyChange()
        return live
    }

    override suspend fun deleteStyle(style: UserStyle) {
        list.removeIf { it.styleId == style.styleId }
        notifyChange()
    }

    override suspend fun getStyleById(styleId: Long): UserStyle? {
        return list.find { it.styleId == styleId }
    }

    override suspend fun deleteAllStyles() {
        list.clear()
        notifyChange()
    }

    override suspend fun updateStyle(style: UserStyle) {
        val idx = list.indexOfFirst { it.styleId == style.styleId }
        if (idx != -1) {
            list[idx] = style
            notifyChange()
        }
    }

    override suspend fun getAllStyleNames(): List<String> {
        return list.map { it.styleName }
    }

    override suspend fun updateSampleDiary(id: Long, diary: String) {
        val idx = list.indexOfFirst { it.styleId == id }
        if (idx != -1) {
            val cur = list[idx]
            list[idx] = cur.copy(sampleDiary = diary)
            notifyChange()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserStyleViewModelTest {

    @get:Rule
    val instantRule = InstantTaskExecutorRule()

    private lateinit var fakeDao: FakeUserStyleDao
    private lateinit var viewModel: UserStyleViewModel

    @Before
    fun setup() {
        fakeDao = FakeUserStyleDao()

        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = UserStyleViewModel(app)

        // private dao 필드를 FakeDao로 교체
        val field = UserStyleViewModel::class.java.getDeclaredField("dao")
        field.isAccessible = true
        field.set(viewModel, fakeDao)
    }

    // LiveData 동기 읽기 헬퍼
    private fun <T> LiveData<T>.getOrAwait(
        time: Long = 2,
        unit: TimeUnit = TimeUnit.SECONDS
    ): T {
        var data: T? = null
        val latch = CountDownLatch(1)
        val observer = object : Observer<T> {
            override fun onChanged(t: T) {
                data = t
                latch.countDown()
                this@getOrAwait.removeObserver(this)
            }
        }
        observeForever(observer)

        if (!latch.await(time, unit)) {
            removeObserver(observer)
            throw TimeoutException("LiveData value was never set.")
        }
        @Suppress("UNCHECKED_CAST")
        return data as T
    }

    private fun dummyPrompt() = StylePrompt(
        tone = "t",
        formality = "f",
        sentence_length = "len",
        sentence_structure = "struct",
        sentence_endings = listOf("다"),
        lexical_choice = "lex",
        common_phrases = listOf("흠"),
        emotional_tone = "emo",
        irony_or_sarcasm = "none",
        slang_or_dialect = "slang",
        pacing = "fast"
    )

    private fun dummyStyle(name: String) = UserStyle(
        styleName = name,
        styleVector = listOf(1f),
        styleExamples = listOf("ex"),
        stylePrompt = dummyPrompt(),
        sampleDiary = "sample"
    )

    @Test
    fun insertStyleAndGetAllStyles() = runBlocking {
        val style = dummyStyle("A")

        // 비동기 insert 실행
        viewModel.insertStyle(style)

        // IO 코루틴이 돌 시간을 조금 준다
        delay(50)

        val list = viewModel.getAllStyles().getOrAwait()
        assertEquals(1, list.size)
        assertEquals("A", list[0].styleName)
    }

    @Test
    fun updateStyleWorks() = runBlocking {
        // 먼저 Dao에 직접 하나 넣고
        val id = fakeDao.insertStyle(dummyStyle("D"))

        // ViewModel 통해 업데이트
        val updated = dummyStyle("D2").copy(styleId = id)
        viewModel.updateStyle(updated)

        delay(50)

        val list = viewModel.getAllStyles().getOrAwait()
        assertEquals(1, list.size)
        assertEquals("D2", list[0].styleName)
    }

    @Test
    fun deleteAllStylesWorks() = runBlocking {
        fakeDao.insertStyle(dummyStyle("A"))
        fakeDao.insertStyle(dummyStyle("B"))

        viewModel.deleteAllStyles()
        delay(50)

        val list = viewModel.getAllStyles().getOrAwait()
        assertTrue(list.isEmpty())
    }

    @Test
    fun updateSampleDiaryWorks() = runBlocking {
        val id = fakeDao.insertStyle(dummyStyle("X"))

        viewModel.updateSampleDiary(id, "NEW")
        delay(50)

        val loaded = viewModel.getStyleById(id)
        assertNotNull(loaded)
        assertEquals("NEW", loaded!!.sampleDiary)
    }

    @Test
    fun generateNextStyleNameWorks() = runBlocking {
        fakeDao.insertStyle(dummyStyle("나의 스타일 - 1번째"))
        fakeDao.insertStyle(dummyStyle("나의 스타일 - 2번째"))

        val next = viewModel.generateNextStyleName()
        assertEquals("나의 스타일 - 3번째", next)
    }

    @Test
    fun insertStyleReturnIdWorks() = runBlocking {
        val id = viewModel.insertStyleReturnId(dummyStyle("Z"))
        assertEquals(1L, id)
    }

    @Test
    fun deleteStyleWorks() = runBlocking {
        val id = viewModel.insertStyleReturnId(dummyStyle("DEL_TARGET"))

        delay(50)

        val styleToDelete = dummyStyle("DEL_TARGET").copy(styleId = id)
        viewModel.deleteStyle(styleToDelete)

        delay(50)

        val list = viewModel.getAllStyles().getOrAwait()
        assertTrue(list.isEmpty())
    }

}