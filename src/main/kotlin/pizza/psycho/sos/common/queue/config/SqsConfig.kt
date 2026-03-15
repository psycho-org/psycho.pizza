package pizza.psycho.sos.common.queue.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "app.sqs", name = ["request-queue-url"])
@EnableConfigurationProperties(SqsProperties::class)
class SqsConfig
