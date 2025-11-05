package com.example.sumdays.image

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.network.ApiClient
import com.example.sumdays.network.ApiService
import com.example.sumdays.network.OcrResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * 이미지 선택 및 OCR 처리를 관리하는 헬퍼 클래스 (람다 콜백 방식).
 */
class ImageOcrHelper(
    private val activity: AppCompatActivity,
    // ★★★ 콜백 람다 함수 정의 ★★★
    private val onOcrSuccess: (extractedText: String) -> Unit,
    private val onOcrFailed: (errorMessage: String) -> Unit,
    private val onImageSelected: (uri: Uri) -> Unit = {} // 이미지 선택 시 호출 (선택적)
) {
    private var analysisType: String = "extract" // 기본값
    // --- 이미지 선택 런처 ---
    // ActivityResultLauncher는 Activity/Fragment 내에서 초기화되어야 하므로,
    // Activity로부터 registerForActivityResult 결과를 받아옵니다.
    private val pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                Log.d("ImageOcrHelper", "Selected Image URI: $uri")
                onImageSelected(uri) // 이미지 선택 콜백 호출
                // 선택된 이미지 URI를 사용하여 OCR 처리 함수 호출
                uploadImageForOcr(uri)
            } else {
                Log.d("ImageOcrHelper", "Image selection cancelled")
            }
        }

    /**
     * 이미지 선택기를 실행합니다. Activity의 버튼 클릭 리스너에서 호출됩니다.
     */
    fun selectImage(type: String) {
        this.analysisType = type
        // 이미지 타입만 선택하도록 제한
        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    /**
     * 선택된 이미지 URI를 사용하여 서버에 OCR 요청을 보냅니다.
     */
    private fun uploadImageForOcr(imageUri: Uri) {
        val contentResolver = activity.contentResolver
        try {
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val imageBytes = inputStream.readBytes()
                val mimeType = contentResolver.getType(imageUri)
                val requestBody = imageBytes.toRequestBody(mimeType?.toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData(
                    "image",
                    "upload_image.${getImageExtension(imageUri, contentResolver)}",
                    requestBody
                )
                val typeValue = analysisType // 또는 "describe" 등 API 요구사항에 맞게 설정
// 문자열을 RequestBody로 변환 (미디어 타입은 "text/plain")
                val typePart = typeValue.toRequestBody("text/plain".toMediaTypeOrNull())
                // API 호출
                ApiClient.api.extractTextFromImage(imagePart, typePart).enqueue(object : Callback<OcrResponse> {
                    override fun onResponse(call: Call<OcrResponse>, response: Response<OcrResponse>) {
                        if (response.isSuccessful) {
                            val ocrResponse = response.body()
                            if (ocrResponse != null && ocrResponse.success && !ocrResponse.result.isNullOrEmpty()) {
                                // ★★★ 성공 콜백 호출 ★★★
                                onOcrSuccess(ocrResponse.result)
                            } else {
                                val message = ocrResponse?.result ?: "서버에서 텍스트 추출 실패"
                                Log.e("ImageOcrHelper", "OCR API Error: $message")
                                // ★★★ 실패 콜백 호출 ★★★
                                onOcrFailed(message)
                            }
                        } else {
                            val errorMsg = "서버 통신 오류: ${response.code()}"
                            Log.e("ImageOcrHelper", "OCR API HTTP Error: ${response.code()} ${response.message()}")
                            onOcrFailed(errorMsg)
                        }
                    }

                    override fun onFailure(call: Call<OcrResponse>, t: Throwable) {
                        val errorMsg = "네트워크 오류: ${t.message}"
                        Log.e("ImageOcrHelper", "OCR API Network Failure", t)
                        onOcrFailed(errorMsg)
                    }
                })

            } ?: run {
                Log.e("ImageOcrHelper", "Cannot open InputStream from URI: $imageUri")
                onOcrFailed("이미지 파일을 열 수 없습니다.")
            }
        } catch (e: Exception) {
            Log.e("ImageOcrHelper", "Error processing image for OCR", e)
            onOcrFailed("이미지 처리 오류: ${e.message}")
        }
    }

    // URI로부터 확장자 추측
    private fun getImageExtension(uri: Uri, contentResolver: ContentResolver): String {
        return contentResolver.getType(uri)?.substringAfterLast('/') ?: "jpg"
    }
}
