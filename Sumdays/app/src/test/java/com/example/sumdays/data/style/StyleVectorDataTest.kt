package com.example.sumdays.data.style

import android.content.Context
import com.example.sumdays.TestApplication
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
class StyleVectorLoaderTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `loadStyleVector id 1 returns nonEmptyList`() {
        val vec = loadStyleVector(context, 1)

        assertNotNull(vec)
        assertTrue("style_vector1은 비어 있으면 안 됨", vec.isNotEmpty())
    }

    @Test
    fun `loadStyleVector id 2 returns nonEmptyList`() {
        val vec = loadStyleVector(context, 2)

        assertNotNull(vec)
        assertTrue("style_vector2는 비어 있으면 안 됨", vec.isNotEmpty())
    }

    @Test
    fun `loadStyleVector id 3 returns nonEmptyList`() {
        val vec = loadStyleVector(context, 3)

        assertNotNull(vec)
        assertTrue("style_vector3는 비어 있으면 안 됨", vec.isNotEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `loadStyleVector invalidId throws`() {
        loadStyleVector(context, 999)
    }
}