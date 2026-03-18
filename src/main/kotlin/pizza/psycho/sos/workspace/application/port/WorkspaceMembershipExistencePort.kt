package pizza.psycho.sos.workspace.application.port

import java.util.UUID

interface WorkspaceMembershipExistencePort {
    fun existsActiveMembership(
        workspaceId: UUID,
        accountId: UUID,
    ): Boolean
}
