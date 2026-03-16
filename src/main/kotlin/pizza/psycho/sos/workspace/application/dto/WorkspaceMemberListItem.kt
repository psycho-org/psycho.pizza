package pizza.psycho.sos.workspace.application.dto

import pizza.psycho.sos.workspace.domain.model.membership.Role
import java.time.Instant
import java.util.UUID

data class WorkspaceMemberListItem(
    val membershipId: UUID,
    val accountId: UUID,
    val name: String,
    val role: Role,
    val joinedAt: Instant?,
)
