package pizza.psycho.sos.identity.challenge.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pizza.psycho.sos.identity.challenge.application.port.VerificationDelivery
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType

@Component
class LoggingVerificationDelivery : VerificationDelivery {
    private val log = LoggerFactory.getLogger(javaClass)

    // TODO: 이메일 모듈 연동 시 교체
    override fun sendOtp(
        email: String,
        otp: String,
        operationType: OperationType,
    ) {
        log.info("[OTP] operation={}, email={}, otp={}", operationType, email, otp)
    }
}
