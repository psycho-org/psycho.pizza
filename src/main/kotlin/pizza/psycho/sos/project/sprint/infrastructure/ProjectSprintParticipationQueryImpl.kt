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
}
