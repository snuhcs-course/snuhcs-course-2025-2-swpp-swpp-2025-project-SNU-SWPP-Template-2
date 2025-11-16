package com.example.sumdays

import android.content.Intent
import android.os.Build
import android.widget.ImageButton
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.robolectric.shadows.ShadowApplication
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.O], // LocalDate 사용을 위해 API 26 (Oreo) 이상으로 설정
    packageName = "com.example.sumdays"
)
class StatisticsActivityTest {

    private lateinit var activity: StatisticsActivity
    private lateinit var shadowApplication: ShadowApplication

    // R.id 값은 테스트 코드 내에서 임시로 설정하거나, 실제 프로젝트의 R 클래스를 참조해야 합니다.
    // 여기서는 실제 ID가 존재한다고 가정하고 findViewByID를 사용합니다.

    @Before
    fun setUp() {
        // ActivityController를 사용하여 Activity 생성 및 생명 주기 메소드 호출
        activity = Robolectric.buildActivity(StatisticsActivity::class.java).setup().get()
        shadowApplication = Shadows.shadowOf(activity.application)
    }

    // --- 1. 초기화 및 뷰 설정 테스트 ---

    @Test
    fun `testActivityInitialization_success`() {
        // 액티비티가 null이 아니며 성공적으로 생성되었는지 확인
        assertNotNull(activity)
    }
}