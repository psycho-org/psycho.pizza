package pizza.psycho.sos.identity.challenge.infrastructure

import org.springframework.stereotype.Component
import pizza.psycho.sos.identity.challenge.domain.OtpGenerator
import java.security.SecureRandom

@Component
class SecureRandomOtpGenerator : OtpGenerator {
    private val secureRandom = SecureRandom()

    override fun generate(length: Int): String {
        require(length > 0) { "OTP length must be positive" }
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(secureRandom.nextInt(10))
        }
        return sb.toString()
    }
}
