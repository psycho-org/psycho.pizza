package pizza.psycho.sos.project.project.application.port.out

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.project.domain.model.entity.Project
import java.util.UUID

interface ProjectRepository {
    fun findActiveProjectByIdOrNull(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): Project?

    fun findProgressByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): ProjectProgress?

    fun save(project: Project): Project
}
