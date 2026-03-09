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
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.WorkspaceInviteTemplateData
import pizza.psycho.sos.common.message.channel.mail.template.domain.model.entity.MailTemplate
import pizza.psycho.sos.common.message.channel.mail.template.domain.spec.MailTemplateSpecRegistry
import pizza.psycho.sos.common.message.channel.mail.template.presentation.dto.MailTemplateResponse
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

    @PostMapping("/send")
    fun send(
        @Valid @RequestBody request: MailSendRequest.WorkspaceInvite,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<MailSendResponse.Sent> {
        val command = request.toCommand(principal)
        return try {
            val status =
                mailSendService.send(
                    command,
                )
            responseOf(
                data =
                    MailSendResponse.Sent(
                        status = status,
                    ),
            )
        } catch (ex: DomainException) {
            throw ex
        } catch (ex: Exception) {
            logger.warn("Failed to send mail. mailType={} to={} reason={}", command.mailType, request.to, ex.message)
            responseOf(
                data =
                    MailSendResponse.Sent(
                        status = MailSendStatus.FAILED,
                    ),
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                message = "Mail send failed",
            )
        }
    }

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

    companion object {
        private val logger = LoggerFactory.getLogger(MailSendController::class.java)
    }
}
