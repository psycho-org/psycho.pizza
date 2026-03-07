package pizza.psycho.sos.analysis.infrastructure.messaging

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import pizza.psycho.sos.analysis.application.service.AnalysisJobQueue
import pizza.psycho.sos.analysis.application.service.AnalysisWorkerService
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

@Component
class InMemoryAnalysisJobQueue(
    private val analysisWorkerService: AnalysisWorkerService,
) : AnalysisJobQueue {
    private val log by loggerDelegate()
    private val queue: BlockingQueue<UUID> = LinkedBlockingQueue()

    /*
     * 큐에 작업 추가
     */
    override fun enqueue(jobId: UUID) {
        log.info("1️⃣ Enqueue analysis job: $jobId")
        queue.offer(jobId)
    }

    /*
     * worker thread 생성
     * - 큐가 비어있으면 스레드를 일시 정지(Blocking)시키고 대기
     */
    @PostConstruct
    fun startWorker() {
        Thread {
            while (true) {
                try {
                    val jobId = queue.take()
                    log.info("2️⃣ Dequeued job: $jobId")

                    analysisWorkerService.processAnalysisJob(jobId)
                } catch (e: Exception) {
                    log.error("❌ Job processing failed", e)
                }
            }
        }.start()
    }
}
