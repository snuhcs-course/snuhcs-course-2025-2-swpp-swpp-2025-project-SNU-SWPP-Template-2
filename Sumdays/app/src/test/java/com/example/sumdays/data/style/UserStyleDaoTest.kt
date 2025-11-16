package com.example.sumdays.data.style

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class UserStyleDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: StyleDatabase
    private lateinit var dao: UserStyleDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            StyleDatabase::class.java
        )
            .allowMainThreadQueries()   // 테스트니까 메인 스레드 허용
            .build()

        dao = db.userStyleDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * LiveData를 동기적으로 꺼내오기 위한 헬퍼
     */
    private fun <T> LiveData<T>.getOrAwaitValue(
        time: Long = 2,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): T {
        var data: T? = null
        val latch = CountDownLatch(1)

        val observer = object : Observer<T> {
            override fun onChanged(t: T) {
                data = t
                latch.countDown()
                this@getOrAwaitValue.removeObserver(this)
            }
        }

        this.observeForever(observer)

        // 타임아웃 나면 테스트 실패
        if (!latch.await(time, timeUnit)) {
            this.removeObserver(observer)
            throw TimeoutException("LiveData value was never set.")
        }

        @Suppress("UNCHECKED_CAST")
        return data as T
    }

    /**
     * 테스트용 더미 StylePrompt 생성
     */
    private fun dummyPrompt(): StylePrompt =
        StylePrompt(
            tone = "톤",
            formality = "격식",
            sentence_length = "길이",
            sentence_structure = "구조",
            sentence_endings = listOf("다"),
            lexical_choice = "어휘",
            common_phrases = listOf("그냥", "뭐지"),
            emotional_tone = "감정적",
            irony_or_sarcasm = "아이러니 없음",
            slang_or_dialect = "반말",
            pacing = "빠름"
        )

    /**
     * 테스트용 더미 UserStyle
     */
    private fun dummyStyle(name: String = "테스트 스타일"): UserStyle =
        UserStyle(
            styleName = name,
            styleVector = listOf(0.1f, 0.2f, 0.3f),
            styleExamples = listOf("예시 문장 1", "예시 문장 2"),
            stylePrompt = dummyPrompt(),
            sampleDiary = "샘플 일기"
        )

    @Test
    fun insert_andQuery_andUpdateSampleDiary_andGetNames() = runBlocking {
        // insertStyle
        val id = dao.insertStyle(dummyStyle("나의 스타일 - 1번째"))
        Assert.assertTrue(id > 0)

        // getStyleById
        val loaded = dao.getStyleById(id)
        Assert.assertNotNull(loaded)
        Assert.assertEquals("나의 스타일 - 1번째", loaded!!.styleName)

        // getAllStyles (LiveData)
        val list = dao.getAllStyles().getOrAwaitValue()
        Assert.assertEquals(1, list.size)
        Assert.assertEquals(id, list[0].styleId)

        // getAllStyleNames
        val names = dao.getAllStyleNames()
        Assert.assertTrue(names.contains("나의 스타일 - 1번째"))

        // updateSampleDiary
        dao.updateSampleDiary(id, "업데이트된 샘플 일기")
        val afterDiaryUpdate = dao.getStyleById(id)
        Assert.assertEquals("업데이트된 샘플 일기", afterDiaryUpdate?.sampleDiary)
    }

    @Test
    fun updateStyle_andDeleteStyle_andDeleteAllStyles() = runBlocking {
        // 스타일 2개 삽입
        val id1 = dao.insertStyle(dummyStyle("스타일 A"))
        val id2 = dao.insertStyle(dummyStyle("스타일 B"))

        var all = dao.getAllStyles().getOrAwaitValue()
        Assert.assertEquals(2, all.size)

        // updateStyle : A 이름 변경
        val styleA = dao.getStyleById(id1)!!
        val updatedA = styleA.copy(styleName = "수정된 스타일 A")
        dao.updateStyle(updatedA)

        val reloadedA = dao.getStyleById(id1)
        Assert.assertEquals("수정된 스타일 A", reloadedA?.styleName)

        // deleteStyle : B 삭제
        val styleB = dao.getStyleById(id2)!!
        dao.deleteStyle(styleB)

        all = dao.getAllStyles().getOrAwaitValue()
        Assert.assertEquals(1, all.size)
        Assert.assertEquals(id1, all[0].styleId)

        // deleteAllStyles : 모두 삭제
        dao.deleteAllStyles()
        all = dao.getAllStyles().getOrAwaitValue()
        Assert.assertTrue(all.isEmpty())
    }
}
