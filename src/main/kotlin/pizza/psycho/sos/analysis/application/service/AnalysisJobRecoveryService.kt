package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.analysis.domain.vo.AnalysisRequestStatus
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisRequestRepository
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.util.UUID

@Service
class AnalysisJobRecoveryService(
    private val analysisRequestRepository: AnalysisRequestRepository,
) {
    private val log by loggerDelegate()

    /*
     * RUNNING 상태의 작업을 QUEUED 상태로 DB 기준 복구
     */
    @Transactional
    fun recoverJobs(): List<UUID> {
        val runningJobs =
            analysisRequestRepository.findAllByStatus(AnalysisRequestStatus.RUNNING)

        runningJobs.forEach {
            it.markAsQueuedForRetry()
        }

        val queuedJobIds =
            analysisRequestRepository
                .findAllByStatus(AnalysisRequestStatus.QUEUED)
                .mapNotNull { it.id }

        log.info("♻️ Recovered RUNNING jobs: ${runningJobs.size}, queued jobs: ${queuedJobIds.size}")

        return queuedJobIds
    }
}
