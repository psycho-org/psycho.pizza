package pizza.psycho.sos.project.project.presentation

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
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.responseOf
import pizza.psycho.sos.common.support.pagination.PageInfoSupport
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.service.ProjectService
import pizza.psycho.sos.project.project.application.service.dto.ProjectCommand
import pizza.psycho.sos.project.project.application.service.dto.ProjectResult
import pizza.psycho.sos.project.project.presentation.dto.ProjectRequest
import pizza.psycho.sos.project.project.presentation.dto.ProjectResponse
import java.util.UUID

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/projects")
class ProjectController(
    private val projectService: ProjectService,
    private val pageInfoSupport: PageInfoSupport,
) {
    @PostMapping
    fun createProject(
        @PathVariable workspaceId: UUID,
        @Valid @RequestBody request: ProjectRequest.Create,
    ): ApiResponse<*> =
        handleResult {
            projectService.create(request.toCommand(workspaceId))
        }

    @GetMapping("/{projectId}")
    fun findProjectById(
        @PathVariable workspaceId: UUID,
        @PathVariable projectId: UUID,
    ): ApiResponse<*> =
        handleResult {
            projectService.getProject(ProjectCommand.Get(WorkspaceId(workspaceId), projectId))
        }

    @GetMapping("/{projectId}/tasks")
    fun findTasksInProject(
        @PathVariable workspaceId: UUID,
        @PathVariable projectId: UUID,
        @PageableDefault(size = 10) pageable: Pageable,
    ): ApiResponse<*> =
        handleResult {
            projectService.getTasksInProject(ProjectCommand.GetTasks(WorkspaceId(workspaceId), projectId, pageable))
        }

    @PatchMapping("/{projectId}")
    fun modifyProject(
        @PathVariable workspaceId: UUID,
        @PathVariable projectId: UUID,
        @Valid @RequestBody request: ProjectRequest.Update,
    ): ApiResponse<*> =
        handleResult {
            projectService.modify(request.toCommand(workspaceId, projectId))
        }

    @DeleteMapping("/{projectId}/{userId}")
    fun removeProject(
        @PathVariable workspaceId: UUID,
        @PathVariable projectId: UUID,
        @PathVariable userId: UUID,
    ): ApiResponse<*> =
        handleResult {
            projectService.remove(ProjectCommand.Remove(WorkspaceId(workspaceId), projectId, userId))
        }

    @DeleteMapping("/{projectId}/{userId}/with-tasks")
    fun removeProjectWithTasks(
        @PathVariable workspaceId: UUID,
        @PathVariable projectId: UUID,
        @PathVariable userId: UUID,
    ): ApiResponse<*> =
        handleResult {
            projectService.removeWithTasks(ProjectCommand.RemoveWithTasks(WorkspaceId(workspaceId), projectId, userId))
        }

    // ------------------------------------------------------------------------------------------------

    private fun handleResult(function: () -> ProjectResult): ApiResponse<*> =
        when (val result: ProjectResult = function()) {
            is ProjectResult.ProjectInfo -> responseOf(data = result.toResponse())
            is ProjectResult.TaskList -> pageInfoSupport.toPageResponse(result.page.map { it.toResponse() })
            is ProjectResult.Remove ->
                responseOf(
                    message = "데이터 삭제에 성공하였습니다.",
                    data = ProjectResponse.Remove(result.count),
                )
            is ProjectResult.RemoveWithTasks ->
                responseOf(
                    message = "프로젝트 및 하위 태스크 삭제에 성공하였습니다.",
                    data = ProjectResponse.RemoveWithTasks(result.projectCount, result.taskCount),
                )

            is ProjectResult.Success -> responseOf(message = "데이터 수정에 성공하였습니다.", data = null)
            is ProjectResult.Failure.IdNotFound -> throw DomainException("id not found")
            is ProjectResult.Failure.TaskNotFound -> throw DomainException("task not found")
            is ProjectResult.Failure.InvalidRequest -> throw DomainException("invalid request")
            is ProjectResult.Progress -> responseOf(data = result.toResponse())
        }

    private fun ProjectRequest.Create.toCommand(workspaceId: UUID) =
        ProjectCommand.Create(
            workspaceId = WorkspaceId(workspaceId),
            name = name,
        )

    private fun ProjectRequest.Update.toCommand(
        workspaceId: UUID,
        projectId: UUID,
    ) = ProjectCommand.Update(
        workspaceId = WorkspaceId(workspaceId),
        projectId = projectId,
        name = name,
        addTaskIds = addTaskIds,
        removeTaskIds = removeTaskIds,
    )

    private fun ProjectResult.ProjectInfo.toResponse(): ProjectResponse.Information =
        ProjectResponse.Information(
            workspaceId = workspaceId.value,
            projectId = projectId,
            name = name,
            progress = progress.toResponse(),
        )

    private fun ProjectResult.Progress.toResponse(): ProjectResponse.Progress =
        ProjectResponse.Progress(
            totalCount = totalCount,
            completedCount = completedCount,
            progress = progress,
        )

    private fun ProjectResult.Task.toResponse(): ProjectResponse.Task =
        ProjectResponse.Task(
            id = id,
            title = title,
            status = status,
            assignee = assignee?.let { ProjectResponse.Assignee(it.id, it.name, it.email) },
            dueDate = dueDate,
        )
}
