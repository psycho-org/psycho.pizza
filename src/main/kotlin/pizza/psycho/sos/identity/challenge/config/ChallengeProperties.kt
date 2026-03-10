package pizza.psycho.sos.identity.challenge.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "challenge")
class ChallengeProperties {
    var otpLength: Int = 6
    var otpTtlSeconds: Long = 300
    var otpMaxAttempts: Int = 5
    var cooldownSeconds: Long = 60
    var confirmationTokenTtlSeconds: Long = 300
}
