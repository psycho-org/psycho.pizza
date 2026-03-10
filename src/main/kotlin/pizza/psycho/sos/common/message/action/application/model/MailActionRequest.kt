package pizza.psycho.sos.common.message.action.application.model

import pizza.psycho.sos.common.message.action.domain.MailActionType

sealed interface MailActionRequest {
    val actionType: MailActionType

    data class WorkspaceInviteAccept(
        val params: WorkspaceInviteActionParams,
    ) : MailActionRequest {
        override val actionType: MailActionType = MailActionType.WORKSPACE_INVITE_ACCEPT
    }
}
