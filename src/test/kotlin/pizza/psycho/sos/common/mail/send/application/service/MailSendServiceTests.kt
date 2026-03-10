package pizza.psycho.sos.common.message.channel.mail.send.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.common.message.action.domain.MailActionType
import pizza.psycho.sos.common.message.channel.mail.send.application.command.MailSendCommand
import pizza.psycho.sos.common.message.channel.mail.send.application.model.MailSendRequest
import pizza.psycho.sos.common.message.channel.mail.send.application.port.MailSender
import pizza.psycho.sos.common.message.channel.mail.send.presentation.dto.MailSendStatus
import pizza.psycho.sos.common.message.channel.mail.template.application.service.MailTemplateService
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.MailTemplateData
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.OtpTemplateData
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.WorkspaceInviteTemplateData
import pizza.psycho.sos.common.message.channel.mail.template.domain.model.entity.MailTemplate
import pizza.psycho.sos.common.message.channel.mail.template.domain.model.vo.RenderedMailTemplate
import pizza.psycho.sos.common.message.domain.MessageChannel
import pizza.psycho.sos.common.message.domain.MessageType
import pizza.psycho.sos.common.message.token.application.service.MailAuthTokenService
import pizza.psycho.sos.common.message.token.domain.model.entity.MailAuthToken
import pizza.psycho.sos.common.message.token.infrastructure.config.MailTokenProperties
import java.time.Instant

@ActiveProfiles("test")
class MailSendServiceTests {
    private val mailTemplateService = mockk<MailTemplateService>()
    private val mailAuthTokenService = mockk<MailAuthTokenService>()
    private val mailTokenProperties = MailTokenProperties(verifyBaseUrl = "https://psycho.pizza/api/v1/mails/verify")
    private val mailSender = mockk<MailSender>()
    private val mailSendService =
        MailSendService(
            mailTemplateService,
            mailAuthTokenService,
            mailTokenProperties,
            mailSender,
        )

