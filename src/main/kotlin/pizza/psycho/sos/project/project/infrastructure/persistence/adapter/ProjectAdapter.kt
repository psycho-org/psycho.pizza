package pizza.psycho.sos.project.project.infrastructure.persistence.adapter

import org.springframework.stereotype.Component
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.facade.ProjectFacade
import pizza.psycho.sos.project.project.application.port.out.ProjectPort
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import java.util.UUID

@Component
class ProjectAdapter(
    private val projectFacade: ProjectFacade,
) : ProjectPort {
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
    ): Int = projectFacade.deleteProjectById(projectId, deletedBy, workspaceId)

    override fun deleteByIdIn(
        projectIds: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int = projectFacade.deleteProjectsByIdIn(projectIds, deletedBy, workspaceId)
}
