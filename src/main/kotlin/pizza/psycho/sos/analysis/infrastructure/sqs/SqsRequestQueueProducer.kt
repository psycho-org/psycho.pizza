package pizza.psycho.sos.analysis.infrastructure.sqs

import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pizza.psycho.sos.analysis.application.port.RequestQueueProducer
import pizza.psycho.sos.analysis.application.service.dto.SprintAnalysisInput
import pizza.psycho.sos.analysis.infrastructure.sqs.dto.SqsRequestQueueItem
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.util.UUID

@Component
class SqsRequestQueueProducer(
    private val sqsTemplate: SqsTemplate,
    @param:Value("\${sqs.request-queue-name}")
    private val queueName: String,
) : RequestQueueProducer {
    private val log by loggerDelegate()

    override fun send(
        workspaceId: UUID,
        analysisRequestId: UUID,
        payload: SprintAnalysisInput,
    ) {
        val message =
            SqsRequestQueueItem(
                workspaceId = workspaceId,
                analysisRequestId = analysisRequestId,
                payload = payload,
            )

        log.info(
            "📤 Sending SQS request queue message. queueName={}, workspaceId={}, analysisRequestId={}",
            queueName,
            workspaceId,
            analysisRequestId,
        )

        runCatching {
            sqsTemplate.send {
                it.queue(queueName)
                it.payload(message)
            }
        }.onSuccess {
            log.info(
                "✅ Sent SQS request queue message. queueName={}, workspaceId={}, analysisRequestId={}",
                queueName,
                workspaceId,
                analysisRequestId,
            )
        }.onFailure { e ->
            log.error(
                "❌ Failed to send SQS request queue message. queueName={}, workspaceId={}, analysisRequestId={}",
                queueName,
                workspaceId,
                analysisRequestId,
                e,
            )
            throw e
        }
    }
}
