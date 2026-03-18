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
import pizza.psycho.sos.common.support.log.loggerDelegate
import java.net.URI

@Validated
@RestController
@RequestMapping("/api/v1/mails")
class MailAuthController(
    private val mailAuthTokenService: MailAuthTokenService,
    private val mailTokenProperties: MailTokenProperties,
) {
    private val log by loggerDelegate()

    @GetMapping("/verify")
    fun verify(
        @RequestParam @NotBlank token: String,
    ): ResponseEntity<Void> {
        log.info("[ENTRY] mail verify entry: tokenPrefix={}", tokenPrefix(token))

        val result = mailAuthTokenService.verify(token)
        val targetUrl =
            when (result) {
                is MailAuthTokenResult.Verified,
                is MailAuthTokenResult.AlreadyVerified,
                -> workspaceInviteResultUrl(success = true)

                is MailAuthTokenResult.Expired,
                is MailAuthTokenResult.Failed,
                MailAuthTokenResult.NotFound,
                -> workspaceInviteResultUrl(success = false)
            }

        log.info(
            "[EXIT] mail verify exit: tokenPrefix={}, result={}, redirect={}",
            tokenPrefix(token),
            result::class.simpleName,
            targetUrl,
        )
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

    private fun workspaceInviteResultUrl(success: Boolean): String {
        val baseUrl = requireUrl(mailTokenProperties.frontendBaseUrl, "mail.token.frontend-base-url")
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val path = if (success) "workspace-invite/success" else "workspace-invite/failure"
        return "$normalizedBaseUrl/$path"
    }

    private fun tokenPrefix(token: String): String = token.take(8)
}
