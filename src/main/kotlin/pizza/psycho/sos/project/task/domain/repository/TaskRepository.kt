package pizza.psycho.sos.project.task.domain.repository

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.domain.model.entity.Task
import java.util.UUID

interface TaskRepository {
    fun findByIdOrNull(
        id: UUID,
        workspaceId: WorkspaceId,
    ): Task?

    fun findActiveTaskByIdOrNull(
        id: UUID,
        workspaceId: WorkspaceId,
    ): Task?

    fun save(task: Task): Task
}
