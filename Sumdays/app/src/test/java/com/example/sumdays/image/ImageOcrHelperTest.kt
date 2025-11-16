package com.example.sumdays.image

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.example.sumdays.network.ApiClient
import com.example.sumdays.network.ApiService
import com.example.sumdays.network.OcrResponse
import io.mockk.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ImageOcrHelperTest {

    private lateinit var activity: AppCompatActivity
    private lateinit var helper: ImageOcrHelper
    private lateinit var apiMock: ApiService

    private var successFired = false
    private var failedFired = false
    private var lastText: String? = null

    @Before
    fun setup() {
        // Activity는 RESUMED 이전에 helper를 생성해야 함
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.create()
        activity = controller.get()

        // ApiClient.api 목 세팅
        apiMock = mockk(relaxed = true)
        mockkObject(ApiClient)
        every { ApiClient.api } returns apiMock

        // 콜백 플래그 초기화
        successFired = false
        failedFired = false
        lastText = null

        // helper를 RESUMED 이전에 생성
        helper = ImageOcrHelper(
            activity = activity,
            onOcrSuccess = { txt -> successFired = true; lastText = txt },
            onOcrFailed = { msg -> failedFired = true; lastText = msg },
            onImageSelected = { /* 이 테스트는 직접 private method를 호출하므로 사용 안 함 */ }
        )

        // 이후에 start/resume
        controller.start().resume().visible()
    }

    @After
    fun tearDown() {
        unmockkObject(ApiClient)
        unmockkAll()
    }

    /** temp 파일을 만들고 file:// Uri를 반환 */
    private fun makeTempImageUri(bytes: ByteArray, name: String = "img.bin"): Uri {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val f = File(ctx.cacheDir, name)
        f.writeBytes(bytes)
        return Uri.fromFile(f)
    }

    // 사설 메서드(uploadImageForOcr)를 리플렉션으로 직접 호출 (ActivityResult 경로 우회)
    private fun callPrivateUpload(imageUri: Uri, type: String = "extract") {
        helper.javaClass.getDeclaredField("analysisType").apply {
            isAccessible = true
            set(helper, type)
        }
        val m = helper.javaClass.getDeclaredMethod("uploadImageForOcr", Uri::class.java)
        m.isAccessible = true
        m.invoke(helper, imageUri)
    }

    @Test
    fun success_response_triggers_onOcrSuccess() {
        val uri = makeTempImageUri("IMG".toByteArray(), "ok.jpg")

        val callMock = mockk<Call<OcrResponse>>(relaxed = true)
        every { apiMock.extractTextFromImage(any<MultipartBody.Part>(), any<RequestBody>()) } returns callMock
        every { callMock.enqueue(any()) } answers {
            val cb = arg<Callback<OcrResponse>>(0)
            cb.onResponse(
                callMock,
                Response.success(OcrResponse(success = true, type = "extract", result = "HELLO"))
            )
        }

        callPrivateUpload(uri)

        assertTrue(successFired)
        assertEquals("HELLO", lastText)
    }

    @Test
    fun failure_response_triggers_onOcrFailed() {
        val uri = makeTempImageUri("IMG".toByteArray(), "fail.jpg")

        val callMock = mockk<Call<OcrResponse>>(relaxed = true)
        every { apiMock.extractTextFromImage(any(), any()) } returns callMock
        every { callMock.enqueue(any()) } answers {
            val cb = arg<Callback<OcrResponse>>(0)
            cb.onResponse(
                callMock,
                Response.success(OcrResponse(success = false, type = "extract", result = null))
            )
        }

        callPrivateUpload(uri)

        assertTrue(failedFired)
        // lastText는 헬퍼 내부 에러 메시지라 정확 문자열 비교는 생략
    }

    @Test
    fun network_failure_triggers_onOcrFailed() {
        val uri = makeTempImageUri("IMG".toByteArray(), "net.jpg")

        val callMock = mockk<Call<OcrResponse>>(relaxed = true)
        every { apiMock.extractTextFromImage(any(), any()) } returns callMock
        every { callMock.enqueue(any()) } answers {
            val cb = arg<Callback<OcrResponse>>(0)
            cb.onFailure(callMock, RuntimeException("boom"))
        }

        callPrivateUpload(uri)

        assertTrue(failedFired)
    }

    @Test
    fun null_InputStream_triggers_onOcrFailed() {
        // 파일을 일부러 만들지 않음 → openInputStream(fileUri) 시도하면 FileNotFound로 실패
        val missing = Uri.fromFile(File("/no/such/file.jpg"))

        val callMock = mockk<Call<OcrResponse>>(relaxed = true)
        every { apiMock.extractTextFromImage(any(), any()) } returns callMock
        // 호출되면 안 되지만, 혹시 몰라 relaxed로 둠

        callPrivateUpload(missing)

        assertTrue(failedFired)
    }
}
