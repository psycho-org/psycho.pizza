package pizza.psycho.sos.analysis.infrastructure.llm

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import pizza.psycho.sos.analysis.application.port.LlmClient
import pizza.psycho.sos.analysis.domain.exception.AnalysisGenerationFailedException
import pizza.psycho.sos.analysis.infrastructure.llm.dto.Message
import pizza.psycho.sos.analysis.infrastructure.llm.dto.OpenAiChatRequest
import pizza.psycho.sos.analysis.infrastructure.llm.dto.OpenAiChatResponse
import pizza.psycho.sos.analysis.infrastructure.llm.dto.ResponseFormat
import pizza.psycho.sos.common.support.log.loggerDelegate

// TODO: Spring AI, LangChain4j 도입 고려해보기
@Component
class OpenAiClient(
    private val restClient: RestClient,
    @Value("\${openai.model}")
    private val model: String,
) : LlmClient {
    private val log by loggerDelegate()

    override fun analyze(prompt: String): String {
        val requestBody =
            OpenAiChatRequest(
                model = model,
                temperature = 0.2,
                responseFormat = ResponseFormat(type = "json_object"), // 무조건 JSON으로 반환하도록 강제
                messages =
                    listOf(
                        Message(
                            role = "system",
                            content = "You are an expert project manager. You must respond ONLY in valid JSON format.",
                        ),
                        Message(
                            role = "user",
                            content = prompt,
                        ),
                    ),
            )

        val response =
            restClient
                .post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                // 1. 클라이언트 에러 (4xx) 처리
                .onStatus({ it.is4xxClientError }) { _, response ->
                    log.error("❌ OpenAI API 4xx Error: ${response.statusCode}")
                    throw AnalysisGenerationFailedException("OpenAI 연동 설정 또는 요청에 문제가 있습니다. (status: ${response.statusCode})")
                }
                // 2. 서버 에러 (5xx) 처리
                .onStatus({ it.is5xxServerError }) { _, response ->
                    log.error("❌ OpenAI API 5xx Error: ${response.statusCode}")
                    throw AnalysisGenerationFailedException("OpenAI 서버에 일시적인 장애가 발생했습니다. 잠시 후 다시 시도해주세요.")
                }.body(OpenAiChatResponse::class.java)

        return response
            ?.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?: throw AnalysisGenerationFailedException("OpenAI API로부터 유효한 본문(content)을 받지 못했습니다.")
    }
}
