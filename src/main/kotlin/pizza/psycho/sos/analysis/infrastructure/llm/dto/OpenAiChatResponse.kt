package pizza.psycho.sos.analysis.infrastructure.llm.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// OpenAI가 API 응답에 새로운 필드를 추가해도 파싱 에러가 나지 않도록 방어
@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiChatResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Choice(
        val index: Int,
        val message: Message,
        @JsonProperty("finish_reason")
        val finishReason: String?, // 응답이 정상적으로 끝났는지(stop) 토큰 제한에 걸렸는지(length) 확인
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Message(
        val role: String,
        val content: String?, // 간혹 모델이 답변을 거부(refusal)하거나 함수 호출을 할 경우 null일 수 있음
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Usage(
        @JsonProperty("prompt_tokens")
        val promptTokens: Int,
        @JsonProperty("completion_tokens")
        val completionTokens: Int,
        @JsonProperty("total_tokens")
        val totalTokens: Int,
    )
}

/*
{
  "id": "chatcmpl-B9MBs8CjcvOU2jLn4n570S5qMJKcT",
  "object": "chat.completion",
  "created": 1741569952,
  "model": "gpt-5.4",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello! How can I assist you today?",
        "refusal": null,
        "annotations": []
      },
      "logprobs": null,
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 19,
    "completion_tokens": 10,
    "total_tokens": 29,
    "prompt_tokens_details": {
      "cached_tokens": 0,
      "audio_tokens": 0
    },
    "completion_tokens_details": {
      "reasoning_tokens": 0,
      "audio_tokens": 0,
      "accepted_prediction_tokens": 0,
      "rejected_prediction_tokens": 0
    }
  },
  "service_tier": "default"
}
 */
