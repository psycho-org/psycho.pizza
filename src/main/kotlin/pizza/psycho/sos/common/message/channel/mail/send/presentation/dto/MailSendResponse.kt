package pizza.psycho.sos.common.message.channel.mail.send.presentation.dto

sealed interface MailSendResponse {
    data class Sent(
        val status: MailSendStatus,
    )
}

enum class MailSendStatus {
    SUCCESS,
    FAILED,
}
