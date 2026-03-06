package pizza.psycho.sos.analysis.application.event

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.analysis.application.service.AnalysisJobQueue
import pizza.psycho.sos.analysis.domain.event.AnalysisRequestCreatedEvent

@Component
class AnalysisRequestEventListener(
    private val queue: AnalysisJobQueue,
) {
    // NOTE: DB 트랜잭션이 성공적으로 COMMIT 된 직후에만 실행됩니다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleAnalysisRequestCreated(event: AnalysisRequestCreatedEvent) {
        queue.enqueue(event.jobId)
    }
}
