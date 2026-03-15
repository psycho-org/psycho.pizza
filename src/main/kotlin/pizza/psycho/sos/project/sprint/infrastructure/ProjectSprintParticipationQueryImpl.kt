package pizza.psycho.sos.project.sprint.infrastructure

import org.springframework.stereotype.Component
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectSprintParticipationQuery
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import java.util.UUID

@Component
class ProjectSprintParticipationQueryImpl(
    private val sprintRepository: SprintRepository,
) : ProjectSprintParticipationQuery {
    override fun existsActiveSprintByProjectId(
        projectId: UUID,
        workspaceId: UUID,
    ): Boolean =
        sprintRepository.existsActiveSprintByProjectId(
            projectId = projectId,
            workspaceId = WorkspaceId(workspaceId),
        )

    override fun findActiveSprintIdsByProjectId(
        projectId: UUID,
        workspaceId: UUID,
    ): List<UUID> =
        sprintRepository.findActiveSprintIdsByProjectId(
            projectId = projectId,
            workspaceId = WorkspaceId(workspaceId),
        )

    override fun findActiveSprintIdsByProjectIds(
        projectIds: Collection<UUID>,
        workspaceId: UUID,
    ): Map<UUID, Set<UUID>> =
        sprintRepository.findActiveSprintIdsByProjectIds(
            projectIds = projectIds,
            workspaceId = WorkspaceId(workspaceId),
        )
}
