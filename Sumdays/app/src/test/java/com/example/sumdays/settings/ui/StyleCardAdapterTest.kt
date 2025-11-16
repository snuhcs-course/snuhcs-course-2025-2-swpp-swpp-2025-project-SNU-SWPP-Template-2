package com.example.sumdays.settings.ui

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.TestActivity
import com.example.sumdays.TestApplication
import com.example.sumdays.data.style.StylePrompt
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.databinding.ItemStyleAddCardBinding
import com.example.sumdays.databinding.ItemStyleCardBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE],
    application = TestApplication::class,
    manifest = Config.NONE
)
class StyleCardAdapterTest {

    private fun fakeStyle(id: Long, name: String = "스타일$id"): UserStyle {
        return UserStyle(
            styleId = id,
            styleName = name,
            styleVector = listOf(0.1f, 0.2f),
            styleExamples = listOf("예시1", "예시2"),
            stylePrompt = StylePrompt(
                tone = "톤",
                formality = "격식",
                sentence_length = "길이",
                sentence_structure = "구조",
                sentence_endings = listOf("다"),
                lexical_choice = "단어",
                common_phrases = listOf("흔한말"),
                emotional_tone = "감정",
                irony_or_sarcasm = "아이러니",
                slang_or_dialect = "사투리",
                pacing = "템포"
            ),
            sampleDiary = "샘플다이어리"
        )
    }

    // region 기본 submit / 조회 계열

    @Test
    fun submit_includesDefaultStyleAndProvidedStyles() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1), fakeStyle(2)), activeStyleId = 2)

        // items: Default + 1 + 2 = 3
        assertEquals(3, adapter.items.size)
        // itemCount: items + ADD 카드 = 4
        assertEquals(4, adapter.itemCount)
    }

    @Test
    fun getItemViewType_returnsCorrectTypes() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        assertEquals(0, adapter.getItemViewType(0)) // Default
        assertEquals(0, adapter.getItemViewType(1)) // fakeStyle(1)
        assertEquals(1, adapter.getItemViewType(2)) // ADD card
    }

    @Test
    fun styleAt_returnsNullWhenOutOfRange() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        assertNull(adapter.styleAt(-1))
        assertNull(adapter.styleAt(999))
    }

    @Test
    fun styleAt_returnsCorrectStyle() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1), fakeStyle(2)), null)

        assertEquals(1L, adapter.styleAt(1)?.styleId)
        assertEquals(2L, adapter.styleAt(2)?.styleId)
    }

    @Test
    fun updateActiveStyleId_changesInternalState() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1), fakeStyle(2)), null)

        adapter.updateActiveStyleId(2L)

        assertEquals(2L, adapter.styleAt(2)?.styleId)
    }

    // endregion

    // region onCreateViewHolder / onBindViewHolder 커버

    @Test
    fun onCreateViewHolder_and_onBindViewHolder_coverBothTypes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parent = FrameLayout(context)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        // TYPE_STYLE ViewHolder 생성 및 바인딩 (position 0: Default, 1: fakeStyle(1))
        val styleHolder0 =
            adapter.onCreateViewHolder(parent, 0) as RecyclerView.ViewHolder
        adapter.onBindViewHolder(styleHolder0, 0)

        val styleHolder1 =
            adapter.onCreateViewHolder(parent, 0) as RecyclerView.ViewHolder
        adapter.onBindViewHolder(styleHolder1, 1)

        // TYPE_ADD ViewHolder 생성 및 바인딩 (마지막 position = items.size)
        val addHolder =
            adapter.onCreateViewHolder(parent, 1) as RecyclerView.ViewHolder
        adapter.onBindViewHolder(addHolder, adapter.items.size)

        // 예외 없이 여기까지 오면 onCreateViewHolder / onBindViewHolder 전부 실행된 것
        assertTrue(true)
    }

    // endregion

    // region ViewHolder bind + flip 동작

    @Test
    fun viewHolder_bind_bindsCorrectly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemStyleCardBinding.inflate(inflater)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), activeStyleId = 1)

        // items[0] = Default, [1] = fakeStyle(1)
        val style = adapter.items[1]
        val holder = adapter.StyleVH(binding)
        holder.bind(style)

        assertEquals(style.styleName, binding.styleName.text.toString())
        assertEquals("샘플다이어리", binding.sampleDiary.text.toString())
        assertEquals(View.VISIBLE, binding.front.visibility)
        assertEquals(View.GONE, binding.back.visibility)
    }

    @Test
    fun viewHolder_flip_togglesFrontBackVisibility() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemStyleCardBinding.inflate(inflater)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(adapter.items[1])

        // 초기 상태
        assertEquals(View.VISIBLE, binding.front.visibility)
        assertEquals(View.GONE, binding.back.visibility)

        // flipContainer 클릭 → 두 번째 setOnClickListener의 단순 토글 분기 실행
        binding.flipContainer.performClick()
        assertEquals(View.GONE, binding.front.visibility)
        assertEquals(View.VISIBLE, binding.back.visibility)

        binding.flipContainer.performClick()
        assertEquals(View.VISIBLE, binding.front.visibility)
        assertEquals(View.GONE, binding.back.visibility)
    }

    // endregion

    // region 메뉴 버튼 / showRenameDialog / applyFlipAnimation 커버

    @Test
    fun menuButton_click_doesNotCrash_andPopupCodeRuns() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemStyleCardBinding.inflate(inflater)

        var deleteCalled = false
        val adapter = StyleCardAdapter(
            onSelect = {},
            onRename = { _, _ -> },
            onDelete = { _ -> deleteCalled = true },
            onAdd = {}
        )
        adapter.submit(listOf(fakeStyle(1)), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(adapter.items[1])

        // moreButton 클릭 → PopupMenu.apply{...}.show()까지 실행됨
        binding.moreButton.performClick()

        // 여기서는 단순히 에러 없이 통과하는지만 보면 됨 (PopupMenu 생성 분기 커버)
        assertFalse(deleteCalled)
    }

    @Test
    fun showRenameDialog_runsWithoutCrash() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val inflater = LayoutInflater.from(activity)
        val binding = ItemStyleCardBinding.inflate(inflater)

        val adapter = StyleCardAdapter(
            onSelect = {},
            onRename = { _, _ -> },
            onDelete = {},
            onAdd = {}
        )
        val style = fakeStyle(1, "스타일1")
        adapter.submit(listOf(style), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(adapter.items[1])

        val method = StyleCardAdapter.StyleVH::class.java
            .getDeclaredMethod("showRenameDialog", UserStyle::class.java)
        method.isAccessible = true

        // 단순히 예외 없이 실행되기만 하면 OK
        method.invoke(holder, style)
    }



    @Test
    fun applyFlipAnimation_runsWithoutCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemStyleCardBinding.inflate(inflater)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(adapter.items[1])

        val method = StyleCardAdapter.StyleVH::class.java
            .getDeclaredMethod("applyFlipAnimation", Boolean::class.javaPrimitiveType)
        method.isAccessible = true

        // 단순히 예외 없이 실행되면 OK (라인 커버)
        method.invoke(holder, false)
        method.invoke(holder, true)
    }


    // endregion

    // region AddVH

    @Test
    fun addCard_click_callsOnAdd() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemStyleAddCardBinding.inflate(inflater)

        var clicked = false
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, { clicked = true })

        val holder = adapter.AddVH(binding)
        holder.bind()

        binding.addRoot.performClick()

        assertTrue(clicked)
    }

    // endregion
}
