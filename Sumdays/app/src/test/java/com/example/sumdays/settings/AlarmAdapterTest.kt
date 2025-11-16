package com.example.sumdays.settings

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sumdays.R // R.id 사용을 위해 import
import com.example.sumdays.databinding.ItemAlarmTimeBinding
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AlarmAdapterTest {

    private val mockOnTimeClicked = mockk<(position: Int, time: String) -> Unit>(relaxed = true)
    private val mockOnDeleteClicked = mockk<(position: Int) -> Unit>(relaxed = true)

    private lateinit var adapter: AlarmAdapter
    private lateinit var parent: ViewGroup
    private val initialAlarms = listOf("08:00", "12:30", "20:00")

    // 💡 Mock Binding 대신, 테스트에서 생성된 실제 ViewHolder에서 바인딩 객체를 가져옵니다.
    // private lateinit var mockBinding: ItemAlarmTimeBinding

    @Before
    fun setUp() {
        clearAllMocks()

        adapter = AlarmAdapter(mockOnTimeClicked, mockOnDeleteClicked)
        adapter.updateList(initialAlarms)

        // ViewGroup 준비
        parent = Robolectric.setupActivity(android.app.Activity::class.java).findViewById(android.R.id.content)

        // 💡 핵심 수정: 모든 Mocking 설정 제거. Robolectric이 실제 Binding 객체를 생성하도록 허용합니다.
    }

    // ─────────────────────────────────────────────────────────────
    // 2. ViewHolder 생성 및 바인딩 헬퍼
    // ─────────────────────────────────────────────────────────────

    /**
     * ViewHolder를 생성하고, onBindViewHolder를 호출하며, 바인딩 객체와 뷰 홀더를 반환합니다.
     */
    private fun createAndBindViewHolder(position: Int): Pair<AlarmAdapter.AlarmViewHolder, ItemAlarmTimeBinding> {
        // onCreateViewHolder 호출 (실제 Binding 객체가 생성됨)
        val viewHolder = adapter.onCreateViewHolder(parent, 0)

        // Reflection을 사용하여 private binding 필드에 접근 (테스트 대상 코드의 내부 구조에 의존)
        val bindingField = AlarmAdapter.AlarmViewHolder::class.java.getDeclaredField("binding")
        bindingField.isAccessible = true
        val binding = bindingField.get(viewHolder) as ItemAlarmTimeBinding

        // onBindViewHolder 호출
        adapter.onBindViewHolder(viewHolder, position)

        return Pair(viewHolder, binding)
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Adapter 기본 기능 테스트
    // ─────────────────────────────────────────────────────────────
    // ... (getItemCount, updateList는 변경 없음)

    @Test
    fun onCreateViewHolder_returnsCorrectViewHolder() {
        val viewHolder = adapter.onCreateViewHolder(parent, 0)
        assertTrue(viewHolder is AlarmAdapter.AlarmViewHolder)
        // 💡 Mocking이 없으므로 verify(inflate)는 제거합니다.
    }

    @Test
    fun onBindViewHolder_setsCorrectTime() {
        val (_, binding) = createAndBindViewHolder(0)
        // 💡 실제 TextView의 텍스트를 확인합니다.
        assertEquals("08:00", binding.alarmTimeTextView.text.toString())
    }

    // ─────────────────────────────────────────────────────────────
    // 3. bind() 메서드의 마스터 스위치 분기 커버리지 (100%)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun bind_whenMasterIsOn_enablesViewsAndSetsAlpha1() {
        adapter.isMasterOn = true
        val (_, binding) = createAndBindViewHolder(1)

        // 💡 실제 Binding 객체를 사용
        assertEquals(1.0f, binding.root.alpha)
        assertTrue(binding.root.isEnabled)
        assertTrue(binding.alarmTimeTextView.isEnabled)
        assertTrue(binding.deleteAlarmButton.isEnabled)
    }

    @Test
    fun bind_whenMasterIsOff_disablesViewsAndSetsAlpha05() {
        adapter.isMasterOn = false
        val (_, binding) = createAndBindViewHolder(1)

        // 💡 실제 Binding 객체를 사용
        assertEquals(0.5f, binding.root.alpha)
        assertFalse(binding.root.isEnabled)
        assertFalse(binding.alarmTimeTextView.isEnabled)
        assertFalse(binding.deleteAlarmButton.isEnabled)
    }

    // ─────────────────────────────────────────────────────────────
    // 4. 클릭 리스너 분기 커버리지 (100%)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun timeTextViewClick_whenMasterIsOn_callsOnTimeClicked() {
        adapter.isMasterOn = true
        val (_, binding) = createAndBindViewHolder(1) // 12:30

        // 💡 실제 View를 사용
        binding.alarmTimeTextView.performClick()

        verify(exactly = 1) { mockOnTimeClicked(1, "12:30") }
    }

    @Test
    fun timeTextViewClick_whenMasterIsOff_doesNotCallOnTimeClicked() {
        adapter.isMasterOn = false
        val (_, binding) = createAndBindViewHolder(1)

        // 💡 실제 View를 사용 (isEnabled=false 이지만 performClick은 가능)
        binding.alarmTimeTextView.performClick()

        verify(exactly = 0) { mockOnTimeClicked(any(), any()) }
    }

    @Test
    fun deleteButtonClick_whenMasterIsOn_callsOnDeleteClicked() {
        adapter.isMasterOn = true
        val (_, binding) = createAndBindViewHolder(2) // 20:00

        binding.deleteAlarmButton.performClick()

        verify(exactly = 1) { mockOnDeleteClicked(2) }
    }

    @Test
    fun deleteButtonClick_whenMasterIsOff_doesNotCallOnDeleteClicked() {
        adapter.isMasterOn = false
        val (_, binding) = createAndBindViewHolder(2)

        binding.deleteAlarmButton.performClick()

        verify(exactly = 0) { mockOnDeleteClicked(any()) }
    }
}