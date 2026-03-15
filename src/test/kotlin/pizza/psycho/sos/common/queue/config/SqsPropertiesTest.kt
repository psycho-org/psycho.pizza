package pizza.psycho.sos.common.queue.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SqsPropertiesTest {
    @Test
    fun `should hold queue URLs and region`() {
        val props =
            SqsProperties(
                requestQueueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/request.fifo",
                resultQueueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/result.fifo",
                region = "ap-northeast-2",
            )
        assertEquals("https://sqs.ap-northeast-2.amazonaws.com/123/request.fifo", props.requestQueueUrl)
        assertEquals("https://sqs.ap-northeast-2.amazonaws.com/123/result.fifo", props.resultQueueUrl)
        assertEquals("ap-northeast-2", props.region)
    }

    @Test
    fun `should have empty defaults`() {
        val props = SqsProperties()
        assertEquals("", props.requestQueueUrl)
        assertEquals("", props.resultQueueUrl)
        assertEquals("ap-northeast-2", props.region)
    }
}
