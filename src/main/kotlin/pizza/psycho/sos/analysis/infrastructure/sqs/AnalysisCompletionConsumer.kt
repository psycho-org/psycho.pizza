package pizza.psycho.sos.analysis.infrastructure.sqs

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import pizza.psycho.sos.analysis.application.service.AnalysisLifecycleService
import pizza.psycho.sos.analysis.application.service.dto.BusinessNotifyPayload
import pizza.psycho.sos.common.support.log.loggerDelegate
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.util.UUID

@Component
class AnalysisCompletionConsumer(
    private val sqsAsyncClient: SqsAsyncClient,
    private val objectMapper: ObjectMapper,
    private val analysisLifecycleService: AnalysisLifecycleService,
    @Value("\${aws.sqs.response-queue-url}")
    private val responseQueueUrl: String,
) {
    private val log by loggerDelegate()

    @Scheduled(fixedDelay = 5000) // 5초마다 폴링
    fun pollMessages() {
        log.info("🔄 Polling SQS response queue. queueUrl={}", responseQueueUrl)

        runBlocking {
            try {
                val request =
                    ReceiveMessageRequest
                        .builder()
                        .queueUrl(responseQueueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(20)
                        .build()

                val response = sqsAsyncClient.receiveMessage(request).await()
                val messages = response.messages()

                log.info("📥 Received {} messages from response queue", messages.size)

                for (message in messages) {
                    processMessage(message)
                }
            } catch (e: Exception) {
                log.error("❌ Failed to poll SQS messages", e)
            }
        }
    }

    private suspend fun processMessage(message: Message) {
        try {
            val payload = objectMapper.readValue(message.body(), BusinessNotifyPayload::class.java)

            // externalRequestId = psycho.pizza jobId (UUID)
            val jobId = UUID.fromString(payload.externalRequestId)

            // 분석 요청 완료 처리 (running -> done) & 분석 리포트 반영
            analysisLifecycleService.complete(
                jobId = jobId,
                runId = payload.jobId.toString(),
                result = payload.result.analysis,
            )

            // 메시지 삭제
            val deleteRequest =
                DeleteMessageRequest
                    .builder()
                    .queueUrl(responseQueueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build()
            sqsAsyncClient.deleteMessage(deleteRequest).await()

            log.info("✅ Processed completion for job: ${payload.externalRequestId}")
        } catch (e: Exception) {
            log.error("❌ Failed to process message: ${message.body()}", e)
        }
    }
}
