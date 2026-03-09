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

    fun findProgressesByProjectId(
        projectIds: List<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectProgress>

    fun findActiveProjectsByIdIn(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<Project>

    fun deleteById(
        projectId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int

    fun save(project: Project): Project
}
