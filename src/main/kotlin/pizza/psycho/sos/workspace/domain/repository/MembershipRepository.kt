package pizza.psycho.sos.workspace.domain.repository

import pizza.psycho.sos.workspace.domain.model.membership.Role
import java.util.UUID

interface MembershipRepository {
    fun findRoleByWorkspaceIdAndAccountId(
        workspaceId: UUID,
        accountId: UUID,
    ): Role?

    fun existsActiveOwnerMembershipByAccountId(accountId: UUID): Boolean
}
