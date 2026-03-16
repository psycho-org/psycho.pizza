package pizza.psycho.sos.common.message.channel.mail.template.domain.data

import pizza.psycho.sos.common.message.domain.MessageType

data class EmailAlreadyExistsTemplateData(
    val email: String,
    val name: String,
    val joinedAt: String,
    val url: String,
) : MailTemplateData {
    override val mailType: MessageType = MessageType.EMAIL_ALREADY_EXISTS

    override fun variables(): Map<String, String?> =
        mapOf(
            "email" to email,
            "name" to name,
            "joinedAt" to joinedAt,
            "url" to url,
        )
}
