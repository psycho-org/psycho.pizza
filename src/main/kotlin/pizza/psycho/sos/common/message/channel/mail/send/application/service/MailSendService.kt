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
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.OtpTemplateData
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.WorkspaceInviteTemplateData
import pizza.psycho.sos.common.message.domain.MessageChannel
import pizza.psycho.sos.common.message.domain.MessageType
import pizza.psycho.sos.common.message.domain.exception.MessageErrorCode
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
            is MailSendCommand.General -> sendGeneral(command)
            is MailSendCommand.Otp -> sendOtp(command)
            is MailSendCommand.WorkspaceInvite -> sendWorkspaceInvite(command)
        }

    fun send(
        mailType: MessageType,
        to: String,
        params: Map<String, String?> = emptyMap(),
    ): MailSendStatus = send(toCommand(mailType, to, params))

    fun sendOtp(
        to: String,
        otpCode: String,
        otpPurpose: String? = null,
        expiresInMinutes: Long = DEFAULT_OTP_EXPIRES_IN_MINUTES,
    ): MailSendStatus {
        if (expiresInMinutes <= 0) {
            throw DomainException(
                MessageErrorCode.MESSAGE_POSITIVE_NUMBER_REQUIRED,
                "expiresInMinutes must be positive",
            )
        }
        return send(
            MailSendCommand.Otp(
                to = to,
                templateData =
                    OtpTemplateData(
                        otpCode = otpCode.trim(),
                        expiresInMinutes = expiresInMinutes,
                        otpPurpose = otpPurpose?.trim()?.takeIf { it.isNotEmpty() },
                    ),
            ),
        )
    }

    private fun sendGeneral(command: MailSendCommand.General): MailSendStatus {
        val normalizedEmail = command.to.trim().lowercase()
        val subject =
            command.subject.trim().takeIf { it.isNotEmpty() }
                ?: throw DomainException(
                    MessageErrorCode.MESSAGE_REQUIRED_FIELD_MISSING,
                    "subject is required",
                )
        val htmlContent =
            command.htmlContent.trim().takeIf { it.isNotEmpty() }
                ?: throw DomainException(
                    MessageErrorCode.MESSAGE_REQUIRED_FIELD_MISSING,
                    "htmlContent is required",
                )
        val from = command.from?.trim()?.takeIf { it.isNotEmpty() }

        logger.info("Sending general mail. to={}", normalizedEmail)
        mailSender.send(
            MailSendRequest(
                to = normalizedEmail,
                subject = subject,
                htmlContent = htmlContent,
                from = from,
            ),
        )
        return MailSendStatus.SUCCESS
    }

    private fun sendOtp(command: MailSendCommand.Otp): MailSendStatus {
        val normalizedEmail = command.to.trim().lowercase()
        logger.info("Sending OTP mail. to={}", normalizedEmail)
        val template = mailTemplateService.getActiveTemplate(command.mailType)
        if (!command.mailType.supportedChannels.contains(MessageChannel.EMAIL)) {
            throw DomainException(
                MessageErrorCode.MESSAGE_CHANNEL_NOT_SUPPORTED,
                "channel EMAIL is not supported for mailType=${command.mailType}",
            )
        }
        if (template.tokenAuthEnabled) {
            throw DomainException(
                MessageErrorCode.MESSAGE_TOKEN_AUTH_NOT_SUPPORTED,
                "token auth is not supported for mailType=${command.mailType}",
            )
        }

        val rendered = mailTemplateService.render(command.templateData)
        mailSender.send(
            MailSendRequest(
                to = normalizedEmail,
                subject = rendered.title,
                htmlContent = rendered.htmlContent,
            ),
        )
        return MailSendStatus.SUCCESS
    }

    private fun sendWorkspaceInvite(command: MailSendCommand.WorkspaceInvite): MailSendStatus {
        val normalizedEmail = command.to.trim().lowercase()
        logger.info("Sending mail. mailType={} to={}", command.mailType, normalizedEmail)
        val template = mailTemplateService.getActiveTemplate(command.mailType)
        if (!command.mailType.supportedChannels.contains(MessageChannel.EMAIL)) {
            throw DomainException(
                MessageErrorCode.MESSAGE_CHANNEL_NOT_SUPPORTED,
                "channel EMAIL is not supported for mailType=${command.mailType}",
            )
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
                    ?: throw DomainException(
                        MessageErrorCode.MESSAGE_MAIL_VERIFY_URL_REQUIRED,
                        "mail.token.verifyBaseUrl is required",
                    )
            val expireHours =
                template.tokenExpireHours
                    ?: throw DomainException(
                        MessageErrorCode.MESSAGE_MAIL_TOKEN_EXPIRE_HOURS_REQUIRED,
                        "tokenExpireHours is required",
                    )
            val actionType =
                template.actionType
                    ?: throw DomainException(
                        MessageErrorCode.MESSAGE_MAIL_ACTION_TYPE_REQUIRED,
                        "actionType is required for token auth",
                    )
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
            MessageType.OTP ->
                MailSendCommand.Otp(
                    to = to,
                    templateData =
                        OtpTemplateData(
                            otpCode = params.requiredAny("otpCode", "OTP", "otp"),
                            expiresInMinutes = params.optionalPositiveLong("expiresInMinutes") ?: DEFAULT_OTP_EXPIRES_IN_MINUTES,
                            otpPurpose = params.optional("otpPurpose"),
                            title = params.optional("title") ?: OtpTemplateData.DEFAULT_TITLE,
                        ),
                )

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
            ?: throw DomainException(
                MessageErrorCode.MESSAGE_REQUIRED_FIELD_MISSING,
                "param '$key' is required",
            )

    private fun Map<String, String?>.requiredAny(vararg keys: String): String =
        keys
            .asSequence()
            .mapNotNull { key -> this[key]?.trim()?.takeIf { it.isNotEmpty() } }
            .firstOrNull()
            ?: throw DomainException(
                MessageErrorCode.MESSAGE_REQUIRED_FIELD_MISSING,
                "one of params [${keys.joinToString()}] is required",
            )

    private fun Map<String, String?>.optional(key: String): String? = this[key]?.trim()?.takeIf { it.isNotEmpty() }

    private fun Map<String, String?>.optionalPositiveLong(key: String): Long? =
        optional(key)?.let {
            val value =
                it.toLongOrNull()
                    ?: throw DomainException(
                        MessageErrorCode.MESSAGE_INVALID_NUMBER_PARAM,
                        "param '$key' must be a valid number",
                    )
            if (value <= 0) {
                throw DomainException(
                    MessageErrorCode.MESSAGE_POSITIVE_NUMBER_REQUIRED,
                    "param '$key' must be positive",
                )
            }
            value
        }

    private fun Map<String, String?>.requiredUuid(key: String): UUID =
        try {
            UUID.fromString(required(key))
        } catch (ex: IllegalArgumentException) {
            throw DomainException(
                MessageErrorCode.MESSAGE_INVALID_UUID_PARAM,
                "param '$key' must be a valid UUID",
                ex,
            )
        }

    private fun appendToken(
        url: String,
        token: String,
    ): String {
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}token=$token"
    }

    companion object {
        private const val DEFAULT_OTP_EXPIRES_IN_MINUTES = 5L
        private val logger = LoggerFactory.getLogger(MailSendService::class.java)
    }
}
