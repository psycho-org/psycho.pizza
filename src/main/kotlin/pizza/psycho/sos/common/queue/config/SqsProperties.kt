package pizza.psycho.sos.common.queue.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.sqs")
data class SqsProperties(
    val requestQueueUrl: String = "",
    val resultQueueUrl: String = "",
    val region: String = "ap-northeast-2",
)
