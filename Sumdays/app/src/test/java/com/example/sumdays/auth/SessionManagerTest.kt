package com.example.sumdays.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mockito
import org.mockito.Mockito.withSettings
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyStore
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionManagerTest {

    private lateinit var context: Context
    private lateinit var mockKeyStore: KeyStore

    private val PREFS_NAME = "sumdays_keystore_prefs"
    private val KEY_ALIAS = "SumdaysKeyAlias"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockKeyStore = Mockito.mock(
            KeyStore::class.java,
            withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS)
        )

        val fakeSecretKey = SecretKeySpec(ByteArray(32) { 7 }, "AES")
        Mockito.`when`(mockKeyStore.containsAlias(KEY_ALIAS)).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(KEY_ALIAS, null)).thenReturn(fakeSecretKey)

        val ksField = SessionManager::class.java.getDeclaredField("keyStore")
        ksField.isAccessible = true
        ksField.set(SessionManager, mockKeyStore)

        val prefsField = SessionManager::class.java.getDeclaredField("sharedPreferences")
        prefsField.isAccessible = true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        prefsField.set(SessionManager, prefs)
    }

    @Test
    fun save_and_load_token_roundtrip() {
        SessionManager.saveSession(42, "secret_abc")
        assertEquals("secret_abc", SessionManager.getToken())
        assertEquals(42, SessionManager.getUserId())
    }

    @Test
    fun clearSession_clears_all_values() {
        SessionManager.saveSession(10, "tok123")
        SessionManager.clearSession()
        assertNull(SessionManager.getToken())
        assertEquals(-1, SessionManager.getUserId())
        assertFalse(SessionManager.isLoggedIn())
    }

    @Test
    fun isLoggedIn_reflects_saved_state() {
        assertFalse(SessionManager.isLoggedIn())
        SessionManager.saveSession(77, "tokx")
        assertTrue(SessionManager.isLoggedIn())
    }

    @Test
    fun corrupt_sharedprefs_returns_null_token() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("encrypted_auth_token", "BADBASE64").apply()
        prefs.edit().putString("encryption_iv", "BADIV").apply()
        assertNull(SessionManager.getToken())
    }
}
