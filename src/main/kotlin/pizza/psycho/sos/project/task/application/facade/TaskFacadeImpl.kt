package pizza.psycho.sos.project.task.application.facade

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.application.port.out.dto.SprintTaskMembershipSnapshot
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import pizza.psycho.sos.project.task.application.service.TaskService
import pizza.psycho.sos.project.task.application.service.dto.TaskCommand
import pizza.psycho.sos.project.task.domain.model.entity.Task
import java.time.Instant
import java.util.UUID

@Service
class TaskFacadeImpl(
    private val taskService: TaskService,
) : TaskFacade {
    override fun createTask(
        workspaceId: UUID,
        title: String,
        description: String,
        assigneeId: UUID?,
        dueDate: Instant?,
    ): TaskSnapshot =
        taskService
            .saveTask(
                TaskCommand.AddTask(
                    workspaceId = workspaceId,
                    title = title,
                    description = description,
                    assigneeId = assigneeId,
                    dueDate = dueDate,
                ),
            ).toSnapshot()

    override fun findTasksByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskSnapshot> = taskService.findTasksByIdIn(ids, workspaceId).map { it.toSnapshot() }

    override fun findTasksByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<TaskSnapshot> = taskService.findTasksByIdIn(ids, workspaceId, pageable).map { it.toSnapshot() }

    override fun deleteTaskById(
        id: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String?,
    ): Int = taskService.deleteTaskById(id, deletedBy, workspaceId, reason)

    override fun deleteTasksByIdIn(
        ids: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String?,
    ): Int = taskService.deleteTasksByIdIn(ids, deletedBy, workspaceId, reason)

    override fun moveToBacklog(
        ids: Collection<UUID>,
        actorId: UUID?,
        workspaceId: WorkspaceId,
    ) = taskService.moveToBacklog(ids, workspaceId, actorId)

    override fun moveSprintTasksToBacklog(
        ids: Collection<UUID>,
        actorId: UUID?,
        workspaceId: WorkspaceId,
        membershipSnapshot: SprintTaskMembershipSnapshot,
    ) = taskService.moveSprintTasksToBacklog(ids, workspaceId, actorId, membershipSnapshot)

    private fun Task.toSnapshot(): TaskSnapshot =
        TaskSnapshot(
            id = taskId,
            title = title,
            status = status,
            priority = priority,
            assigneeId = assigneeId.value,
            dueDate = dueDate.value,
        )
}
