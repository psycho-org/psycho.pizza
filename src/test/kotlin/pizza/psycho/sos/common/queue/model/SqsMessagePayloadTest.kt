package pizza.psycho.sos.common.queue.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqsMessagePayloadTest {
    @Test
    fun `should create payload with groupId and auto-generated deduplicationId`() {
        val payload = SqsMessagePayload(groupId = "job-123")
        assertEquals("job-123", payload.groupId)
        assertTrue(payload.deduplicationId.startsWith("job-123-"))
        assertEquals(emptyMap(), payload.body)
    }

    @Test
    fun `should create payload with explicit deduplicationId`() {
        val payload =
            SqsMessagePayload(
                groupId = "job-123",
                deduplicationId = "custom-dedup-id",
                body = mapOf("key" to "value"),
            )
        assertEquals("custom-dedup-id", payload.deduplicationId)
        assertEquals("value", payload.body["key"])
    }
}
