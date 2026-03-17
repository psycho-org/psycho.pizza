package pizza.psycho.sos.project.project.application.port.out

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.project.domain.event.ProjectDeletedEvent
import java.util.UUID

interface ProjectPort {
    fun createProject(
        workspaceId: WorkspaceId,
        name: String,
    ): ProjectSnapshot

    fun findByIdIn(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectSnapshot>

    fun findProgressesByProjectId(
        projectIds: List<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectProgress>

    fun deleteById(
        projectId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String? = null,
    ): Int

    fun deleteByIdIn(
        projectIds: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String? = null,
    ): List<ProjectDeletedEvent>

    fun findActiveProjectIdsByTaskIds(
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskAssignment>
}
