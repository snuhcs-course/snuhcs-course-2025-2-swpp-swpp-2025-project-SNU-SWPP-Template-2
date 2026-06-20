package com.example.sumdays.data.style

import android.content.Context
import com.example.sumdays.TestApplication
import com.example.sumdays.data.UserStyle
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = TestApplication::class
)
class UserStyleTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    /** Utility: 공통 검증 함수 */
    private fun assertValidDefaultStyle(style: UserStyle, expectedId: Long, expectedName: String) {
        assertEquals(expectedId, style.styleId)
        assertEquals(expectedName, style.styleName)

        // styleVector: 비어 있지 않아야 함
        assertNotNull(style.styleVector)
        assertTrue(style.styleVector.isNotEmpty())

        // stylePrompt: null 이면 안 됨
        assertNotNull(style.stylePrompt)
        assertTrue(style.stylePrompt.character_concept.isNotBlank())

        // styleExamples: 1개 이상
        assertTrue(style.styleExamples.isNotEmpty())

        // sampleDiary: 기본값 존재
        assertTrue(style.sampleDiary.isNotEmpty())
    }

    // -------------------
    // createDefault1
    // -------------------

    @Test
    fun `createDefault1 creates valid UserStyle`() {
        val style = UserStyle.createDefault1(context)
        assertValidDefaultStyle(style, 1L, "무색무취")
    }

    // -------------------
    // createDefault2
    // -------------------

    @Test
    fun `createDefault2 creates valid UserStyle`() {
        val style = UserStyle.createDefault2(context)
        assertValidDefaultStyle(style, 2L, "냥냥체")
    }

    // -------------------
    // createDefault3
    // -------------------

    @Test
    fun `createDefault3 creates valid UserStyle`() {
        val style = UserStyle.createDefault3(context)
        assertValidDefaultStyle(style, 3L, "오덕체")
    }

    // -------------------
    // loadStyleVector 예외 테스트
    // -------------------

    @Test(expected = IllegalArgumentException::class)
    fun `loadStyleVector throws on invalid id`() {
        loadStyleVector(context, 999)  // 존재하지 않는 id → 예외 발생해야 함
    }
}
