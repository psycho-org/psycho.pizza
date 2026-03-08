package pizza.psycho.sos.analysis.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import pizza.psycho.sos.analysis.application.service.dto.ParsedAnalysisResult
import pizza.psycho.sos.common.handler.DomainException

/*
 * AnalysisResultParser
 * - LLM 응답 파싱
 */
@Component
class AnalysisResultParser(
    private val objectMapper: ObjectMapper,
) {
    fun parse(rawResponse: String): ParsedAnalysisResult =
        try {
            objectMapper.readValue(rawResponse, ParsedAnalysisResult::class.java)
        } catch (e: Exception) {
            throw DomainException("Failed to parse LLM response", e)
        }
}
