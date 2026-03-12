package pizza.psycho.sos.project.task.presentation

import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.common.support.pagination.PageInfoSupport
import pizza.psycho.sos.project.task.application.service.TaskService
import pizza.psycho.sos.project.task.application.service.dto.TaskCommand
import pizza.psycho.sos.project.task.application.service.dto.TaskQuery
import pizza.psycho.sos.project.task.application.service.dto.TaskResult
import pizza.psycho.sos.project.task.domain.exception.TaskErrorCode
import pizza.psycho.sos.project.task.presentation.dto.TaskRequest
import pizza.psycho.sos.project.task.presentation.dto.TaskResponse
import java.util.UUID

// todo: userId AuthenticationPrincipal에서 받아오도록 변경
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/tasks")
class TaskController(
    private val taskService: TaskService,
    private val pageInfoSupport: PageInfoSupport,
) {
    @PostMapping
    fun createTask(
        @PathVariable workspaceId: UUID,
        @Valid @RequestBody request: TaskRequest.Create,
    ): ApiResponse<*> =
        handleResult {
            taskService.create(request.toCommand(workspaceId))
        }

    @GetMapping
    fun findAllTasks(
        @PathVariable workspaceId: UUID,
        @PageableDefault(page = 1, size = 10) pageable: Pageable,
    ): ApiResponse<*> =
        handleResult {
            taskService.getAll(TaskQuery.FindTasks(workspaceId, pageable))
        }

    @GetMapping("/{id}")
    fun findTaskById(
        @PathVariable workspaceId: UUID,
        @PathVariable id: UUID,
    ): ApiResponse<*> =
        handleResult {
            taskService.getInformation(TaskQuery.FindTask(workspaceId, id))
        }

    @DeleteMapping("/{id}/{userId}")
    fun remove(
        @PathVariable workspaceId: UUID,
        @PathVariable id: UUID,
        @PathVariable userId: UUID,
    ): ApiResponse<*> =
        handleResult {
            taskService.remove(TaskCommand.RemoveTask(workspaceId, id, userId))
        }

    @PatchMapping("/{id}/{userId}")
    fun update(
        @PathVariable workspaceId: UUID,
        @PathVariable id: UUID,
        @PathVariable userId: UUID,
        @Valid @RequestBody request: TaskRequest.Update,
    ): ApiResponse<*> =
        handleResult {
            taskService.update(request.toCommand(workspaceId, id, userId))
        }

    // ------------------------------------------------------------------------------------------------

    private fun handleResult(function: () -> TaskResult): ApiResponse<*> =
        when (val result: TaskResult = function()) {
            is TaskResult.Remove -> responseOf(message = "Data deletion was successful.", data = TaskResponse.Remove(result.count))
            is TaskResult.TaskInformation -> responseOf(data = result.toResponse())
            is TaskResult.TaskList -> pageInfoSupport.toPageResponse(result.page.map { it.toResponse() })
            is TaskResult.Failure.IdNotFound -> throw DomainException(TaskErrorCode.TASK_NOT_FOUND)
            is TaskResult.Failure.TaskInformationNotFound -> throw DomainException(TaskErrorCode.TASK_INFO_NOT_FOUND)
        }

    private fun TaskRequest.Create.toCommand(spaceId: UUID) =
        TaskCommand.AddTask(
            workspaceId = spaceId,
            title = title,
            description = description,
            assigneeId = assigneeId,
            dueDate = dueDate,
        )

    private fun TaskRequest.Update.toCommand(
        workspaceId: UUID,
        taskId: UUID,
        actorId: UUID,
    ): TaskCommand.UpdateTask =
        TaskCommand.UpdateTask(
            workspaceId = workspaceId,
            id = taskId,
            title = title?.let { Patch.Value(it) } ?: Patch.Undefined,
            description = description?.let { Patch.Value(it) } ?: Patch.Undefined,
            status = status?.let { Patch.Value(it) } ?: Patch.Undefined,
            assigneeId = assigneeId?.let { Patch.Value(assigneeId) } ?: Patch.Clear,
            dueDate = dueDate?.let { Patch.Value(dueDate) } ?: Patch.Clear,
            priority = priority?.let { Patch.Value(it) } ?: Patch.Clear,
            actorId = actorId,
        )

    private fun TaskResult.TaskListInfo.toResponse(): TaskResponse.List =
        TaskResponse.List(
            id = id,
            title = title,
            status = status,
            assignee = assignee?.let { TaskResponse.Assignee(it.id, it.name, it.email) },
            dueDate = dueDate,
        )

    private fun TaskResult.TaskInformation.toResponse(): TaskResponse.Information =
        TaskResponse.Information(
            id = id,
            title = title,
            description = description,
            status = status,
            assignee = assignee?.let { TaskResponse.Assignee(it.id, it.name, it.email) },
            workspaceId = workspaceId,
            dueDate = dueDate,
        )
}
