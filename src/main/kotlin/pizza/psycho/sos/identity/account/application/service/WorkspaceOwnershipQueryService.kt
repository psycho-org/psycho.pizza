package pizza.psycho.sos.identity.account.application.service

import java.util.UUID

interface WorkspaceOwnershipQueryService {
    fun existsActiveOwnerMembershipByAccountId(accountId: UUID): Boolean
}
