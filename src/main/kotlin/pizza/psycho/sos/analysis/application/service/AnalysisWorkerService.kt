package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.audit.application.service.AuditLogService
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.project.project.application.port.out.ProjectPort
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import java.util.UUID

/*
 * AnalysisWorkerService
 * - 분석 처리 파이프라인 orchestration
 */
@Service
class AnalysisWorkerService(
    private val analysisLifecycleService: AnalysisLifecycleService,
    private val auditLogService: AuditLogService,
//    private val sprintService: SprintPort,
    private val projectService: ProjectPort,
    private val taskService: TaskPort,
//    private val sprintMetricsCalculator: SprintMetricsCalculator,
//    private val relayServerClient: RelayServerClient, // FastAPI와 통신할 WebClient/RestTemplate
) {
    private val log by loggerDelegate()

    fun processAnalysisJob(jobId: UUID) {
        log.info("🍕 Start analysis job: $jobId")

        var step = AnalysisStep.MARK_RUNNING

        try {
            // 1. QUEUE -> RUNNING
            analysisLifecycleService.markRunning(jobId)

            // 2. 필요한 도메인 데이터 모두 수집 (Sprint, Projects, Tasks, AuditLogs)
            step = AnalysisStep.COLLECT_TARGET_DATA
            val analysisRequest = analysisLifecycleService.getAnalysisRequest(jobId)
//            val sprint = sprintService.findById(...)
//            val projects = projectService.findByIdIn(...)
//            val tasks = taskService.findByIdIn(...)
            val auditLogs = auditLogService.getAuditLogsForAnalysis(analysisRequest.targetId)

            // 2. 메트릭 계산 로직 호출 (JSON v1 페이로드 생성)
            step = AnalysisStep.CALCULATE_METRICS
            // val payload = sprintMetricsCalculator.calculate(sprint, tasks, auditLogs)

            step = AnalysisStep.SEND_TO_RELAY_SERVER
            // 3. 릴레이 서버(FastAPI)로 데이터 전송 (이때 메인서버의 Webhook URL을 함께 넘겨줌)
            // val callbackUrl = "https://relay.psycho-pizza.com/api/v1/internal/analysis/$jobId/callback"
            // https://relay.psycho.pizza/callback?token={token value}&job_id={job id value}
            // relayServerClient.send(payload, callbackUrl)

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
        COLLECT_TARGET_DATA,
        CALCULATE_METRICS,
        SEND_TO_RELAY_SERVER,
    }
}
