package pizza.psycho.sos.project.project.application.facade

import org.springframework.stereotype.Service
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectRepository
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.project.domain.event.ProjectDeletedEvent
import pizza.psycho.sos.project.project.domain.model.entity.Project
import java.util.UUID

@Service
class ProjectFacadeImpl(
    private val projectRepository: ProjectRepository,
) : ProjectFacade {
    override fun createProject(
        workspaceId: WorkspaceId,
        name: String,
    ): ProjectSnapshot =
        Tx.writable {
            val project = Project.create(workspaceId = workspaceId, name = name)
            projectRepository.save(project).toSnapshot(emptyList())
        }

    override fun findProjectsByIdIn(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectSnapshot> {
        val projects = projectRepository.findActiveProjectsByIdIn(projectIds, workspaceId)
        val taskIdsByProjectId = projectRepository.findActiveTaskIdsByProjectIds(projectIds, workspaceId)
        return projects.map { project ->
            project.toSnapshot(taskIdsByProjectId[project.projectId].orEmpty())
        }
    }

    override fun findProgressesByProjectId(
        projectIds: List<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectProgress> = projectRepository.findProgressesByProjectId(projectIds, workspaceId)

    override fun deleteProjectById(
        projectId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String?,
    ): Int = projectRepository.deleteById(projectId, deletedBy, workspaceId, reason)

    override fun deleteProjectsByIdIn(
        projectIds: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String?,
    ): List<ProjectDeletedEvent> = projectRepository.deleteByIdIn(projectIds, deletedBy, workspaceId, reason)

    override fun findActiveProjectIdsByTaskIds(
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskAssignment> = projectRepository.findActiveProjectIdsByTaskIds(taskIds, workspaceId)

    private fun Project.toSnapshot(taskIds: List<UUID>): ProjectSnapshot =
        ProjectSnapshot(
            projectId = projectId,
            workspaceId = workspaceId,
            name = name,
            taskIds = taskIds,
        )
}
