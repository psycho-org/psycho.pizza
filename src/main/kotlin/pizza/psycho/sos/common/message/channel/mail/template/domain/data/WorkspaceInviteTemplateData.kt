package pizza.psycho.sos.common.message.channel.mail.template.domain.data

import pizza.psycho.sos.common.message.domain.MessageType

data class WorkspaceInviteTemplateData(
    val workspaceName: String,
    val inviteLink: String,
    val inviterName: String? = null,
) : MailTemplateData {
    override val mailType: MessageType = MessageType.WORKSPACE_INVITE

    override fun variables(): Map<String, String?> =
        mapOf(
            "workspaceName" to workspaceName,
            "inviteLink" to inviteLink,
            "inviterName" to inviterName,
        )
}
