package pizza.psycho.sos.common.queue.publisher

import io.awspring.cloud.sqs.listener.SqsHeaders
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import pizza.psycho.sos.common.queue.config.SqsProperties
import pizza.psycho.sos.common.queue.model.SqsMessagePayload
import pizza.psycho.sos.common.support.log.loggerDelegate

@Component
@ConditionalOnBean(SqsTemplate::class)
class SqsMessagePublisher(
    private val sqsTemplate: SqsTemplate,
    private val properties: SqsProperties,
) {
    private val log by loggerDelegate()

    fun sendToRequestQueue(payload: SqsMessagePayload) {
        log.info("Publishing message to request queue: groupId={}", payload.groupId)
        sqsTemplate.send<SqsMessagePayload> { sendOptions ->
            sendOptions
                .queue(properties.requestQueueUrl)
                .payload(payload)
                .header(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, payload.groupId)
                .header(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, payload.deduplicationId)
        }
    }
}
