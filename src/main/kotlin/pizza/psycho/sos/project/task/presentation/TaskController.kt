package pizza.psycho.sos.project.task.presentation

import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.common.support.pagination.PageInfoSupport
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.project.task.application.service.TaskService
import pizza.psycho.sos.project.task.application.service.dto.TaskCommand
import pizza.psycho.sos.project.task.application.service.dto.TaskQuery
import pizza.psycho.sos.project.task.application.service.dto.TaskResult
import pizza.psycho.sos.project.task.domain.exception.TaskErrorCode
import pizza.psycho.sos.project.task.presentation.dto.TaskRequest
import pizza.psycho.sos.project.task.presentation.dto.TaskResponse
import java.util.UUID

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
        @PageableDefault(page = 0, size = 10) pageable: Pageable,
    ): ApiResponse<*> =
        handleResult {
            taskService.getAll(TaskQuery.FindTasks(workspaceId, pageable))
        }

    @GetMapping("/assignees")
    fun findTasksByAssignee(
        @PathVariable workspaceId: UUID,
        @PageableDefault(page = 0, size = 10) pageable: Pageable,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<*> =
        handleResult {
            taskService.getAssignedTasks(TaskQuery.FindAssignedTasks(workspaceId, principal.accountId, pageable))
        }

    @GetMapping("/backlog")
    fun findBacklogTasks(
        @PathVariable workspaceId: UUID,
        @PageableDefault(page = 0, size = 10) pageable: Pageable,
    ): ApiResponse<*> =
        handleResult {
            taskService.getBacklog(TaskQuery.FindBacklogTasks(workspaceId, pageable))
        }

    @GetMapping("/{id}")
    fun findTaskById(
        @PathVariable workspaceId: UUID,
        @PathVariable id: UUID,
    ): ApiResponse<*> =
        handleResult {
            taskService.getInformation(TaskQuery.FindTask(workspaceId, id))
        }

    @DeleteMapping("/{id}")
    fun remove(
        @PathVariable workspaceId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: TaskRequest.Delete,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<*> =
        handleResult {
            taskService.remove(TaskCommand.RemoveTask(workspaceId, id, principal.accountId, request.reason))
        }

    @PatchMapping("/{id}")
    fun update(
        @PathVariable workspaceId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: TaskRequest.Update,
        @AuthenticationPrincipal principal: AuthenticatedAccountPrincipal,
    ): ApiResponse<*> =
        handleResult {
            taskService.update(request.toCommand(workspaceId, id, principal.accountId))
        }

    // ------------------------------------------------------------------------------------------------

    private fun handleResult(function: () -> TaskResult): ApiResponse<*> =
        when (val result: TaskResult = function()) {
            is TaskResult.Remove ->
                responseOf(
                    message = "Data deletion was successful.",
                    data = TaskResponse.Remove(result.count),
                )

            is TaskResult.TaskInformation -> responseOf(data = result.toResponse())
            is TaskResult.TaskList -> pageInfoSupport.toPageResponse(result.page.map { it.toResponse() })
            is TaskResult.AssignedTaskList -> pageInfoSupport.toPageResponse(result.page.map { it.toResponse() })
            is TaskResult.Failure.IdNotFound -> throw DomainException(TaskErrorCode.TASK_NOT_FOUND)
            is TaskResult.Failure.TaskInformationNotFound -> throw DomainException(TaskErrorCode.TASK_INFO_NOT_FOUND)
            is TaskResult.Failure.InvalidRequest -> throw DomainException(TaskErrorCode.INVALID_REQUEST)
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
            title = title,
            description = description,
            status = status,
            assigneeId = assigneeId,
            dueDate = dueDate,
            priority = priority,
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
            priority = priority,
            assignee = assignee?.let { TaskResponse.Assignee(it.id, it.name, it.email) },
            workspaceId = workspaceId,
            dueDate = dueDate,
        )

    private fun TaskResult.AssignedTaskListInfo.toResponse(): TaskResponse.AssignedList =
        TaskResponse.AssignedList(
            id = id,
            title = title,
            status = status,
            assignee = assignee?.let { TaskResponse.Assignee(it.id, it.name, it.email) },
            dueDate = dueDate,
            projects =
                projects.map { project ->
                    TaskResponse.Project(
                        id = project.id,
                        name = project.name,
                    )
                },
            sprints =
                sprints.map { sprint ->
                    TaskResponse.Sprint(
                        id = sprint.id,
                        name = sprint.name,
                        startDate = sprint.startDate,
                        endDate = sprint.endDate,
                    )
                },
        )
}
