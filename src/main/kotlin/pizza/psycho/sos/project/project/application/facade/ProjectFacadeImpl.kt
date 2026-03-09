package pizza.psycho.sos.project.project.application.facade

import org.springframework.stereotype.Service
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectRepository
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.project.domain.model.entity.Project
import java.util.UUID

@Service
class ProjectFacadeImpl(
    private val projectRepository: ProjectRepository,
) : ProjectFacade {
    override fun findProjectsByIdIn(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectSnapshot> = projectRepository.findActiveProjectsByIdIn(projectIds, workspaceId).map { it.toSnapshot() }

    override fun findProgressesByProjectId(
        projectIds: List<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectProgress> = projectRepository.findProgressesByProjectId(projectIds, workspaceId)

    override fun deleteProjectById(
        projectId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int = projectRepository.deleteById(projectId, deletedBy, workspaceId)

    override fun deleteProjectsByIdIn(
        projectIds: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int = projectRepository.deleteByIdIn(projectIds, deletedBy, workspaceId)

    private fun Project.toSnapshot(): ProjectSnapshot =
        ProjectSnapshot(
            projectId = projectId,
            workspaceId = workspaceId,
            name = name,
            taskIds = taskIds(),
        )
}
