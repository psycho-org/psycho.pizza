package pizza.psycho.sos.common.message.channel.mail.send.infrastructure.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.mail.MailProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@EnableConfigurationProperties(MailProperties::class)
class MailSenderConfig(
    private val mailProperties: MailProperties,
) {
    @Bean
    @ConditionalOnMissingBean(JavaMailSender::class)
    fun javaMailSender(): JavaMailSender {
        val sender = JavaMailSenderImpl()
        sender.host = mailProperties.host
        sender.port = mailProperties.port
        sender.username = mailProperties.username
        sender.password = mailProperties.password
        sender.javaMailProperties.putAll(mailProperties.properties)
        return sender
    }
}
