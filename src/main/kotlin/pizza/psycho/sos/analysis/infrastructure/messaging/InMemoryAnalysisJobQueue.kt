package pizza.psycho.sos.analysis.infrastructure.messaging

import org.springframework.stereotype.Component
import pizza.psycho.sos.analysis.application.port.AnalysisJobQueueConsumer
import pizza.psycho.sos.analysis.application.port.AnalysisJobQueueProducer
import pizza.psycho.sos.analysis.domain.exception.AnalysisErrorCode
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/*
 * InMemoryAnalysisJobQueue
 * - 분석 작업의 jobId를 메모리에 저장하고 worker가 가져갈 수 있게 하는 큐 어댑터
 */
@Component
class InMemoryAnalysisJobQueue :
    AnalysisJobQueueProducer,
    AnalysisJobQueueConsumer {
    private val log by loggerDelegate()
    private val queue: BlockingQueue<UUID> = LinkedBlockingQueue(1000)

    /*
     * 큐에 작업 추가
     */
    override fun enqueue(jobId: UUID) {
        log.info("1️⃣ Enqueue analysis job: $jobId")

        if (!queue.offer(jobId)) {
            throw DomainException(AnalysisErrorCode.ANALYSIS_JOB_QUEUE_FULL)
        }
    }

    /*
     * 큐에서 작업 소비
     */
    override fun take(): UUID = queue.take()
}
