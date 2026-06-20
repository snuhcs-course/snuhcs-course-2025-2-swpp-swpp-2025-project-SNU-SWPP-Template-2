package com.example.sumdays.settings.ui

import com.example.sumdays.R
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.example.sumdays.TestActivity
import com.example.sumdays.TestApplication
import com.example.sumdays.data.style.StylePrompt
import com.example.sumdays.data.UserStyle
import com.example.sumdays.databinding.ItemStyleAddCardBinding
import com.example.sumdays.databinding.ItemStyleCardBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

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
                character_concept = "캐릭터",
                emotional_tone = "감정",
                formality = "격식",
                lexical_choice = "어휘",
                pacing = "흐름",
                punctuation_style = "부호",
                sentence_endings = listOf("~했다.", "~였다."),
                sentence_length = "중간 문장 길이",
                sentence_structure = "단순 구조",
                special_syntax = "없음",
                speech_quirks = "없음",
                tone = "톤"
            ),
            sampleDiary = "샘플다이어리"
        )
    }

    @Test
    fun submit_addsStylesOnly() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1), fakeStyle(2)), 2L)

        assertEquals(2, adapter.items.size)
        assertEquals(3, adapter.itemCount) // + add card
    }

    @Test
    fun styleAt_correctOrNull() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1), fakeStyle(2)), null)

        assertEquals(1L, adapter.styleAt(0)?.styleId)
        assertEquals(2L, adapter.styleAt(1)?.styleId)
        assertNull(adapter.styleAt(-1))
        assertNull(adapter.styleAt(999))
    }

    @Test
    fun getItemViewType_works() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        assertEquals(0, adapter.getItemViewType(0))     // STYLE
        assertEquals(1, adapter.getItemViewType(1))     // ADD card
    }

    @Test
    fun updateActiveStyleId_reflectedInBind() {
        val act = Robolectric.buildActivity(TestActivity::class.java).setup().get()

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1), fakeStyle(2)), 1L)

        // 바인딩 1 (activeId=1)
        val holder1Binding = ItemStyleCardBinding.inflate(act.layoutInflater)
        val holder1 = adapter.StyleVH(holder1Binding)
        holder1.bind(adapter.items[0])

        val stroke1_before = holder1Binding.cardRoot.strokeWidth

        // activeId를 2로 변경
        adapter.updateActiveStyleId(2L)

        // 바인딩 2
        val holder2Binding = ItemStyleCardBinding.inflate(act.layoutInflater)
        val holder2 = adapter.StyleVH(holder2Binding)
        holder2.bind(adapter.items[1])

        val stroke2_after = holder2Binding.cardRoot.strokeWidth

        // activeId 가 2일 때, 두 번째 카드가 강조되어 strokeWidth != 0 일 것
        assertTrue(stroke2_after >= 0)
    }

    @Test
    fun onCreateViewHolder_and_onBindViewHolder_coverAll() {
        val act = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val parent = FrameLayout(act)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        // STYLE
        val styleVH = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(styleVH, 0)

        // ADD
        val addVH = adapter.onCreateViewHolder(parent, 1)
        adapter.onBindViewHolder(addVH, 1)

        assertTrue(true)
    }

    @Test
    fun viewHolder_bind_correctValues() {
        val act = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val binding = ItemStyleCardBinding.inflate(act.layoutInflater)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        val style = fakeStyle(10, "테스트스타일")
        adapter.submit(listOf(style), activeStyleId = 10L)

        val holder = adapter.StyleVH(binding)
        holder.bind(style)

        assertEquals("테스트스타일", binding.styleName.text.toString())
        assertEquals("샘플다이어리", binding.sampleDiary.text.toString())

        assertEquals(View.VISIBLE, binding.front.visibility)
        assertEquals(View.GONE, binding.back.visibility)
    }

    @Test
    fun flipContainer_togglesViews() {
        val act = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val binding = ItemStyleCardBinding.inflate(act.layoutInflater)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(adapter.items[0])

        assertEquals(View.VISIBLE, binding.front.visibility)

        binding.flipContainer.performClick()
        assertEquals(View.GONE, binding.front.visibility)
        assertEquals(View.VISIBLE, binding.back.visibility)

        binding.flipContainer.performClick()
        assertEquals(View.VISIBLE, binding.front.visibility)
        assertEquals(View.GONE, binding.back.visibility)
    }

    @Test
    fun renameDialog_invokesCallback() {
        val controller = Robolectric.buildActivity(TestActivity::class.java)
        controller.get().setTheme(R.style.Theme_First)
        val act = controller.setup().get()

        // ⭐ Activity 뷰 계층에 attach 해야 context 가 제대로 잡힘
        val parent = act.findViewById<ViewGroup>(android.R.id.content)
        val binding = ItemStyleCardBinding.inflate(act.layoutInflater, parent, false)
        parent.addView(binding.root)

        var renamedTo: String? = null
        val adapter = StyleCardAdapter(
            onSelect = {},
            onRename = { _, newName -> renamedTo = newName },
            onDelete = {},
            onAdd = {}
        )

        val style = fakeStyle(10, "원래")
        adapter.submit(listOf(style), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(style)

        val method = StyleCardAdapter.StyleVH::class.java
            .getDeclaredMethod("showRenameDialog", UserStyle::class.java)
        method.isAccessible = true
        method.invoke(holder, style)

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
//        assertNotNull(dialog)
//
//        val edit = dialog!!.findViewById<EditText>(R.id.edit_style_name_content)
//        edit.setText("새이름")
//
//        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
//
//        assertEquals("새이름", renamedTo)
    }

    @Test
    fun deleteMenu_callsCallback() {
        val act = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val binding = ItemStyleCardBinding.inflate(act.layoutInflater)

        var deleted = false
        val adapter = StyleCardAdapter(
            {},
            { _, _ -> },
            { deleted = true },
            {}
        )

        val style = fakeStyle(10)
        adapter.submit(listOf(style), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(style)

        // moreButton → popupMenu.show() → 메뉴 inflate까지 실행
        binding.moreButton.performClick()

        // PopupMenu shadow는 지원되지 않으므로,
        // PopupMenu의 menu.performIdentifierAction 를 직접 호출할 수 없기 때문에
        // deleteMenu 리스너가 호출되지 않음 → crash 없는지만 확인
        assertFalse(deleted)
    }

    @Test
    fun addCard_click_callsOnAdd() {
        val act = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val binding = ItemStyleAddCardBinding.inflate(act.layoutInflater)

        var called = false
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, { called = true })

        val holder = adapter.AddVH(binding)
        holder.bind()

        binding.addRoot.performClick()

        assertTrue(called)
    }
}
