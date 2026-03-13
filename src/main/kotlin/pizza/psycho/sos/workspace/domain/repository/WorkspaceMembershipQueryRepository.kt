package pizza.psycho.sos.workspace.domain.repository

import pizza.psycho.sos.workspace.application.dto.ActiveWorkspaceMembership
import pizza.psycho.sos.workspace.domain.model.membership.Role
import java.util.UUID

interface WorkspaceMembershipQueryRepository {
    fun findRoleByWorkspaceIdAndAccountId(
        workspaceId: UUID,
        accountId: UUID,
    ): Role?

    fun findActiveWorkspaceMembershipsByAccountId(accountId: UUID): List<ActiveWorkspaceMembership>
}
