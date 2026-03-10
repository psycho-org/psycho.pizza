package pizza.psycho.sos.analysis.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import pizza.psycho.sos.analysis.application.port.LlmClient
import pizza.psycho.sos.analysis.application.service.dto.AnalysisTarget
import pizza.psycho.sos.analysis.application.service.dto.ParsedAnalysisResult
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest
import pizza.psycho.sos.analysis.domain.exception.LlmResponseParsingException
import pizza.psycho.sos.audit.application.service.AuditLogService
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.util.UUID

/*
 * AnalysisWorkerService
 * - 분석 처리 파이프라인 orchestration
 */
@Service
class AnalysisWorkerService(
    private val analysisLifecycleService: AnalysisLifecycleService,
    private val auditLogService: AuditLogService,
    private val llmClient: LlmClient,
    private val objectMapper: ObjectMapper,
) {
    private val log by loggerDelegate()

    fun processAnalysisJob(jobId: UUID) {
        log.info("🍕 Start analysis job: $jobId")

        var step = AnalysisStep.MARK_RUNNING

        try {
            analysisLifecycleService.markRunning(jobId)

            step = AnalysisStep.LOAD_REQUEST
            val analysisRequest = analysisLifecycleService.getAnalysisRequest(jobId)

            step = AnalysisStep.COLLECT_TARGET_DATA
            val analysisTarget = readAnalysisTarget(analysisRequest)

            step = AnalysisStep.CREATE_PROMPT
            val prompt = createPrompt(analysisTarget)

            step = AnalysisStep.LLM_CALL
            val llmRawResponse = llmClient.analyze(prompt)

            step = AnalysisStep.PARSE_RESULT
            val parsedResult = parseResult(llmRawResponse)

            step = AnalysisStep.SAVE_RESULT
            analysisLifecycleService.complete(jobId, parsedResult)

            log.info("✅ Analysis job completed: $jobId")
        } catch (e: Exception) {
            log.error("❌ Analysis job failed: $jobId, step=$step", e)

            analysisLifecycleService.fail(
                id = jobId,
                errorMessage = "FAILED_AT=$step message=${e.message ?: "Unknown error"}",
            )

            throw e
        }
    }

    private fun readAnalysisTarget(analysisRequest: AnalysisRequest): AnalysisTarget {
        // TODO: sprint snapshot 조회
        // val sprintSnapshot = sprintService.getSprintSnapshot()
        val auditLogs = auditLogService.getAuditLogsForAnalysis(analysisRequest.targetId)

        return AnalysisTarget(
            snapshot = "sprintSnapshot",
            auditLogs = auditLogs,
        )
    }

    // TODO: prompt 고도화
    private fun createPrompt(target: AnalysisTarget): String =
        """
        너는 프로젝트 스프린트 분석 도우미다.
        
        아래 데이터를 기반으로 현재 스프린트 상태를 분석하라.
        반드시 아래의 JSON 포맷으로만 응답해야 하며, 각 필드의 타입(String 또는 Array)을 정확히 지켜라.
        
        {
          "summary": "스프린트 전체 요약 (String)",
          "risks": ["위험 신호 1", "위험 신호 2"], // Array of Strings
          "topIssues": ["우선순위 높은 이슈 1", "우선순위 높은 이슈 2"], // Array of Strings
          "recommendations": ["추천 액션 1", "추천 액션 2"] // Array of Strings
        }
        
        [SNAPSHOT]
        ${target.snapshot}
        
        [AUDIT LOGS]
        ${target.auditLogs.joinToString(separator = "\n")}
        """.trimIndent()

    private fun parseResult(rawResponse: String): ParsedAnalysisResult {
        try {
            return objectMapper.readValue(rawResponse, ParsedAnalysisResult::class.java)
        } catch (e: Exception) {
            throw LlmResponseParsingException(cause = e)
        }
    }

    private enum class AnalysisStep {
        MARK_RUNNING,
        LOAD_REQUEST,
        COLLECT_TARGET_DATA,
        CREATE_PROMPT,
        LLM_CALL,
        PARSE_RESULT,
        SAVE_RESULT,
    }
}
