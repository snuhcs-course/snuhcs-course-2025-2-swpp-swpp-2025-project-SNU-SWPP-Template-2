package com.example.sumdays.data.dao

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.TestApplication
import com.example.sumdays.data.AppDatabase
import com.example.sumdays.data.style.StylePrompt
import com.example.sumdays.data.UserStyle
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Config(sdk = [34],
    application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
class UserStyleDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var dao: UserStyleDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
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
    }
}
