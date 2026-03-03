package pizza.psycho.sos.project.task.presentation

import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.common.support.pagination.PageInfoSupport
import pizza.psycho.sos.project.task.application.service.TaskService
import pizza.psycho.sos.project.task.application.service.dto.TaskCommand
import pizza.psycho.sos.project.task.application.service.dto.TaskResult
import pizza.psycho.sos.project.task.presentation.dto.TaskRequest
import pizza.psycho.sos.project.task.presentation.dto.TaskResponse
import java.util.UUID

@RestController
@RequestMapping("/api/v1/{workspaceId}/tasks")
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
        @PageableDefault(size = 10) pageable: Pageable,
    ): ApiResponse<*> =
        handleResult {
            taskService.getAll(TaskCommand.FindTasks(workspaceId, pageable))
        }

    @GetMapping("/{id}")
    fun findTaskById(
        @PathVariable workspaceId: UUID,
        @PathVariable id: UUID,
    ): ApiResponse<*> =
        handleResult {
            taskService.getInformation(TaskCommand.FindTask(workspaceId, id))
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

    // ------------------------------------------------------------------------------------------------

    private fun handleResult(function: () -> TaskResult): ApiResponse<*> =
        when (val result: TaskResult = function()) {
            is TaskResult.Success -> responseOf(message = "데이터 삭제에 성공하였습니다.", data = null)
            is TaskResult.TaskInformation -> responseOf(data = result.toResponse())
            is TaskResult.TaskList -> pageInfoSupport.toPageResponse(result.page.map { it.toResponse() })
            is TaskResult.Failure.IdNotFound -> throw DomainException("id not found")
            is TaskResult.Failure.TaskInformationNotFound -> throw DomainException("task information not found")
        }

    private fun TaskRequest.Create.toCommand(spaceId: UUID) =
        TaskCommand.AddTask(
            workspaceId = spaceId,
            title = title,
            description = description,
            assigneeId = assigneeId,
            dueDate = dueDate,
        )

    private fun TaskResult.TaskListInfo.toResponse(): TaskResponse.List =
        TaskResponse.List(
            id = id,
            title = title,
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
