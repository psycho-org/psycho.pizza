package pizza.psycho.sos.common.message.channel.mail.send.infrastructure.sender

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import pizza.psycho.sos.common.message.channel.mail.send.application.model.MailSendRequest
import pizza.psycho.sos.common.message.channel.mail.send.application.port.MailSender

@ActiveProfiles("test")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "MAIL_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "MAIL_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "MAIL_TEST_TO", matches = ".+")
class GmailMailSenderIntegrationTests(
    private val mailSender: MailSender,
) {
    @Test
    fun `Gmail SMTP로 메일을 전송한다`() {
        val to = "sobee1403@daum.net"
        val subject = "psycho.pizza mail send test"
        mailSender.send(
            MailSendRequest(
                to = to,
                subject = subject,
                htmlContent = "<p>mail send integration test</p>",
            ),
        )
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerMailProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.mail.host") { "smtp.gmail.com" }
            registry.add("spring.mail.port") { "587" }
            registry.add("spring.mail.username") { System.getenv("MAIL_USERNAME") }
            registry.add("spring.mail.password") { System.getenv("MAIL_PASSWORD") }
            registry.add("spring.mail.properties.mail.smtp.auth") { "true" }
            registry.add("spring.mail.properties.mail.smtp.starttls.enable") { "true" }
            registry.add("mail.sender.type") { "gmail" }
        }
    }
}
