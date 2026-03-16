package pizza.psycho.sos.project.project.application.facade

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import java.util.UUID

interface ProjectFacade {
    fun createProject(
        workspaceId: WorkspaceId,
        name: String,
    ): ProjectSnapshot

    fun findProjectsByIdIn(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectSnapshot>

    fun findProgressesByProjectId(
        projectIds: List<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectProgress>

    fun deleteProjectById(
        projectId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int

    fun deleteProjectsByIdIn(
        projectIds: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int

    fun findActiveProjectIdsByTaskIds(
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskAssignment>
}
