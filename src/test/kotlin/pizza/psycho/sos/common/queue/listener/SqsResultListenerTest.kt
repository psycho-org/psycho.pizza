package pizza.psycho.sos.common.queue.listener

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class SqsResultListenerTest {
    private val listener = SqsResultListener()

    @Test
    fun `should handle message without error`() {
        val message = """{"groupId":"job-123","body":{}}"""
        assertDoesNotThrow {
            listener.onMessage(message)
        }
    }

    @Test
    fun `should handle empty message without error`() {
        assertDoesNotThrow {
            listener.onMessage("")
        }
    }
}
