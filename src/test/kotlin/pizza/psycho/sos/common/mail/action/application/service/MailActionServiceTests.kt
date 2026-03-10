package pizza.psycho.sos.common.message.action.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import pizza.psycho.sos.common.message.action.application.model.MailActionRequest
import pizza.psycho.sos.common.message.action.application.model.WorkspaceInviteActionParams
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.workspace.application.service.WorkspaceService
import pizza.psycho.sos.workspace.domain.model.membership.Role
import java.util.UUID

class MailActionServiceTests {
    private val workspaceService = mockk<WorkspaceService>(relaxed = true)
    private val accountService = mockk<AccountService>()
    private val service = MailActionService(workspaceService, accountService)

    @Test
    fun `미가입 이메일이면 멤버 추가를 건너뛴다`() {
        val workspaceId = UUID.fromString("6c8e3e5d-7d2a-4b28-a34b-49a4f1a87f2a")
        val inviterAccountId = UUID.fromString("f21f98f7-4224-4ab4-8105-a0fdb0f8ac2a")
        val request =
            MailActionRequest.WorkspaceInviteAccept(
                params =
                    WorkspaceInviteActionParams(
                        workspaceId = workspaceId,
                        inviterAccountId = inviterAccountId,
                        inviteeEmail = "new-user@psycho.pizza",
                    ),
            )
        every { accountService.findActiveAccountIdByEmailOrNull("new-user@psycho.pizza") } returns null

        service.handle(request)

        verify(exactly = 0) {
            workspaceService.addMember(any(), any(), any(), any())
        }
    }

    @Test
    fun `가입된 이메일이면 멤버를 추가한다`() {
        val workspaceId = UUID.fromString("6c8e3e5d-7d2a-4b28-a34b-49a4f1a87f2a")
        val inviterAccountId = UUID.fromString("f21f98f7-4224-4ab4-8105-a0fdb0f8ac2a")
        val inviteeAccountId = UUID.fromString("2b9a4f26-7a14-4e10-98c0-4371a048a7d1")
        val request =
            MailActionRequest.WorkspaceInviteAccept(
                params =
                    WorkspaceInviteActionParams(
                        workspaceId = workspaceId,
                        inviterAccountId = inviterAccountId,
                        inviteeEmail = "user4@psycho.pizza",
                    ),
            )
        every { accountService.findActiveAccountIdByEmailOrNull("user4@psycho.pizza") } returns inviteeAccountId

        service.handle(request)

        verify(exactly = 1) {
            workspaceService.addMember(
                workspaceId = workspaceId,
                requesterAccountId = inviterAccountId,
                accountId = inviteeAccountId,
                role = Role.CREW,
            )
        }
    }
}
