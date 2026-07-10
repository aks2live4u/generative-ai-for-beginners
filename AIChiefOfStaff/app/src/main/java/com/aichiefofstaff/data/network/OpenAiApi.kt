package com.aichiefofstaff.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") bearerToken: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}
