package pizza.psycho.sos.project.task.application.facade

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import java.time.Instant
import java.util.UUID

interface TaskFacade {
    fun createTask(
        workspaceId: UUID,
        title: String,
        description: String,
        assigneeId: UUID?,
        dueDate: Instant?,
    ): TaskSnapshot

    fun findTasksByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskSnapshot>

    fun findTasksByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<TaskSnapshot>

    fun deleteTaskById(
        id: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int

    fun deleteTasksByIdIn(
        ids: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int

    fun moveToBacklog(
        ids: Collection<UUID>,
        actorId: UUID,
        workspaceId: WorkspaceId,
    )
}
