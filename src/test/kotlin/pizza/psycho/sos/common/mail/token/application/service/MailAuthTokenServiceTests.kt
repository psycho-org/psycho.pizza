package pizza.psycho.sos.common.message.token.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.common.message.action.application.model.MailActionRequest
import pizza.psycho.sos.common.message.action.domain.MailActionType
import pizza.psycho.sos.common.message.domain.MessageChannel
import pizza.psycho.sos.common.message.domain.MessageType
import pizza.psycho.sos.common.message.token.application.event.MailAuthVerifiedEvent
import pizza.psycho.sos.common.message.token.application.service.dto.MailAuthTokenResult
import pizza.psycho.sos.common.message.token.domain.model.entity.MailAuthFailureReason
import pizza.psycho.sos.common.message.token.domain.model.entity.MailAuthToken
import pizza.psycho.sos.common.message.token.domain.model.entity.MailAuthTokenParam
import pizza.psycho.sos.common.message.token.domain.repository.MailAuthTokenRepository
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
class MailAuthTokenServiceTests {
    private val repository = mockk<MailAuthTokenRepository>()
    private val entityManager = mockk<EntityManager>(relaxed = true)
    private val publisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val service = MailAuthTokenService(repository, entityManager, publisher)

    @Test
    fun `만료된 토큰은 EXPIRED로 응답하고 이벤트를 발행하지 않는다`() {
        val entity =
            MailAuthToken.issue(
                mailType = MessageType.WORKSPACE_INVITE,
                channel = MessageChannel.EMAIL,
                actionType = MailActionType.WORKSPACE_INVITE_ACCEPT,
                targetEmail = "user@psycho.pizza",
                token = "expired",
                expiredAt = Instant.now().minusSeconds(10),
            )

        every { repository.findByTokenWithParams("expired") } returns entity

        val result = service.verify("expired")

        assertTrue(result is MailAuthTokenResult.Expired)
        verify(exactly = 0) { publisher.publishEvent(any()) }
        assertEquals(MailAuthFailureReason.EXPIRED, entity.failureReason)
    }

    @Test
    fun `이미 인증된 토큰은 AlreadyVerified로 응답하고 이벤트를 발행하지 않는다`() {
        val entity =
            MailAuthToken.issue(
                mailType = MessageType.WORKSPACE_INVITE,
                channel = MessageChannel.EMAIL,
                actionType = MailActionType.WORKSPACE_INVITE_ACCEPT,
                targetEmail = "user@psycho.pizza",
                token = "verified",
                expiredAt = Instant.now().plusSeconds(3600),
            )
        entity.verify()

        every { repository.findByTokenWithParams("verified") } returns entity

        val result = service.verify("verified")

        assertTrue(result is MailAuthTokenResult.AlreadyVerified)
        verify(exactly = 0) { publisher.publishEvent(any()) }
    }

    @Test
    fun `실패 처리된 토큰은 Failed로 응답하고 이벤트를 발행하지 않는다`() {
        val entity =
            MailAuthToken.issue(
                mailType = MessageType.WORKSPACE_INVITE,
                channel = MessageChannel.EMAIL,
                actionType = MailActionType.WORKSPACE_INVITE_ACCEPT,
                targetEmail = "user@psycho.pizza",
                token = "failed",
                expiredAt = Instant.now().plusSeconds(3600),
            )
        entity.fail(MailAuthFailureReason.SUPERSEDED)

        every { repository.findByTokenWithParams("failed") } returns entity

        val result = service.verify("failed")

        assertTrue(result is MailAuthTokenResult.Failed)
        verify(exactly = 0) { publisher.publishEvent(any()) }
    }

    @Test
    fun `인증 성공 시 이벤트가 발행된다`() {
        val entity =
            MailAuthToken.issue(
                mailType = MessageType.WORKSPACE_INVITE,
                channel = MessageChannel.EMAIL,
                actionType = MailActionType.WORKSPACE_INVITE_ACCEPT,
                targetEmail = "user@psycho.pizza",
                token = "ok",
                expiredAt = Instant.now().plusSeconds(3600),
            )
        val workspaceId = UUID.fromString("6c8e3e5d-7d2a-4b28-a34b-49a4f1a87f2a")
        val inviterAccountId = UUID.fromString("f21f98f7-4224-4ab4-8105-a0fdb0f8ac2a")
        val inviteeEmail = "invitee@psycho.pizza"
        entity.params.add(MailAuthTokenParam.create(entity, "workspaceId", workspaceId.toString()))
        entity.params.add(MailAuthTokenParam.create(entity, "inviterAccountId", inviterAccountId.toString()))
        entity.params.add(MailAuthTokenParam.create(entity, "inviteeEmail", inviteeEmail))

        every { repository.findByTokenWithParams("ok") } returns entity
        val eventSlot = slot<MailAuthVerifiedEvent>()
        every { publisher.publishEvent(capture(eventSlot)) } returns Unit

        val result = service.verify("ok")

        assertTrue(result is MailAuthTokenResult.Verified)
        assertEquals("ok", eventSlot.captured.token)
        val actionRequest = eventSlot.captured.actionRequest as MailActionRequest.WorkspaceInviteAccept
        assertEquals(workspaceId, actionRequest.params.workspaceId)
        assertEquals(inviterAccountId, actionRequest.params.inviterAccountId)
        assertEquals(inviteeEmail, actionRequest.params.inviteeEmail)
    }
}
