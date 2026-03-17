package pizza.psycho.sos.analysis.infrastructure.sqs.config

import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@Configuration
class SqsConfig {
    @Bean
    fun sqsTemplate(sqsAsyncClient: SqsAsyncClient): SqsTemplate = SqsTemplate.newTemplate(sqsAsyncClient)
}
