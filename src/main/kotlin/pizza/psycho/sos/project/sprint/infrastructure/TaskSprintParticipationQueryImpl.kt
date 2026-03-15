package pizza.psycho.sos.project.sprint.infrastructure

import org.springframework.stereotype.Component
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import pizza.psycho.sos.project.task.application.port.out.TaskSprintParticipationQuery
import java.util.UUID

@Component
class TaskSprintParticipationQueryImpl(
    private val sprintRepository: SprintRepository,
) : TaskSprintParticipationQuery {
    override fun existsActiveSprintByTaskId(
        taskId: UUID,
        workspaceId: UUID,
    ): Boolean =
        sprintRepository.existsActiveSprintByTaskId(
            taskId = taskId,
            workspaceId = WorkspaceId(workspaceId),
        )

    override fun findTaskIdsInActiveSprints(
        taskIds: Collection<UUID>,
        workspaceId: UUID,
    ): Set<UUID> =
        sprintRepository
            .findActiveSprintIdsByTaskIds(taskIds, WorkspaceId(workspaceId))
            .keys
}
