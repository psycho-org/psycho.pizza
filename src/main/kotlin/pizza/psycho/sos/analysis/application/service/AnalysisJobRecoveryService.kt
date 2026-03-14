package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.analysis.application.port.dto.AnalysisJobQueueItem
import pizza.psycho.sos.analysis.domain.vo.AnalysisRequestStatus
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisRequestRepository
import pizza.psycho.sos.common.support.log.loggerDelegate

@Service
class AnalysisJobRecoveryService(
    private val analysisRequestRepository: AnalysisRequestRepository,
) {
    private val log by loggerDelegate()

    /*
     * RUNNING 상태의 작업을 QUEUED 상태로 DB 기준 복구
     */
    @Transactional
    fun recoverJobs(): List<AnalysisJobQueueItem> {
        val runningJobs =
            analysisRequestRepository.findAllByStatus(AnalysisRequestStatus.RUNNING)

        runningJobs.forEach {
            it.markAsQueuedForRetry()
        }

        val queuedJobs =
            analysisRequestRepository
                .findAllByStatus(AnalysisRequestStatus.QUEUED)
                .mapNotNull { request ->
                    val jobId = request.id ?: return@mapNotNull null
                    AnalysisJobQueueItem(
                        workspaceId = request.workspaceId,
                        jobId = jobId,
                    )
                }

        log.info("♻️ Recovered RUNNING jobs: ${runningJobs.size}, queued jobs: ${queuedJobs.size}")

        return queuedJobs
    }
}
