package pizza.psycho.sos.project.task.infrastructure.persistence.adapter

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.application.facade.TaskFacade
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import pizza.psycho.sos.project.task.application.port.out.dto.SprintTaskMembershipSnapshot
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import java.time.Instant
import java.util.UUID

@Component
class TaskAdapter(
    private val taskFacade: TaskFacade,
) : TaskPort {
    override fun createTask(
        workspaceId: UUID,
        title: String,
        description: String,
        assigneeId: UUID?,
        dueDate: Instant?,
    ): TaskSnapshot =
        taskFacade.createTask(
            workspaceId = workspaceId,
            title = title,
            description = description,
            assigneeId = assigneeId,
            dueDate = dueDate,
        )

    override fun findByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskSnapshot> = taskFacade.findTasksByIdIn(ids, workspaceId)

    override fun findByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<TaskSnapshot> = taskFacade.findTasksByIdIn(ids, workspaceId, pageable)

    override fun deleteById(
        id: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String?,
    ): Int = taskFacade.deleteTaskById(id, deletedBy, workspaceId, reason)

    override fun deleteByIdIn(
        ids: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String?,
    ): Int = taskFacade.deleteTasksByIdIn(ids, deletedBy, workspaceId, reason)

    override fun moveToBacklog(
        ids: Collection<UUID>,
        actorId: UUID?,
        workspaceId: WorkspaceId,
    ) = taskFacade.moveToBacklog(ids, actorId, workspaceId)

    override fun moveSprintTasksToBacklog(
        ids: Collection<UUID>,
        actorId: UUID?,
        workspaceId: WorkspaceId,
        membershipSnapshot: SprintTaskMembershipSnapshot,
    ) = taskFacade.moveSprintTasksToBacklog(ids, actorId, workspaceId, membershipSnapshot)
}
