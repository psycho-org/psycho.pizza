package pizza.psycho.sos.project.task.domain.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    fun findAll(
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Task>

    fun findAllActiveTasks(
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Task>

    fun findAllByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<Task>

    fun findAllByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Task>

    fun save(task: Task): Task
}
