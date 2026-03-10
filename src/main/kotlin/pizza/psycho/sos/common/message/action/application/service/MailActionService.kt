package pizza.psycho.sos.common.message.action.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.common.message.action.application.model.MailActionRequest
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.workspace.application.service.WorkspaceService
import pizza.psycho.sos.workspace.domain.model.membership.Role

@Service
class MailActionService(
    private val workspaceService: WorkspaceService,
    private val accountService: AccountService,
) {
    @Transactional
    fun handle(request: MailActionRequest) {
        logger.info("Handling mail action. actionType={}", request.actionType)
        when (request) {
            is MailActionRequest.WorkspaceInviteAccept -> handleWorkspaceInviteAction(request)
        }
    }

    private fun handleWorkspaceInviteAction(request: MailActionRequest.WorkspaceInviteAccept) {
        val inviteeAccountId =
            accountService.findActiveAccountIdByEmailOrNull(request.params.inviteeEmail)
        if (inviteeAccountId == null) {
            logger.info(
                "Invitee account not found yet. skip membership add. workspaceId={} inviteeEmail={}",
                request.params.workspaceId,
                request.params.inviteeEmail,
            )
            return
        }
        logger.info(
            "Workspace invite action. workspaceId={} inviterAccountId={} inviteeEmail={} inviteeAccountId={}",
            request.params.workspaceId,
            request.params.inviterAccountId,
            request.params.inviteeEmail,
            inviteeAccountId,
        )

        workspaceService.addMember(
            workspaceId = request.params.workspaceId,
            requesterAccountId = request.params.inviterAccountId,
            accountId = inviteeAccountId,
            role = Role.CREW,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MailActionService::class.java)
    }
}
