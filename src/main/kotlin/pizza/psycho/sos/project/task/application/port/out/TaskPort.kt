package pizza.psycho.sos.project.task.application.port.out

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.application.port.out.dto.SprintTaskMembershipSnapshot
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
        reason: String? = null,
    ): Int

    fun deleteByIdIn(
        ids: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String? = null,
    ): Int

    /**
     * 주어진 Task 들을 backlog 상태로 되돌린다.
     * backlog 전환은 스프린트 소속 해제와 함께 TO DO 상태 보정까지 포함한다.
     */
    fun moveToBacklog(
        ids: Collection<UUID>,
        actorId: UUID?,
        workspaceId: WorkspaceId,
    )

    fun moveSprintTasksToBacklog(
        ids: Collection<UUID>,
        actorId: UUID?,
        workspaceId: WorkspaceId,
        membershipSnapshot: SprintTaskMembershipSnapshot,
    )
}
