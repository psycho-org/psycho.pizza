package pizza.psycho.sos.common.message.channel.mail.send.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.message.action.application.model.WorkspaceInviteActionParams
import pizza.psycho.sos.common.message.channel.mail.send.application.command.MailSendCommand
import pizza.psycho.sos.common.message.channel.mail.send.application.model.MailSendRequest
import pizza.psycho.sos.common.message.channel.mail.send.application.port.MailSender
import pizza.psycho.sos.common.message.channel.mail.send.presentation.dto.MailSendStatus
import pizza.psycho.sos.common.message.channel.mail.template.application.service.MailTemplateService
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.WorkspaceInviteTemplateData
import pizza.psycho.sos.common.message.domain.MessageChannel
import pizza.psycho.sos.common.message.domain.MessageType
import pizza.psycho.sos.common.message.token.application.service.MailAuthTokenService
import pizza.psycho.sos.common.message.token.infrastructure.config.MailTokenProperties
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class MailSendService(
    private val mailTemplateService: MailTemplateService,
    private val mailAuthTokenService: MailAuthTokenService,
    private val mailTokenProperties: MailTokenProperties,
    private val mailSender: MailSender,
) {
    fun send(command: MailSendCommand): MailSendStatus =
        when (command) {
            is MailSendCommand.WorkspaceInvite -> sendWorkspaceInvite(command)
        }

    fun send(
        mailType: MessageType,
        to: String,
        params: Map<String, String?> = emptyMap(),
    ): MailSendStatus = send(toCommand(mailType, to, params))

    private fun sendWorkspaceInvite(command: MailSendCommand.WorkspaceInvite): MailSendStatus {
        val normalizedEmail = command.to.trim().lowercase()
        logger.info("Sending mail. mailType={} to={}", command.mailType, normalizedEmail)
        val template = mailTemplateService.getActiveTemplate(command.mailType)
        if (!command.mailType.supportedChannels.contains(MessageChannel.EMAIL)) {
            throw DomainException("channel EMAIL is not supported for mailType=${command.mailType}")
        }
        var inviteLink = command.templateData.inviteLink.trim()
        if (template.tokenAuthEnabled) {
            val actionParams =
                WorkspaceInviteActionParams(
                    workspaceId = command.workspaceId,
                    inviterAccountId = command.requesterAccountId,
                    inviteeEmail = normalizedEmail,
                )
            val baseLink =
                inviteLink.takeIf { it.isNotEmpty() }
                    ?: mailTokenProperties.verifyBaseUrl.trim().takeIf { it.isNotEmpty() }
                    ?: throw DomainException("mail.token.verifyBaseUrl is required")
            val expireHours = template.tokenExpireHours ?: throw DomainException("tokenExpireHours is required")
            val actionType = template.actionType ?: throw DomainException("actionType is required for token auth")
            val token =
                mailAuthTokenService.issue(
                    mailType = command.mailType,
                    channel = MessageChannel.EMAIL,
                    actionType = actionType,
                    targetEmail = normalizedEmail,
                    expiredAt = Instant.now().plus(expireHours, ChronoUnit.HOURS),
                    params = actionParams.toTokenParams(),
                    contextKey = actionParams.workspaceId.toString(),
                )
            inviteLink = appendToken(baseLink, token.token)
        }
        val renderData = command.templateData.copy(inviteLink = inviteLink)
        val rendered = mailTemplateService.render(renderData)
        mailSender.send(
            MailSendRequest(
                to = normalizedEmail,
                subject = rendered.title,
                htmlContent = rendered.htmlContent,
            ),
        )
        return MailSendStatus.SUCCESS
    }

    private fun toCommand(
        mailType: MessageType,
        to: String,
        params: Map<String, String?>,
    ): MailSendCommand =
        when (mailType) {
            MessageType.WORKSPACE_INVITE ->
                MailSendCommand.WorkspaceInvite(
                    to = to,
                    templateData =
                        WorkspaceInviteTemplateData(
                            workspaceName = params.required("workspaceName"),
                            inviteLink = params.optional("inviteLink").orEmpty(),
                            inviterName = params.optional("inviterName"),
                        ),
                    workspaceId = params.requiredUuid("workspaceId"),
                    requesterAccountId = params.requiredUuid("inviterAccountId"),
                )
        }

    private fun Map<String, String?>.required(key: String): String =
        this[key]?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw DomainException("param '$key' is required")

    private fun Map<String, String?>.optional(key: String): String? = this[key]?.trim()?.takeIf { it.isNotEmpty() }

    private fun Map<String, String?>.requiredUuid(key: String): UUID =
        try {
            UUID.fromString(required(key))
        } catch (ex: IllegalArgumentException) {
            throw DomainException("param '$key' must be a valid UUID")
        }

    private fun appendToken(
        url: String,
        token: String,
    ): String {
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}token=$token"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MailSendService::class.java)
    }
}
