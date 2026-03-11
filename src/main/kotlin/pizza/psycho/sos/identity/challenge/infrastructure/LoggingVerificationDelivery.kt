package pizza.psycho.sos.identity.challenge.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pizza.psycho.sos.common.message.channel.mail.send.application.service.MailSendService
import pizza.psycho.sos.identity.account.domain.vo.Email
import pizza.psycho.sos.identity.challenge.application.port.VerificationDelivery
import pizza.psycho.sos.identity.challenge.config.ChallengeProperties
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType

@Component
class LoggingVerificationDelivery(
    private val mailSendService: MailSendService,
    private val properties: ChallengeProperties,
) : VerificationDelivery {
    override fun sendOtp(
        email: Email,
        otp: String,
        operationType: OperationType,
    ) {
        mailSendService.sendOtp(
            to = email.value,
            otpCode = otp,
            otpPurpose = operationType.name,
            expiresInMinutes = properties.otpTtlSeconds / 60,
        )
        log.info("[OTP SENT] operation={}, email={}", operationType, email)
    }

    companion object {
        private val log = LoggerFactory.getLogger(LoggingVerificationDelivery::class.java)
    }
}
