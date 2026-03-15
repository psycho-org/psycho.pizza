package pizza.psycho.sos.common.queue.publisher

import io.awspring.cloud.sqs.operations.SqsSendOptions
import io.awspring.cloud.sqs.operations.SqsTemplate
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import pizza.psycho.sos.common.queue.config.SqsProperties
import pizza.psycho.sos.common.queue.model.SqsMessagePayload
import java.util.function.Consumer

class SqsMessagePublisherTest {
    private val sqsTemplate: SqsTemplate = mockk(relaxed = true)
    private val properties =
        SqsProperties(
            requestQueueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/request.fifo",
            resultQueueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/result.fifo",
        )
    private val publisher = SqsMessagePublisher(sqsTemplate, properties)

    @Test
    fun `should send message to request queue`() {
        val payload =
            SqsMessagePayload(
                groupId = "job-123",
                deduplicationId = "job-123-dedup",
                body = mapOf("action" to "analyze"),
            )

        publisher.sendToRequestQueue(payload)

        verify(exactly = 1) { sqsTemplate.send(any<Consumer<SqsSendOptions<SqsMessagePayload>>>()) }
    }
}
