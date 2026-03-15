package pizza.psycho.sos.workspace.domain.repository

import pizza.psycho.sos.workspace.domain.model.membership.Membership
import java.util.UUID

interface WorkspaceMembershipCommandRepository {
    fun findActiveNonOwnerMembershipsByAccountId(accountId: UUID): List<Membership>
}