    @Test
    fun `일반 메일 전송은 템플릿과 토큰 발급 없이 바로 발송한다`() {
        val requestSlot = slot<MailSendRequest>()
        every { mailSender.send(capture(requestSlot)) } returns Unit

        val status =
            mailSendService.send(
                MailSendCommand.General(
                    to = "USER@PSYCHO.PIZZA",
                    subject = "  hello subject  ",
                    htmlContent = "  <p>hello</p>  ",
                    from = "  noreply@psycho.pizza  ",
                ),
            )

        assertEquals(MailSendStatus.SUCCESS, status)
        assertEquals("user@psycho.pizza", requestSlot.captured.to)
        assertEquals("hello subject", requestSlot.captured.subject)
        assertEquals("<p>hello</p>", requestSlot.captured.htmlContent)
        assertEquals("noreply@psycho.pizza", requestSlot.captured.from)
        verify(exactly = 1) { mailSender.send(any()) }
        verify(exactly = 0) { mailTemplateService.getActiveTemplate(any()) }
        verify(exactly = 0) { mailTemplateService.render(any()) }
        verify(exactly = 0) {
            mailAuthTokenService.issue(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `OTP 메일 전송은 템플릿을 렌더링해 발송하고 토큰은 발급하지 않는다`() {
        val template =
            MailTemplate(
                mailType = MessageType.OTP,
                title = "[psycho] 인증번호 안내",
                description = "otp",
                actionType = null,
                tokenAuthEnabled = false,
                tokenExpireHours = null,
                htmlContent = "<p>\${otpCode}</p>",
            )
        val rendered =
            RenderedMailTemplate(
                title = "인증번호 안내",
                htmlContent = "<p>123456</p>",
            )

        every { mailTemplateService.getActiveTemplate(MessageType.OTP) } returns template
        val dataSlot = slot<MailTemplateData>()
        every { mailTemplateService.render(capture(dataSlot)) } returns rendered
        val requestSlot = slot<MailSendRequest>()
        every { mailSender.send(capture(requestSlot)) } returns Unit

        val status =
            mailSendService.send(
                mailType = MessageType.OTP,
                to = "USER@PSYCHO.PIZZA",
                params =
                    mapOf(
                        "otpCode" to "123456",
                        "otpPurpose" to "login",
                        "expiresInMinutes" to "5",
                    ),
            )

        assertEquals(MailSendStatus.SUCCESS, status)
        val capturedData = dataSlot.captured as OtpTemplateData
        assertEquals("123456", capturedData.otpCode)
        assertEquals("login", capturedData.otpPurpose)
        assertEquals(5L, capturedData.expiresInMinutes)
        assertEquals("user@psycho.pizza", requestSlot.captured.to)
        assertEquals("인증번호 안내", requestSlot.captured.subject)
        assertEquals("<p>123456</p>", requestSlot.captured.htmlContent)
        verify(exactly = 1) { mailTemplateService.getActiveTemplate(MessageType.OTP) }
        verify(exactly = 1) { mailTemplateService.render(any()) }
        verify(exactly = 1) { mailSender.send(any()) }
        verify(exactly = 0) {
            mailAuthTokenService.issue(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `메일 전송 시 템플릿을 렌더링하고 발송 요청을 전달한다`() {
        val template =
            MailTemplate(
                mailType = MessageType.WORKSPACE_INVITE,
                title = "invite",
                description = "workspace invite",
                actionType = MailActionType.WORKSPACE_INVITE_ACCEPT,
                tokenAuthEnabled = false,
                tokenExpireHours = null,
                htmlContent = "<p>template</p>",
            )
        val rendered =
            RenderedMailTemplate(
                title = "초대합니다",
                htmlContent = "<p>hello</p>",
            )

        every { mailTemplateService.getActiveTemplate(MessageType.WORKSPACE_INVITE) } returns template
        val dataSlot = slot<MailTemplateData>()
        every { mailTemplateService.render(capture(dataSlot)) } returns rendered
        val requestSlot = slot<MailSendRequest>()
        every { mailSender.send(capture(requestSlot)) } returns Unit

        val status =
            mailSendService.send(
                mailType = MessageType.WORKSPACE_INVITE,
                to = "user@psycho.pizza",
                params =
                    mapOf(
                        "workspaceName" to "psycho",
                        "inviteLink" to "https://psycho.pizza/invite",
                        "inviterName" to "admin",
                        "workspaceId" to "6c8e3e5d-7d2a-4b28-a34b-49a4f1a87f2a",
                        "inviterAccountId" to "f21f98f7-4224-4ab4-8105-a0fdb0f8ac2a",
                    ),
            )

        assertEquals(MailSendStatus.SUCCESS, status)
        val capturedData = dataSlot.captured as WorkspaceInviteTemplateData
        assertEquals("https://psycho.pizza/invite", capturedData.inviteLink)
        assertEquals("user@psycho.pizza", requestSlot.captured.to)
        assertEquals("초대합니다", requestSlot.captured.subject)
        assertEquals("<p>hello</p>", requestSlot.captured.htmlContent)
        verify(exactly = 1) { mailTemplateService.render(any()) }
        verify(exactly = 1) { mailSender.send(any()) }
        verify(exactly = 0) {
            mailAuthTokenService.issue(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `토큰 인증이 활성화되면 링크에 토큰이 포함된다`() {
        val template =
            MailTemplate(
                mailType = MessageType.WORKSPACE_INVITE,
                title = "invite",
                description = "workspace invite",
                actionType = MailActionType.WORKSPACE_INVITE_ACCEPT,
                tokenAuthEnabled = true,
                tokenExpireHours = 24,
                htmlContent = "<p>template</p>",
            )
        val rendered =
            RenderedMailTemplate(
                title = "초대합니다",
                htmlContent = "<p>hello</p>",
            )
        val issued =
            MailAuthToken.issue(
                mailType = MessageType.WORKSPACE_INVITE,
                channel = MessageChannel.EMAIL,
                actionType = MailActionType.WORKSPACE_INVITE_ACCEPT,
                targetEmail = "user@psycho.pizza",
                token = "token123",
                expiredAt = Instant.now().plusSeconds(3600),
            )

        every { mailTemplateService.getActiveTemplate(MessageType.WORKSPACE_INVITE) } returns template
        val dataSlot = slot<MailTemplateData>()
        every { mailTemplateService.render(capture(dataSlot)) } returns rendered
        every {
            mailAuthTokenService.issue(
                MessageType.WORKSPACE_INVITE,
                MessageChannel.EMAIL,
                MailActionType.WORKSPACE_INVITE_ACCEPT,
                "user@psycho.pizza",
                any(),
                any(),
                "6c8e3e5d-7d2a-4b28-a34b-49a4f1a87f2a",
            )
        } returns issued
        val requestSlot = slot<MailSendRequest>()
        every { mailSender.send(capture(requestSlot)) } returns Unit

        val status =
            mailSendService.send(
                mailType = MessageType.WORKSPACE_INVITE,
                to = "user@psycho.pizza",
                params =
                    mapOf(
                        "workspaceName" to "psycho",
                        "workspaceId" to "6c8e3e5d-7d2a-4b28-a34b-49a4f1a87f2a",
                        "inviterAccountId" to "f21f98f7-4224-4ab4-8105-a0fdb0f8ac2a",
                    ),
            )

        val capturedData = dataSlot.captured as WorkspaceInviteTemplateData
        assertEquals("https://psycho.pizza/api/v1/mails/verify?token=token123", capturedData.inviteLink)
        assertEquals(MailSendStatus.SUCCESS, status)
        verify(exactly = 1) { mailTemplateService.render(any()) }
        verify(exactly = 1) { mailSender.send(any()) }
        verify(exactly = 1) {
            mailAuthTokenService.issue(
                MessageType.WORKSPACE_INVITE,
                MessageChannel.EMAIL,
                MailActionType.WORKSPACE_INVITE_ACCEPT,
                "user@psycho.pizza",
                any(),
                any(),
                "6c8e3e5d-7d2a-4b28-a34b-49a4f1a87f2a",
            )
        }
    }

    @Test
    fun `이메일은 소문자로 정규화되어 토큰에 저장된다`() {
        val template =
            MailTemplate(
                mailType = MessageType.WORKSPACE_INVITE,
                title = "invite",
                description = "workspace invite",
                actionType = MailActionType.WORKSPACE_INVITE_ACCEPT,
                tokenAuthEnabled = true,
                tokenExpireHours = 24,
                htmlContent = "<p>template</p>",
            )
        val rendered =
            RenderedMailTemplate(
                title = "초대합니다",
                htmlContent = "<p>hello</p>",
            )
        val issued =
            MailAuthToken.issue(
                mailType = MessageType.WORKSPACE_INVITE,
                channel = MessageChannel.EMAIL,
                actionType = MailActionType.WORKSPACE_INVITE_ACCEPT,
                targetEmail = "user@psycho.pizza",
                token = "token123",
                expiredAt = Instant.now().plusSeconds(3600),
            )

        every { mailTemplateService.getActiveTemplate(MessageType.WORKSPACE_INVITE) } returns template
        every { mailTemplateService.render(any()) } returns rendered
        every {
            mailAuthTokenService.issue(
                MessageType.WORKSPACE_INVITE,
                MessageChannel.EMAIL,
                MailActionType.WORKSPACE_INVITE_ACCEPT,
                "user@psycho.pizza",
                any(),
                any(),
                "6c8e3e5d-7d2a-4b28-a34b-49a4f1a87f2a",
            )
        } returns issued
        every { mailSender.send(any()) } returns Unit

        mailSendService.send(
            mailType = MessageType.WORKSPACE_INVITE,
            to = "USER@PSYCHO.PIZZA",
            params =
                mapOf(
                    "workspaceName" to "psycho",
                    "workspaceId" to "6c8e3e5d-7d2a-4b28-a34b-49a4f1a87f2a",
                    "inviterAccountId" to "f21f98f7-4224-4ab4-8105-a0fdb0f8ac2a",
                ),
        )

        verify(exactly = 1) {
            mailAuthTokenService.issue(
                MessageType.WORKSPACE_INVITE,
                MessageChannel.EMAIL,
                MailActionType.WORKSPACE_INVITE_ACCEPT,
                "user@psycho.pizza",
                any(),
                any(),
                "6c8e3e5d-7d2a-4b28-a34b-49a4f1a87f2a",
            )
        }
    }
}
