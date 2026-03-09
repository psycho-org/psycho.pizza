package pizza.psycho.sos.project.task.application.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.application.service.dto.TaskCommand
import pizza.psycho.sos.project.task.application.service.dto.TaskResult
import pizza.psycho.sos.project.task.application.service.dto.TaskResult.Assignee
import pizza.psycho.sos.project.task.application.service.dto.TaskResult.TaskInformation
import pizza.psycho.sos.project.task.domain.model.entity.Task
import pizza.psycho.sos.project.task.domain.repository.TaskRepository
import java.util.UUID

@Service
class TaskService(
    private val taskRepository: TaskRepository,
) {
    fun create(command: TaskCommand.AddTask): TaskResult =
        Tx.writable {
            saveTask(command).toResult()
        }

    fun getAll(command: TaskCommand.FindTasks): TaskResult.TaskList =
        taskRepository.findAllActiveTasks(WorkspaceId(command.workspaceId), command.pageable).let {
            TaskResult.TaskList(it.toResult())
        }

    fun getInformation(command: TaskCommand.FindTask): TaskResult =
        taskRepository
            .findActiveTaskByIdOrNull(id = command.id, workspaceId = WorkspaceId(command.workspaceId))
            ?.toResult()
            ?: TaskResult.Failure.IdNotFound

    fun remove(command: TaskCommand.RemoveTask): TaskResult =
        Tx.writable {
            with(command) {
                taskRepository
                    .deleteById(
                        id = id,
                        deletedBy = deletedBy,
                        workspaceId = WorkspaceId(workspaceId),
                    ).let { TaskResult.Remove(it) }
            }
        }

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
    ): Int = taskRepository.deleteById(id, deletedBy, workspaceId)

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
}
