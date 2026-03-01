package pizza.psycho.sos.project.task.domain.repository

import pizza.psycho.sos.project.task.domain.model.entity.Task
import java.util.UUID

interface TaskRepository {
    fun findByIdOrNull(
        id: UUID,
        workspaceId: UUID,
    ): Task?

    fun findActiveTaskByIdOrNull(
        id: UUID,
        workspaceId: UUID,
    ): Task?

    fun save(task: Task): Task
}
