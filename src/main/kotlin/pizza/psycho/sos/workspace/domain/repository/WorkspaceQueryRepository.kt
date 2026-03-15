package pizza.psycho.sos.workspace.domain.repository

import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import java.util.UUID

interface WorkspaceQueryRepository {
    fun findByIdOrNull(id: UUID): Workspace?

    fun findActiveByIdOrNull(id: UUID): Workspace?
}
