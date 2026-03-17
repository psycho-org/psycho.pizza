package pizza.psycho.sos.workspace.domain.repository

import pizza.psycho.sos.workspace.domain.model.workspace.Workspace

interface WorkspaceCommandRepository {
    fun save(workspace: Workspace): Workspace
}
