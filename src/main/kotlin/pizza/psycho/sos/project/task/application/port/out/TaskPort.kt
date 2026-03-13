package pizza.psycho.sos.project.task.application.port.out

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import java.time.Instant
import java.util.UUID

interface TaskPort {
    fun createTask(
        workspaceId: UUID,
        title: String,
        description: String,
        assigneeId: UUID?,
        dueDate: Instant?,
    ): TaskSnapshot

    fun findByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskSnapshot>

    fun findByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<TaskSnapshot>

    fun deleteById(
        id: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int

    fun deleteByIdIn(
        ids: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int

    /**
     * 주어진 Task ID 들의 상태를 TO DO로 리셋한다.
     */
    fun resetStatusToTodo(
        ids: Collection<UUID>,
        actorId: UUID,
        workspaceId: WorkspaceId,
        emitEvent: Boolean = false,
    )
}
