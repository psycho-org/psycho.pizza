package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.analysis.application.port.RelayServerClient
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.util.UUID

/*
 * AnalysisWorkerService
 * - 분석 처리 파이프라인 orchestration
 */
@Service
class AnalysisWorkerService(
    private val analysisLifecycleService: AnalysisLifecycleService,
    private val sprintAnalysisMetricService: SprintAnalysisMetricService,
    private val relayServerClient: RelayServerClient,
) {
    private val log by loggerDelegate()

    fun processAnalysisJob(jobId: UUID) {
        log.info("🍕 Start analysis job: $jobId")

        var step = AnalysisStep.MARK_RUNNING

        try {
            // 1. QUEUE -> RUNNING
            analysisLifecycleService.markRunning(jobId)

            // 2. 메트릭 계산 로직 호출 (JSON v2 페이로드 생성)
            step = AnalysisStep.CALCULATE_METRICS
            sprintAnalysisMetricService.buildInput(jobId)

            // 3. 릴레이 서버로 데이터 전송(x) -> AWS SQS?
            step = AnalysisStep.SEND_TO_RELAY_SERVER
//            relayServerClient.send(jobId, "workspace_id", payload)

            log.info("🚀 Successfully sent analysis job to Relay Server: $jobId")
        } catch (e: Exception) {
            log.error("❌ Analysis job failed: $jobId, step=$step", e)

            analysisLifecycleService.fail(
                id = jobId,
                errorMessage = "FAILED_AT=$step message=${e.message ?: "Unknown error"}",
            )

            throw e
        }
    }

    private enum class AnalysisStep {
        MARK_RUNNING,
        CALCULATE_METRICS,
        SEND_TO_RELAY_SERVER,
    }
}
