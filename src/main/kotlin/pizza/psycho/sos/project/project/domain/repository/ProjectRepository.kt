package pizza.psycho.sos.project.project.domain.repository

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.domain.model.entity.Project
import java.util.UUID

interface ProjectRepository {
    fun findActiveProjectByIdOrNull(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): Project?
}
