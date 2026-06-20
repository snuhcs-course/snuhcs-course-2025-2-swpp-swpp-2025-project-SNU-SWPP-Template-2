package com.example.sumdays.data.style

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import com.example.sumdays.TestApplication
import com.example.sumdays.data.UserStyle
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
import com.example.sumdays.data.dao.UserStyleDao

@Config(sdk = [34],
    application = TestApplication::class)
class FakeUserStyleDao : UserStyleDao {

    private val list = mutableListOf<UserStyle>()
    private val live = MutableLiveData<List<UserStyle>>()

    private fun notifyChange() {
        // 테스트에서는 동기 업데이트가 편하니 value 사용
        live.value = list.toList()
    }

    override suspend fun insertStyleRaw(style: UserStyle): Long {
        TODO("Not yet implemented")
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

    override suspend fun markAsDeleted(styleId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteStyle(style: UserStyle) {
        list.removeIf { it.styleId == style.styleId }
        notifyChange()
    }

    override suspend fun getStyleById(styleId: Long): UserStyle? {
        return list.find { it.styleId == styleId }
    }

    override suspend fun clearAll() {
        TODO("Not yet implemented")
    }

    override suspend fun insertAll(entries: List<UserStyle>) {
        TODO("Not yet implemented")
    }

    override suspend fun getDeletedStyles(): List<UserStyle> {
        TODO("Not yet implemented")
    }

    override suspend fun getEditedStyles(): List<UserStyle> {
        TODO("Not yet implemented")
    }

    override suspend fun resetDeletedFlags(ids: List<Long>) {
        TODO("Not yet implemented")
    }

    override suspend fun resetEditedFlags(ids: List<Long>) {
        TODO("Not yet implemented")
    }

    override suspend fun updateStyleRaw(style: UserStyle) {
        TODO("Not yet implemented")
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

    override suspend fun getAllStylesDirect(): List<UserStyle> {
        TODO("Not yet implemented")
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34],
    application = TestApplication::class)
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
        character_concept = "일상적인 삶을 살아가는 평범한 사람. 소소한 일상을 관찰하고 기록하는 성향을 가진 인물.",
        emotional_tone = "감정이 드러나지 않고 중립적인 톤으로, 일상적인 사건을 기록하는 데 집중한다.",
        formality = "비격식적인 대화체로, 자연스러운 흐름을 유지하며 친근한 느낌을 준다.",
        lexical_choice = "일상적인 단어와 표현을 사용하여 친근함을 느끼게 한다.",
        pacing = "느긋하고 여유로운 흐름, 빠르지 않게 사건을 나열.",
        punctuation_style = "기본적인 문장 부호 사용, 복잡한 구두점은 없다.",
        sentence_endings = listOf("~었다.", "~했다.", "~었다고 생각했다."),
        sentence_length = "중간 길이의 문장들이 많으며, 간결하게 표현되어 있다.",
        sentence_structure = "주어-서술어 구조가 명확하며, 문장이 단순하고 직관적이다.",
        special_syntax = "일상적인 표현을 그대로 사용하며, 특별한 구문은 없음.",
        speech_quirks = "특별한 말투의 버릇은 없으며, 대화체적인 표현이 자연스럽다.",
        tone = "담담하고 차분한 어조로 일상의 소소한 사건들을 서술."
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

    /*
    @Test
    fun deleteAllStylesWorks() = runBlocking {
        fakeDao.insertStyle(dummyStyle("A"))
        fakeDao.insertStyle(dummyStyle("B"))

        viewModel.deleteAllStyles()
        delay(50)

        val list = viewModel.getAllStyles().getOrAwait()
        assertTrue(list.isEmpty())
    }

     */

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