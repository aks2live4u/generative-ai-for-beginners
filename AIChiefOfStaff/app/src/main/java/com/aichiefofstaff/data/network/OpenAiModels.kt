package com.aichiefofstaff.data.network

import com.google.gson.annotations.SerializedName

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatCompletionRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<ChatMessageDto>,
    val temperature: Double = 0.7
)

data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<Choice> = emptyList()
) {
    data class Choice(
        val index: Int = 0,
        val message: ChatMessageDto? = null,
        @SerializedName("finish_reason") val finishReason: String? = null
    )
}

data class OpenAiErrorResponse(
    val error: ErrorDetail? = null
) {
    data class ErrorDetail(
        val message: String? = null,
        val type: String? = null
    )
}
