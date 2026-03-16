package pizza.psycho.sos.analysis.infrastructure.messaging

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import pizza.psycho.sos.analysis.application.port.AnalysisJobQueueConsumer
import pizza.psycho.sos.analysis.application.service.AnalysisWorkerService
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/*
 * AnalysisWorkerRunner
 * - worker lifecycle 관리 담당
 */
@Component
class AnalysisWorkerRunner(
    private val queueConsumer: AnalysisJobQueueConsumer,
    private val analysisWorkerService: AnalysisWorkerService,
) {
    private val log by loggerDelegate()

    // AtomicBoolean: 모든 스레드가 같은 값을 보도록 보장함
    private val running = AtomicBoolean(false)
    private val workerCount = 4
    private val executor = Executors.newFixedThreadPool(workerCount)

    /*
     * 애플리케이션 시작 시 worker thread 실행
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
                        val job = queueConsumer.take()
                        log.info("2️⃣ worker-$idx dequeued job: jobId=${job.jobId}")

                        analysisWorkerService.processAnalysisJob(job.jobId)
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
     * 애플리케이션 종료 시 worker thread 종료
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
