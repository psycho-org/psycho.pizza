package pizza.psycho.sos.common.message.channel.mail.send.application.model

data class MailSendRequest(
    val to: String,
    val subject: String,
    val htmlContent: String,
    val from: String? = null,
)
