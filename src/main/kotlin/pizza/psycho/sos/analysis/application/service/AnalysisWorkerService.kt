package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.util.UUID

/*
 * AnalysisWorkerService
 * - 분석 처리 파이프라인 orchestration
 */
@Service
class AnalysisWorkerService(
    private val analysisExecutionService: AnalysisExecutionService,
    private val analysisTargetReader: AnalysisTargetReader,
    private val analysisPromptFactory: AnalysisPromptFactory,
    private val llmClient: LlmClient,
    private val analysisResultParser: AnalysisResultParser,
) {
    private val log by loggerDelegate()

    fun processAnalysisJob(jobId: UUID) {
        log.info("🍕 Start analysis job: $jobId")

        var step = AnalysisStep.MARK_RUNNING

        try {
            analysisExecutionService.markRunning(jobId)

            step = AnalysisStep.LOAD_REQUEST
            val analysisRequest = analysisExecutionService.getAnalysisRequest(jobId)

            step = AnalysisStep.COLLECT_TARGET_DATA
            val targetData = analysisTargetReader.read(analysisRequest)

            step = AnalysisStep.CREATE_PROMPT
            val prompt = analysisPromptFactory.create(targetData)

            step = AnalysisStep.LLM_CALL
            val llmRawResponse = llmClient.analyze(prompt)

            step = AnalysisStep.PARSE_RESULT
            val parsedResult = analysisResultParser.parse(llmRawResponse)

            step = AnalysisStep.SAVE_RESULT
            analysisExecutionService.complete(jobId, parsedResult)

            log.info("✅ Analysis job completed: $jobId")
        } catch (e: Exception) {
            log.error("❌ Analysis job failed: $jobId, step=$step", e)

            analysisExecutionService.fail(
                jobId = jobId,
                errorMessage = "FAILED_AT=$step message=${e.message ?: "Unknown error"}",
            )

            throw e
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
