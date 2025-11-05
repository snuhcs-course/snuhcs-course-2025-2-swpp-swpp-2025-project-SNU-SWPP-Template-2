package com.example.sumdays.settings

import android.content.Intent
import android.os.Build
import android.widget.ImageView
import android.widget.TextView
import android.view.View
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.example.sumdays.R
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ThemeSettingsActivityTest {

    private fun launch(): ThemeSettingsActivity {
        val controller = buildActivity(
            ThemeSettingsActivity::class.java,
            Intent(getApplicationContext(), ThemeSettingsActivity::class.java)
        )
        return controller.setup().get()
    }

    @Test
    fun onCreate_setsHeaderTitle() {
        val activity = launch()
        val titleView = activity.findViewById<TextView>(R.id.headerTitle)
        assertThat(titleView.text.toString(), `is`("테마 설정"))
    }

    @Test
    fun clickingBackIcon_finishesActivity() {
        val activity = launch()
        val back = activity.findViewById<ImageView>(R.id.headerBackIcon)
        back.performClick()
        assertThat(activity.isFinishing, `is`(true))
    }

    // --- 선택 상태/배경 리소스 검증 유틸 ---
    private fun bgId(view: View): Int {
        val drawable = view.background ?: return -1
        return Shadows.shadowOf(drawable).createdFromResId
    }
    private fun assertSelectedSystem(activity: ThemeSettingsActivity) {
        val sys = activity.findViewById<View>(R.id.themeSystem)
        val light = activity.findViewById<View>(R.id.themeLight)
        val dark = activity.findViewById<View>(R.id.themeDark)

        assertThat(bgId(sys), `is`(R.drawable.settings_theme_btn_selected))
        assertThat(bgId(light), `is`(R.drawable.settings_theme_btn_unselected))
        assertThat(bgId(dark), `is`(R.drawable.settings_theme_btn_unselected))
    }

    private fun assertSelectedLight(activity: ThemeSettingsActivity) {
        val sys = activity.findViewById<View>(R.id.themeSystem)
        val light = activity.findViewById<View>(R.id.themeLight)
        val dark = activity.findViewById<View>(R.id.themeDark)

        assertThat(bgId(sys), `is`(R.drawable.settings_theme_btn_unselected))
        assertThat(bgId(light), `is`(R.drawable.settings_theme_btn_selected))
        assertThat(bgId(dark), `is`(R.drawable.settings_theme_btn_unselected))
    }

    private fun assertSelectedDark(activity: ThemeSettingsActivity) {
        val sys = activity.findViewById<View>(R.id.themeSystem)
        val light = activity.findViewById<View>(R.id.themeLight)
        val dark = activity.findViewById<View>(R.id.themeDark)

        assertThat(bgId(sys), `is`(R.drawable.settings_theme_btn_unselected))
        assertThat(bgId(light), `is`(R.drawable.settings_theme_btn_unselected))
        assertThat(bgId(dark), `is`(R.drawable.settings_theme_btn_selected))
    }

    @Test
    fun defaultSelection_isSystem_andUIReflects() {
        val activity = launch()
        assertSelectedSystem(activity)
    }

    @Test
    fun clickingLight_selectsLight_andUpdatesUI() {
        val activity = launch()
        activity.findViewById<View>(R.id.themeLight).performClick()
        assertSelectedLight(activity)
    }

    @Test
    fun clickingDark_selectsDark_andUpdatesUI() {
        val activity = launch()
        activity.findViewById<View>(R.id.themeDark).performClick()
        assertSelectedDark(activity)
    }

    @Test
    fun clickingSystem_again_setsSystem_andUpdatesUI() {
        val activity = launch()
        // 다른 걸 한번 눌러 상태를 바꾸고…
        activity.findViewById<View>(R.id.themeDark).performClick()
        assertSelectedDark(activity)
        // 다시 System 선택
        activity.findViewById<View>(R.id.themeSystem).performClick()
        assertSelectedSystem(activity)
    }
}
