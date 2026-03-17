package pizza.psycho.sos.common.message.channel.mail.send.presentation

import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.message.channel.mail.send.application.command.MailSendCommand
import pizza.psycho.sos.common.message.channel.mail.send.application.service.MailSendService
import pizza.psycho.sos.common.message.channel.mail.send.presentation.dto.MailSendRequest
import pizza.psycho.sos.common.message.channel.mail.send.presentation.dto.MailSendResponse
import pizza.psycho.sos.common.message.channel.mail.send.presentation.dto.MailSendStatus
import pizza.psycho.sos.common.message.channel.mail.template.application.service.MailTemplateService
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.OtpTemplateData
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.WorkspaceInviteTemplateData
import pizza.psycho.sos.common.message.channel.mail.template.domain.model.entity.MailTemplate
import pizza.psycho.sos.common.message.channel.mail.template.domain.spec.MailTemplateSpecRegistry
import pizza.psycho.sos.common.message.channel.mail.template.presentation.dto.MailTemplateResponse
import pizza.psycho.sos.common.message.domain.MessageType
import pizza.psycho.sos.common.message.domain.exception.MessageErrorCode
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal

@RestController
@RequestMapping("/api/v1/mails")
class MailSendController(
    private val mailSendService: MailSendService,
    private val mailTemplateService: MailTemplateService,
) {
    @GetMapping("/templates")
    fun listTemplates(): ApiResponse<List<MailTemplateResponse.Summary>> =
        responseOf(
            data =
                mailTemplateService
                    .getActiveTemplates()
                    .map { it.toSummary() },
        )

    @PostMapping("/send/workspaceinvite")
    fun sendWorkspaceInvite(
        @Valid @RequestBody request: MailSendRequest.WorkspaceInvite,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<MailSendResponse.Sent> = sendCommand(request.toCommand(principal), request.to)

    @PostMapping("/send/general")
    fun sendGeneral(
        @Valid @RequestBody request: MailSendRequest.General,
    ): ApiResponse<MailSendResponse.Sent> = sendCommand(request.toCommand(), request.to)

    @PostMapping("/send/otp")
    fun sendOtp(
        @Valid @RequestBody request: MailSendRequest.Otp,
    ): ApiResponse<MailSendResponse.Sent> = sendCommand(request.toCommand(), request.to)

    @PostMapping("/send")
    fun send(
        @Valid @RequestBody request: MailSendRequest.Send,
    ): ApiResponse<MailSendResponse.Sent> =
        sendByType(
            mailType = request.toMailType(),
            to = request.to,
            params = request.params,
        )

    private fun MailTemplate.toSummary(): MailTemplateResponse.Summary =
        MailTemplateResponse.Summary(
            mailType = mailType.name,
            title = title,
            description = description,
            actionType = actionType?.name,
            tokenAuthEnabled = tokenAuthEnabled,
            tokenExpireHours = tokenExpireHours,
            variables =
                MailTemplateSpecRegistry
                    .get(mailType)
                    .variables
                    .sortedBy { it.name }
                    .map {
                        MailTemplateResponse.Variable(
                            name = it.name,
                            required = it.required,
                            description = it.description,
                        )
                    },
        )

    private fun MailSendRequest.WorkspaceInvite.toCommand(principal: AuthenticatedAccountPrincipal): MailSendCommand =
        MailSendCommand.WorkspaceInvite(
            to = to,
            templateData =
                WorkspaceInviteTemplateData(
                    workspaceName = workspaceName.trim(),
                    inviteLink = inviteLink?.trim().orEmpty(),
                    inviterName = inviterName?.trim()?.takeIf { it.isNotEmpty() },
                ),
            workspaceId = workspaceId,
            requesterAccountId = principal.accountId,
        )

    private fun MailSendRequest.General.toCommand(): MailSendCommand =
        MailSendCommand.General(
            to = to,
            subject = subject,
            htmlContent = htmlContent,
            from = from,
        )

    private fun MailSendRequest.Otp.toCommand(): MailSendCommand =
        MailSendCommand.Otp(
            to = to,
            templateData =
                OtpTemplateData(
                    otpCode = otpCode.trim(),
                    expiresInMinutes = expiresInMinutes,
                    otpPurpose = otpPurpose?.trim()?.takeIf { it.isNotEmpty() },
                ),
        )

    private fun MailSendRequest.Send.toMailType(): MessageType =
        try {
            MessageType.valueOf(mailType.trim().uppercase())
        } catch (ex: IllegalArgumentException) {
            throw DomainException(
                MessageErrorCode.MESSAGE_MAIL_UNSUPPORTED_TYPE,
                "unsupported mailType=$mailType",
                ex,
            )
        }

    private fun sendCommand(
        command: MailSendCommand,
        to: String,
    ): ApiResponse<MailSendResponse.Sent> =
        try {
            val status = mailSendService.send(command)
            responseOf(
                data =
                    MailSendResponse.Sent(
                        status = status,
                    ),
            )
        } catch (ex: DomainException) {
            throw ex
        } catch (ex: Exception) {
            logger.warn("Failed to send mail. commandType={} to={} reason={}", command::class.simpleName, to, ex.message)
            responseOf(
                data =
                    MailSendResponse.Sent(
                        status = MailSendStatus.FAILED,
                    ),
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                message = "Mail send failed",
            )
        }

    private fun sendByType(
        mailType: MessageType,
        to: String,
        params: Map<String, String?>,
    ): ApiResponse<MailSendResponse.Sent> =
        try {
            val status = mailSendService.send(mailType = mailType, to = to, params = params)
            responseOf(
                data =
                    MailSendResponse.Sent(
                        status = status,
                    ),
            )
        } catch (ex: DomainException) {
            throw ex
        } catch (ex: Exception) {
            logger.warn("Failed to send mail. mailType={} to={} reason={}", mailType, to, ex.message)
            responseOf(
                data =
                    MailSendResponse.Sent(
                        status = MailSendStatus.FAILED,
                    ),
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                message = "Mail send failed",
            )
        }

    companion object {
        private val logger = LoggerFactory.getLogger(MailSendController::class.java)
    }
}
