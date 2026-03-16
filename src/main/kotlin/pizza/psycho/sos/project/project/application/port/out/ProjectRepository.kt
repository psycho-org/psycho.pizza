package pizza.psycho.sos.project.project.application.port.out

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.project.domain.event.ProjectDeletedEvent
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
        reason: String? = null,
    ): Int

    fun deleteByIdIn(
        projectIds: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String? = null,
    ): List<ProjectDeletedEvent>

    fun save(project: Project): Project

    fun findActiveProjectIdsByTaskIds(
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskAssignment>

    fun findActiveTaskIdsByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): List<UUID>

    fun findActiveTaskIdsByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<UUID>

    fun findActiveTaskIdsByProjectIds(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): Map<UUID, List<UUID>>
}
