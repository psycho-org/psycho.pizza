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
    val analysisLifecycleService: AnalysisLifecycleService,
//    private val sprintAnalysisMetricService: SprintAnalysisMetricService,
    private val analysisRequestService: AnalysisRequestService,
) {
    private val log by loggerDelegate()

    fun processAnalysisJob(jobId: UUID) {
        log.info("🍕 Start analysis job: $jobId")

        var step = AnalysisStep.MARK_RUNNING

        try {
            analysisLifecycleService.markRunning(jobId)

            step = AnalysisStep.LOAD_ANALYSIS_REQUEST
            val analysisRequest = analysisRequestService.getAnalysisRequest(jobId)

            step = AnalysisStep.CALCULATE_METRICS
            // FIXME: 빌드를 위해 sprint service 연결로 임시로 주석 처리했습니다.
//            sprintAnalysisMetricService.buildInput(
//                workspaceId = analysisRequest.workspaceId,
//                sprintId = analysisRequest.targetId,
//            )

            step = AnalysisStep.SEND_MESSAGE_TO_SQS
            // TODO: sqs client 호출 -> 다음 PR로 끊어가겠습니다

            log.info("🚀 Successfully sent analysis job to SQS: $jobId")
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
        LOAD_ANALYSIS_REQUEST,
        CALCULATE_METRICS,
        SEND_MESSAGE_TO_SQS,
    }
}
