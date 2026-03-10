package pizza.psycho.sos.common.message.channel.mail.send.application.command

import pizza.psycho.sos.common.message.channel.mail.template.domain.data.OtpTemplateData
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.WorkspaceInviteTemplateData
import pizza.psycho.sos.common.message.domain.MessageType
import java.util.UUID

sealed interface MailSendCommand {
    val to: String

    data class WorkspaceInvite(
        override val to: String,
        val templateData: WorkspaceInviteTemplateData,
        val workspaceId: UUID,
        val requesterAccountId: UUID,
    ) : MailSendCommand {
        val mailType: MessageType = MessageType.WORKSPACE_INVITE
    }

    data class Otp(
        override val to: String,
        val templateData: OtpTemplateData,
    ) : MailSendCommand {
        val mailType: MessageType = MessageType.OTP
    }

    data class General(
        override val to: String,
        val subject: String,
        val htmlContent: String,
        val from: String? = null,
    ) : MailSendCommand
}
