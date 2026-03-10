package pizza.psycho.sos.common.message.channel.mail.send.infrastructure.sender

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import pizza.psycho.sos.common.message.channel.mail.send.application.model.MailSendRequest
import pizza.psycho.sos.common.message.channel.mail.send.application.port.MailSender
import pizza.psycho.sos.common.message.domain.MessageChannel

@Component
class GmailMailSender(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username:}") private val defaultFrom: String,
) : MailSender {
    override val channel: MessageChannel = MessageChannel.EMAIL

    override fun send(request: MailSendRequest) {
        val from = request.from?.takeIf { it.isNotBlank() } ?: defaultFrom
        require(from.isNotBlank()) { "Mail from address is required" }

        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, "UTF-8")

        helper.setFrom(from)
        helper.setTo(request.to)
        helper.setSubject(request.subject)
        helper.setText(request.htmlContent, true)

        mailSender.send(message)
    }
}
