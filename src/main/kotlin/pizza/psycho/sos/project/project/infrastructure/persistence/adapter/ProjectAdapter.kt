package pizza.psycho.sos.project.project.infrastructure.persistence.adapter

import org.springframework.stereotype.Component
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.facade.ProjectFacade
import pizza.psycho.sos.project.project.application.port.out.ProjectPort
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.project.domain.event.ProjectDeletedEvent
import java.util.UUID

@Component
class ProjectAdapter(
    private val projectFacade: ProjectFacade,
) : ProjectPort {
    override fun createProject(
        workspaceId: WorkspaceId,
        name: String,
    ): ProjectSnapshot = projectFacade.createProject(workspaceId, name)

    override fun findByIdIn(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectSnapshot> = projectFacade.findProjectsByIdIn(projectIds, workspaceId)

    override fun findProgressesByProjectId(
        projectIds: List<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectProgress> = projectFacade.findProgressesByProjectId(projectIds, workspaceId)

    override fun deleteById(
        projectId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String?,
    ): Int = projectFacade.deleteProjectById(projectId, deletedBy, workspaceId, reason)

    override fun deleteByIdIn(
        projectIds: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String?,
    ): List<ProjectDeletedEvent> = projectFacade.deleteProjectsByIdIn(projectIds, deletedBy, workspaceId, reason)

    override fun findActiveProjectIdsByTaskIds(
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskAssignment> = projectFacade.findActiveProjectIdsByTaskIds(taskIds, workspaceId)
}
