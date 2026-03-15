package pizza.psycho.sos.common.queue.listener

import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import pizza.psycho.sos.common.support.log.loggerDelegate

@Component
@ConditionalOnBean(SqsTemplate::class)
class SqsResultListener {
    private val log by loggerDelegate()

    @SqsListener("\${app.sqs.result-queue-url}")
    fun onMessage(message: String) {
        log.info("Received result message: {}", message)
        // TODO: 도메인별 핸들러에 위임
    }
}
