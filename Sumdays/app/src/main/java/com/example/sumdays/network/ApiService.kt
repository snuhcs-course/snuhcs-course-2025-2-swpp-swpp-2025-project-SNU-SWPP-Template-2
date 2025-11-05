package com.example.sumdays.network

import com.example.sumdays.daily.diary.AnalysisRequest
import com.example.sumdays.daily.memo.MergeRequest
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.RequestBody

interface ApiService {
    @POST("/api/ai/merge/")
    suspend fun mergeMemos(@Body req: MergeRequest): retrofit2.Response<com.google.gson.JsonObject>
    @Multipart
    @POST("/api/ai/stt/memo") // 서버의 STT 엔드포인트 경로
    fun transcribeAudio(
        // 'audio'는 서버에서 파일을 받을 때 사용할 필드 이름
        @Part audio: MultipartBody.Part
    ): Call<STTResponse>
    @Multipart
    @POST("api/ai/ocr/memo")
    fun extractTextFromImage(
        @Part image: MultipartBody.Part,
        // ★★★ 'type' 필드 추가 (RequestBody 형태) ★★★
        @Part("type") type: RequestBody
    ): Call<OcrResponse>
    @POST("/api/ai/analyze/")
    suspend fun diaryAnalyze(@Body req: AnalysisRequest): retrofit2.Response<com.google.gson.JsonObject>
}

// 응답 DTO (nullable 권장)
data class MergeResponse(
    val merged_memo: String?,
    val count: Int?,
    val end_flag: Boolean?
)
