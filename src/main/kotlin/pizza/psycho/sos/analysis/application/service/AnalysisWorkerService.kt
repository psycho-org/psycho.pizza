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
) {
    private val log by loggerDelegate()

    fun processAnalysisJob(jobId: UUID) {
        log.info("🍕 Start analysis job: $jobId")

        analysisExecutionService.markRunning(jobId)

        try {
            val analysisRequest = analysisExecutionService.getAnalysisRequest(jobId)
            val targetData = analysisTargetReader.read(analysisRequest)
            val prompt = analysisPromptFactory.create(targetData)

            val llmRawResponse = llmClient.analyze(prompt)
            val parsedResult = analysisResultParser.parse(llmRawResponse)

            analysisExecutionService.complete(jobId, parsedResult)

            log.info("✅ Analysis job completed: $jobId")
        } catch (e: Exception) {
            log.error("❌ Analysis job failed: $jobId", e)

            analysisExecutionService.fail(
                jobId = jobId,
                errorMessage = "${e.message}" ?: "Unknown error", // TODO: step 정보 표기
            )

            throw e
        }
        /*
         * TODO:
         * 1. 분석 대상 데이터 수집 (snapshot / audit log)
         * 2. 프롬프트 구성
         * 3. LLM API 호출
         * 4. 결과 파싱 및 검증
         * 5. 결과 저장 & 상태 업데이트 (Analysis Report)
         * 6. 클라이언트에 결과 전달 방법에 대한 고민이 필요함(SSE / API)
         */
    }
}
