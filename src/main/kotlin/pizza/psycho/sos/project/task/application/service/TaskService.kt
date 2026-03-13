package pizza.psycho.sos.project.task.application.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.application.service.dto.TaskCommand
import pizza.psycho.sos.project.task.application.service.dto.TaskQuery
import pizza.psycho.sos.project.task.application.service.dto.TaskResult
import pizza.psycho.sos.project.task.application.service.dto.TaskResult.Assignee
import pizza.psycho.sos.project.task.application.service.dto.TaskResult.TaskInformation
import pizza.psycho.sos.project.task.domain.model.entity.Task
import pizza.psycho.sos.project.task.domain.model.entity.TaskUpdateSpec
import pizza.psycho.sos.project.task.domain.model.vo.Status
import pizza.psycho.sos.project.task.domain.repository.TaskRepository
import java.util.UUID

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val domainEventPublisher: DomainEventPublisher,
) {
    fun create(command: TaskCommand.AddTask): TaskResult =
        Tx.writable {
            saveTask(command).toResult()
        }

    fun getAll(command: TaskQuery.FindTasks): TaskResult.TaskList =
        taskRepository.findAllActiveTasks(WorkspaceId(command.workspaceId), command.pageable).let {
            TaskResult.TaskList(it.toResult())
        }

    fun getInformation(command: TaskQuery.FindTask): TaskResult =
        taskRepository
            .findActiveTaskByIdOrNull(id = command.id, workspaceId = WorkspaceId(command.workspaceId))
            ?.toResult()
            ?: TaskResult.Failure.IdNotFound

    fun remove(command: TaskCommand.RemoveTask): TaskResult =
        this
            .deleteTaskById(
                id = command.id,
                deletedBy = command.deletedBy,
                workspaceId = WorkspaceId(command.workspaceId),
            ).let { TaskResult.Remove(it) }

    fun saveTask(command: TaskCommand.AddTask): Task = taskRepository.save(command.toDomain())

    fun findTasksByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<Task> = taskRepository.findAllByIdIn(ids, workspaceId)

    fun findTasksByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Task> = taskRepository.findAllByIdIn(ids, workspaceId, pageable)

    fun deleteTaskById(
        id: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int =
        Tx.writable {
            taskRepository
                .findActiveTaskByIdOrNull(id, workspaceId)
                ?.also {
                    it.delete(deletedBy)
                    domainEventPublisher.publishAndClear(it)
                }?.let { 1 }
                ?: 0
        }

    fun deleteTasksByIdIn(
        ids: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int =
        Tx.writable {
            if (ids.isEmpty()) return@writable 0
            val tasks =
                taskRepository
                    .findAllByIdIn(ids, workspaceId)
                    .onEach { it.delete(deletedBy) }

            domainEventPublisher.publishAndClearAll(tasks)
            return@writable tasks.size
        }

    /**
     * 주어진 Task 들의 상태를 TO DO로 되돌린다. (예: 스프린트에서 분리될 때)
     */
    fun resetStatusToTodo(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        actorId: UUID,
    ) = Tx.writable {
        if (ids.isEmpty()) return@writable

        val tasks = taskRepository.findAllByIdIn(ids, workspaceId)
        if (tasks.isEmpty()) return@writable

        tasks.forEach { task ->
            if (task.status != Status.TODO) {
                task.changeStatus(
                    status = Status.TODO,
                    by = actorId,
                    emitEvent = false,
                )
            }
        }

        domainEventPublisher.publishAndClearAll(tasks)
    }

    fun update(command: TaskCommand.UpdateTask): TaskResult =
        Tx.writable {
            val workspaceId = WorkspaceId(command.workspaceId)

            val task =
                taskRepository.findActiveTaskByIdOrNull(command.id, workspaceId)
                    ?: return@writable TaskResult.Failure.IdNotFound

            val spec = command.toUpdateSpec()
            task.apply(spec)

            domainEventPublisher.publishAndClear(task)

            task.toResult()
        }

    // -----------------------------------------------------------------------------

    // todo 유저 로직 추가 시 수정
    private fun Page<Task>.toResult(): Page<TaskResult.TaskListInfo> =
        map {
            TaskResult.TaskListInfo(
                id = it.taskId,
                title = it.title,
                assignee =
                    it.assigneeId.value?.let { id ->
                        Assignee(
                            id = id,
                            name = "",
                            email = "",
                        )
                    },
                status = it.status,
                dueDate = it.dueDate.value,
            )
        }

    private fun Task.toResult(): TaskResult =
        TaskInformation(
            id = taskId,
            title = title,
            description = description,
            status = status,
            priority = priority,
            assignee =
                assigneeId.value?.let { id ->
                    Assignee(
                        id = id,
                        name = "",
                        email = "",
                    )
                },
            dueDate = dueDate.value,
            workspaceId = workspaceId.value,
        )

    private fun TaskCommand.AddTask.toDomain(): Task =
        Task.create(
            title = title,
            description = description,
            assigneeId = assigneeId,
            workspaceId = workspaceId,
            dueDate = dueDate,
        )

    private fun TaskCommand.UpdateTask.toUpdateSpec(): TaskUpdateSpec =
        TaskUpdateSpec(
            title = title,
            description = description,
            status = status,
            assigneeId = assigneeId,
            dueDate = dueDate,
            priority = priority,
            actorId = actorId,
        )
}
