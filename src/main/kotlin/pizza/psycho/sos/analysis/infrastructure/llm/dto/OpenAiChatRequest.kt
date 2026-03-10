package pizza.psycho.sos.analysis.infrastructure.llm.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class OpenAiChatRequest(
    val model: String,
    val temperature: Double,
    @JsonProperty("response_format")
    val responseFormat: ResponseFormat? = null,
    val messages: List<Message>,
)

data class ResponseFormat(
    val type: String = "json_object",
)

data class Message(
    val role: String,
    val content: String,
)
