package com.example.mindlog.features.journal.data.api

import ImageUploadCompleteRequest
import com.example.mindlog.features.journal.data.dto.*
import okhttp3.RequestBody
import retrofit2.Response // 2. Retrofit Response import
import retrofit2.http.*

interface JournalApi {

    @POST("journal/")
    suspend fun createJournal(
        @Body request: JournalRequest
    ): JournalResponse

    @GET("journal/me")
    suspend fun getJournals(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: Int?
    ): JournalListResponse

    /**
     * 특정 ID의 일기 상세 정보를 조회합니다. (GET /journal/{id})
     */
    @GET("journal/{journal_id}")
    suspend fun getJournalById(@Path("journal_id") journalId: Int): JournalItemResponse

    /**
     * 특정 ID의 일기를 수정합니다. (PATCH /journal/{id})
     * @param journalId 수정할 일기의 ID
     * @param request 수정할 내용이 담긴 요청 객체
     */
    @PATCH("journal/{journal_id}")
    suspend fun updateJournal(
        @Path("journal_id") journalId: Int,
        @Body request: UpdateJournalRequest
    ): Response<String> // 3. "Update Success" 문자열을 받기 위해 Response<String> 사용

    /**
     * 특정 ID의 일기를 삭제합니다. (DELETE /journal/{id})
     * @param journalId 삭제할 일기의 ID
     */
    @DELETE("journal/{journal_id}")
    suspend fun deleteJournal(
        @Path("journal_id") journalId: Int
    ): Response<String> // 4. "Delete Success" 문자열을 받기 위해 Response<String> 사용

    /**
     * 이미지 업로드를 위한 Presigned URL을 요청합니다.
     */
    @POST("journal/{journal_id}/image")
    suspend fun generatePresignedUrl(
        @Path("journal_id") journalId: Int,
        @Body request: ImageUploadRequest
    ): PresignedUrlResponse

    /**
     * Presigned URL로 실제 이미지 파일을 업로드합니다.
     * @param url Presigned URL
     * @param fileBody 이미지 파일의 RequestBody
     * @param contentType 이미지의 Content-Type (e.g., "image/jpeg")
     */
    @Headers("No-Auth: true")
    @PUT
    suspend fun uploadImageToS3(
        @Url url: String,
        @Body fileBody: RequestBody,
        @Header("Content-Type") contentType: String
    ): Response<Unit>

    /**
     * 이미지 업로드가 완료되었음을 서버에 알립니다.
     */
    @POST("journal/{journal_id}/image/complete")
    suspend fun completeImageUpload(
        @Path("journal_id") journalId: Int,
        @Body request: ImageUploadCompleteRequest
    ): ImageUploadCompleteResponse

    @GET("journal/search")
    suspend fun searchJournals(
        @Query("start_date") startDate: String?, // YYYY-MM-DD
        @Query("end_date") endDate: String?,   // YYYY-MM-DD
        @Query("title") title: String?,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: Int?
    ): JournalListResponse

    @GET("journal/search-keyword")
    suspend fun searchByKeyword(
        @Query("keyword") keyword: String,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: Int?
    ): JournalListResponse

    @Streaming
    @POST("journal/image/generate")
    suspend fun generateImage(
        @Body request: GenerateImageRequest
    ): GenerateImageResponse

    /**
     * 특정 일기의 키워드 분석을 요청합니다.
     */
    @POST("journal/{journal_id}/analyze")
    suspend fun extractKeywords(
        @Path("journal_id") journalId: Int
    ): KeywordListResponse
}
