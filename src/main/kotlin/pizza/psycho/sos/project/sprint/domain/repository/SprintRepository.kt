package pizza.psycho.sos.project.sprint.domain.repository

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import java.util.UUID

interface SprintRepository {
    fun findActiveSprintByIdOrNull(
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): Sprint?

    fun deleteById(
        sprintId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int

    fun save(sprint: Sprint): Sprint
}
