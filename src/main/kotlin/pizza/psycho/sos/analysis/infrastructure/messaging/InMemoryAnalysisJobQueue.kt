package pizza.psycho.sos.analysis.infrastructure.messaging

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import pizza.psycho.sos.analysis.application.service.AnalysisJobQueue
import pizza.psycho.sos.analysis.application.service.AnalysisWorkerService
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Component
class InMemoryAnalysisJobQueue(
    private val analysisWorkerService: AnalysisWorkerService,
) : AnalysisJobQueue {
    private val log by loggerDelegate()
    private val queue: BlockingQueue<UUID> = LinkedBlockingQueue(1000)

    // AtomicBoolean: 모든 스레드가 같은 값을 보도록 보장함
    private val workerCount = 4
    private val running = AtomicBoolean(false)
    private val executor = Executors.newFixedThreadPool(workerCount)

    /*
     * 큐에 작업 추가
     */
    override fun enqueue(jobId: UUID) {
        log.info("1️⃣ Enqueue analysis job: $jobId")

        if (!queue.offer(jobId)) {
            throw DomainException("Analysis queue is full")
        }
    }

    /*
     * worker thread 생성
     * - 큐가 비어있으면 스레드를 일시 정지(Blocking)시키고 대기
     */
    @PostConstruct
    fun startWorker() {
        if (!running.compareAndSet(false, true)) return

        repeat(workerCount) { idx ->
            executor.submit {
                log.info("🚀 Analysis worker-$idx started")

                while (running.get()) {
                    try {
                        val jobId = queue.take()
                        log.info("2️⃣ worker-$idx dequeued job: $jobId")

                        analysisWorkerService.processAnalysisJob(jobId)
                    } catch (e: InterruptedException) {
                        if (!running.get()) {
                            log.info("🛑 Analysis worker-$idx stopping")
                            break
                        }
                        Thread.currentThread().interrupt()
                    } catch (e: Exception) {
                        // TODO: retry 정책 필요
                        log.error("❌ Analysis worker-$idx job processing failed", e)
                    }
                }

                log.info("✅ Analysis worker-$idx terminated")
            }
        }
    }

    /*
     * Spring 종료 시 worker thread 종료
     */
    @PreDestroy
    fun stopWorker() {
        // 이미 종료 중이거나 종료됐으면 return (종료 로직 한 번만 실행되도록 하기 위함)
        if (!running.compareAndSet(true, false)) return

        log.info("🛑 Stopping analysis workers...")
        executor.shutdownNow() // 실행 중 스레드 interrupt -> Interrupt Exception 발생

        try {
            // worker thread 들이 최대 5초 안에 종료되는지 기다림
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("⚠️ Analysis workers did not terminate within timeout")
            }
        } catch (e: InterruptedException) {
            // 현재 thread도 종료된 경우 interrupt 상태 복구
            Thread.currentThread().interrupt()
            log.warn("⚠️ Interrupted while waiting for worker shutdown")
        }
    }
}
