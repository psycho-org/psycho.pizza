package pizza.psycho.sos.analysis.application.listener

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.analysis.application.port.AnalysisJobQueueProducer
import pizza.psycho.sos.analysis.application.port.dto.AnalysisJobQueueItem
import pizza.psycho.sos.analysis.application.service.AnalysisJobRecoveryService
import pizza.psycho.sos.analysis.domain.event.AnalysisRequestCreatedEvent
import pizza.psycho.sos.common.support.log.loggerDelegate

@Component
class AnalysisRequestEventListener(
    private val jobProducer: AnalysisJobQueueProducer,
    private val analysisRecoveryService: AnalysisJobRecoveryService,
) {
    private val log by loggerDelegate()

    /*
     * DB 트랜잭션 COMMIT 된 직후 큐에 작업 추가
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleAnalysisRequestCreated(event: AnalysisRequestCreatedEvent) {
        log.info("🍕 Analysis Request Created: workspaceId=${event.workspaceId}, jobId=${event.jobId}")
        jobProducer.enqueue(
            AnalysisJobQueueItem(
                workspaceId = event.workspaceId,
                jobId = event.jobId,
            ),
        )
    }

    /*
     * 서버 재시작 시 큐 복구
     */
    @EventListener(ApplicationReadyEvent::class)
    fun requeueJobs() {
        val jobs = analysisRecoveryService.recoverJobs()

        jobs.forEach { job ->
            jobProducer.enqueue(
                AnalysisJobQueueItem(
                    workspaceId = job.workspaceId,
                    jobId = job.jobId,
                ),
            )
        }

        log.info("🍕 Requeued ${jobs.size} jobs")
    }
}
