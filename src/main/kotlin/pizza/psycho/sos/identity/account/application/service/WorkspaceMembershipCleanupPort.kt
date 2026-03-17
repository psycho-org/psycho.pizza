package pizza.psycho.sos.identity.account.application.service

import java.util.UUID

interface WorkspaceMembershipCleanupPort {
    fun softDeleteActiveMembershipsByAccountId(
        accountId: UUID,
        deletedBy: UUID,
    )
}
