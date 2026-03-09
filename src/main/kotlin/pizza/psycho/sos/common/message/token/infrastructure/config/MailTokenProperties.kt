package pizza.psycho.sos.common.message.token.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "mail.token")
data class MailTokenProperties(
    var verifyBaseUrl: String = "",
    var verifySuccessUrl: String = "",
    var verifyFailureUrl: String = "",
)
