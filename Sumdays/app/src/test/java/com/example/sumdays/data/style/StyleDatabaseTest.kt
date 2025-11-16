package com.example.sumdays.data.style

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StyleDatabaseTest {

    @Test
    fun getDatabase_createsSingletonInstance_andReturnsSameInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 첫 호출: INSTANCE == null → synchronized 블록 내부 실행
        val db1 = StyleDatabase.getDatabase(context)
        assertNotNull(db1)

        // 두 번째 호출: INSTANCE != null → 반환만
        val db2 = StyleDatabase.getDatabase(context)
        assertSame(db1, db2)
    }

    @Test
    fun getDatabase_returnsValidDao() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = StyleDatabase.getDatabase(context)

        val dao = db.userStyleDao()
        assertNotNull(dao)
    }
}
