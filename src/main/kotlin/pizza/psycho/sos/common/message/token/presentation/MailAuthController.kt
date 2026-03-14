package pizza.psycho.sos.common.message.token.presentation

import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.message.domain.exception.MessageErrorCode
import pizza.psycho.sos.common.message.token.application.service.MailAuthTokenService
import pizza.psycho.sos.common.message.token.application.service.dto.MailAuthTokenResult
import pizza.psycho.sos.common.message.token.infrastructure.config.MailTokenProperties
import java.net.URI

@Validated
@RestController
@RequestMapping("/api/v1/mails")
class MailAuthController(
    private val mailAuthTokenService: MailAuthTokenService,
    private val mailTokenProperties: MailTokenProperties,
) {
    @GetMapping("/verify")
    fun verify(
        @RequestParam @NotBlank token: String,
    ): ResponseEntity<Void> {
        val result = mailAuthTokenService.verify(token)
        val targetUrl =
            when (result) {
                is MailAuthTokenResult.Verified,
                is MailAuthTokenResult.AlreadyVerified,
                -> requireUrl(mailTokenProperties.verifySuccessUrl, "mail.token.verify-success-url")

                is MailAuthTokenResult.Expired,
                is MailAuthTokenResult.Failed,
                MailAuthTokenResult.NotFound,
                -> requireUrl(mailTokenProperties.verifyFailureUrl, "mail.token.verify-failure-url")
            }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(targetUrl)).build()
    }

    private fun requireUrl(
        value: String,
        name: String,
    ): String =
        value.trim().takeIf { it.isNotEmpty() }
            ?: throw DomainException(
                MessageErrorCode.MESSAGE_MAIL_VERIFY_URL_REQUIRED,
                "$name is required",
            )
}
