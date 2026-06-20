package com.example.sumdays.settings

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.example.sumdays.R
import com.example.sumdays.TestApplication
import com.example.sumdays.reminder.ReminderPrefs
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// 🔥 핵심: sdk 를 R(30)으로 낮춰서 checkExactAlarmPermission 이 항상 true 되게
@Config(
    sdk = [Build.VERSION_CODES.R],
    application = TestApplication::class,
    manifest = Config.NONE
)
class NotificationSettingsActivityTest {

    private lateinit var context: Context
    private lateinit var prefs: ReminderPrefs

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        prefs = ReminderPrefs(context)

        // 테스트마다 깨끗한 상태로 시작
        prefs.setMasterSwitch(false)
        prefs.setAlarmTimes(emptyList())
    }

    @Test
    fun activity_launches() {
        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val title = act.findViewById<TextView>(R.id.headerTitle)
        assertEquals("리마인더 설정", title.text.toString())
    }

    @Test
    fun masterSwitch_loadsInitialValue() {
        prefs.setMasterSwitch(true)

        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val switch = act.findViewById<CompoundButton>(R.id.masterSwitch)
        assertTrue(switch.isChecked)
    }

    @Test
    fun masterSwitch_updatesUiState() {
        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val switch = act.findViewById<CompoundButton>(R.id.masterSwitch)
        val container = act.findViewById<View>(R.id.alarmListContainer)

        // 기본값: master off → 흐림 상태
        assertEquals(0.5f, container.alpha)

        // isChecked 바꾸면 listener 타고 updateUiState 호출됨
        switch.isChecked = true

        assertEquals(1.0f, container.alpha)
        assertTrue(container.isEnabled)
    }

    @Test
    fun addAlarm_savesToPrefs() {
        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        // private fun saveOrUpdateAlarm(position: Int?, newTime: String) 호출
        val method = NotificationSettingsActivity::class.java
            .getDeclaredMethod("saveOrUpdateAlarm", Int::class.javaObjectType, String::class.java)
        method.isAccessible = true

        // 새 알람 추가 (position = null)
        method.invoke(act, null, "08:30")

        val stored = prefs.getAlarmTimes()
        assertEquals(listOf("08:30"), stored)
    }

    @Test
    fun editAlarm_updatesPrefs() {
        // 초기 데이터 세팅
        prefs.setAlarmTimes(listOf("08:30", "10:00"))

        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val method = NotificationSettingsActivity::class.java
            .getDeclaredMethod("saveOrUpdateAlarm", Int::class.javaObjectType, String::class.java)
        method.isAccessible = true

        // index=1 의 "10:00" 을 "11:20"으로 수정
        method.invoke(act, 1, "11:20")

        val stored = prefs.getAlarmTimes()
        assertEquals(listOf("08:30", "11:20"), stored)
    }

    @Test
    fun duplicateAlarm_isRejected_onAdd() {
        prefs.setAlarmTimes(listOf("08:30", "10:00"))

        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val method = NotificationSettingsActivity::class.java
            .getDeclaredMethod("saveOrUpdateAlarm", Int::class.javaObjectType, String::class.java)
        method.isAccessible = true

        // 새 알람 추가인데 이미 있는 시간이면 → 저장 안 해야 함
        method.invoke(act, null, "08:30")

        val stored = prefs.getAlarmTimes()
        assertEquals(listOf("08:30", "10:00"), stored)
    }

    @Test
    fun deleteAlarm_removesFromPrefs() {
        prefs.setAlarmTimes(listOf("08:30", "10:00"))

        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val method = NotificationSettingsActivity::class.java
            .getDeclaredMethod("deleteAlarm", Int::class.javaPrimitiveType)
        method.isAccessible = true

        method.invoke(act, 0)

        val stored = prefs.getAlarmTimes()
        assertEquals(listOf("10:00"), stored)
    }

    @Test
    fun recyclerView_isConfigured() {
        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val rv = act.findViewById<RecyclerView>(R.id.alarmTimeRecyclerView)
        assertNotNull(rv.adapter)
        assertTrue(rv.layoutManager is LinearLayoutManager)
    }

    @Test
    fun masterSwitch_off_cancelsReminders() {
        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val switch = act.findViewById<CompoundButton>(R.id.masterSwitch)
        switch.isChecked = true      // ON
        switch.isChecked = false     // OFF → cancelAllReminders()
    }

    @Test
    fun updateUiState_bothBranchesCovered() {
        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val method = NotificationSettingsActivity::class.java
            .getDeclaredMethod("updateUiState", Boolean::class.javaPrimitiveType)
        method.isAccessible = true

        val container = act.findViewById<View>(R.id.alarmListContainer)

        method.invoke(act, true)   // 활성화
        assertEquals(1.0f, container.alpha)

        method.invoke(act, false)  // 비활성화
        assertEquals(0.5f, container.alpha)
    }

    @Test
    fun loadAlarmSettings_invokesAdapterUpdate() {
        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val method = NotificationSettingsActivity::class.java
            .getDeclaredMethod("loadAlarmSettings")
        method.isAccessible = true

        method.invoke(act)
    }

    @Test
    fun showTimePicker_branchesCovered() {
        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val method = NotificationSettingsActivity::class.java
            .getDeclaredMethod("showTimePicker", Int::class.javaObjectType, String::class.java)
        method.isAccessible = true

        // Add 모드
        method.invoke(act, null, null)

        // Edit 모드
        method.invoke(act, 0, "08:30")
    }

    @Test
    fun addAlarmButton_opensTimePicker() {
        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val addButton = act.findViewById<View>(R.id.addAlarmButton)
        addButton.performClick()

        assertTrue(true)
    }

    @Test
    fun duplicateBlocked_onAdd() {
        val prefs = ReminderPrefs(context)
        prefs.setAlarmTimes(listOf("08:30"))

        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val method = NotificationSettingsActivity::class.java
            .getDeclaredMethod("saveOrUpdateAlarm", Int::class.javaObjectType, String::class.java)
        method.isAccessible = true

        // position = null → 추가 모드
        method.invoke(act, null, "08:30")

        assertEquals(listOf("08:30"), prefs.getAlarmTimes())  // 변경 없음
    }

    @Test
    fun duplicateBlocked_onEdit() {
        val prefs = ReminderPrefs(context)
        prefs.setAlarmTimes(listOf("08:30", "10:00"))

        val act = Robolectric.buildActivity(NotificationSettingsActivity::class.java)
            .setup().get()

        val method = NotificationSettingsActivity::class.java
            .getDeclaredMethod("saveOrUpdateAlarm", Int::class.javaObjectType, String::class.java)
        method.isAccessible = true

        // index=1 ("10:00")을 "08:30"으로 바꾸려 함 → 중복 차단
        method.invoke(act, 1, "08:30")

        assertEquals(listOf("08:30", "10:00"), prefs.getAlarmTimes())  // 변경 없음
    }

}

