package pizza.psycho.sos.common.message.channel.mail.send.application.command

import pizza.psycho.sos.common.message.channel.mail.template.domain.data.WorkspaceInviteTemplateData
import pizza.psycho.sos.common.message.domain.MessageType
import java.util.UUID

sealed interface MailSendCommand {
    val mailType: MessageType
    val to: String

    data class WorkspaceInvite(
        override val to: String,
        val templateData: WorkspaceInviteTemplateData,
        val workspaceId: UUID,
        val requesterAccountId: UUID,
    ) : MailSendCommand {
        override val mailType: MessageType = MessageType.WORKSPACE_INVITE
    }
}
