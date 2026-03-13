package pizza.psycho.sos.workspace.application.dto

import pizza.psycho.sos.workspace.domain.model.membership.Role
import java.util.UUID

data class ActiveWorkspaceMembership(
    val workspaceId: UUID,
    val workspaceTitle: String,
    val role: Role,
)
