package com.doccheck.app.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

data class FindingDto(
    @SerializedName("module") val module: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("description") val description: String
)

data class AnalyzeResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("tamper_score") val tamperScore: Int,
    @SerializedName("findings") val findings: List<FindingDto>,
    @SerializedName("timestamp") val timestamp: Long
)

data class StatsResponseDto(
    @SerializedName("scanned") val scanned: Int,
    @SerializedName("tampered") val tampered: Int,
    @SerializedName("accuracy") val accuracy: Double
)

data class ScanContextDto(
    @SerializedName("scan_id") val scanId: String,
    @SerializedName("tamper_score") val tamperScore: Int,
    @SerializedName("findings") val findings: List<FindingDto>
)

data class ChatRequestDto(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("message") val message: String,
    @SerializedName("scan_context") val scanContext: ScanContextDto? = null
)

data class SourceDto(
    @SerializedName("title") val title: String,
    @SerializedName("page") val page: Int,
    @SerializedName("snippet") val snippet: String
)

data class ChatResponseDto(
    @SerializedName("answer") val answer: String,
    @SerializedName("sources") val sources: List<SourceDto>
)

interface DocCheckApiService {

    @Multipart
    @POST("api/analyze")
    suspend fun analyzeDocument(
        @Part file: MultipartBody.Part,
        @Part("module") module: RequestBody? = null
    ): AnalyzeResponseDto

    @GET("api/stats")
    suspend fun getStats(): StatsResponseDto

    @POST("api/chat")
    suspend fun sendChatMessage(
        @Body request: ChatRequestDto
    ): ChatResponseDto

    @Streaming
    @POST("api/chat/stream")
    suspend fun streamChatMessage(
        @Body request: ChatRequestDto
    ): ResponseBody
}

