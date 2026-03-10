package pizza.psycho.sos.project.sprint.presentation

import jakarta.validation.Valid
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
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.application.service.SprintService
import pizza.psycho.sos.project.sprint.application.service.dto.SprintCommand
import pizza.psycho.sos.project.sprint.application.service.dto.SprintResult
import pizza.psycho.sos.project.sprint.presentation.dto.SprintRequest
import pizza.psycho.sos.project.sprint.presentation.dto.SprintResponse
import java.util.UUID

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/sprints")
class SprintController(
    private val sprintService: SprintService,
) {
    @PostMapping
    fun createSprint(
        @PathVariable workspaceId: UUID,
        @Valid @RequestBody request: SprintRequest.Create,
    ): ApiResponse<*> =
        handleResult {
            sprintService.create(request.toCommand(workspaceId))
        }

    @GetMapping("/{sprintId}")
    fun findSprintById(
        @PathVariable workspaceId: UUID,
        @PathVariable sprintId: UUID,
    ): ApiResponse<*> =
        handleResult {
            sprintService.getSprint(SprintCommand.Get(WorkspaceId(workspaceId), sprintId))
        }

    @GetMapping("/{sprintId}/projects")
    fun findProjectsInSprint(
        @PathVariable workspaceId: UUID,
        @PathVariable sprintId: UUID,
    ): ApiResponse<*> =
        handleResult {
            sprintService.getProjectsInSprint(SprintCommand.GetProjects(WorkspaceId(workspaceId), sprintId))
        }

    @PostMapping("/{sprintId}/projects")
    fun createProjectInSprint(
        @PathVariable workspaceId: UUID,
        @PathVariable sprintId: UUID,
        @Valid @RequestBody request: SprintRequest.CreateProject,
    ): ApiResponse<*> =
        handleResult {
            sprintService.createProject(request.toCommand(workspaceId, sprintId))
        }

    @PatchMapping("/{sprintId}")
    fun modifySprint(
        @PathVariable workspaceId: UUID,
        @PathVariable sprintId: UUID,
        @Valid @RequestBody request: SprintRequest.Update,
    ): ApiResponse<*> =
        handleResult {
            sprintService.modify(request.toCommand(workspaceId, sprintId))
        }

    @DeleteMapping("/{sprintId}/{userId}")
    fun removeSprint(
        @PathVariable workspaceId: UUID,
        @PathVariable sprintId: UUID,
        @PathVariable userId: UUID,
    ): ApiResponse<*> =
        handleResult {
            sprintService.remove(SprintCommand.Remove(WorkspaceId(workspaceId), sprintId, userId))
        }

    @DeleteMapping("/{sprintId}/{userId}/with-tasks")
    fun removeSprintWithTasks(
        @PathVariable workspaceId: UUID,
        @PathVariable sprintId: UUID,
        @PathVariable userId: UUID,
    ): ApiResponse<*> =
        handleResult {
            sprintService.removeWithTasks(SprintCommand.RemoveWithTasks(WorkspaceId(workspaceId), sprintId, userId))
        }

    // ------------------------------------------------------------------------------------------------

    private fun handleResult(function: () -> SprintResult): ApiResponse<*> =
        when (val result: SprintResult = function()) {
            is SprintResult.SprintInfo -> responseOf(data = result.toResponse())
            is SprintResult.ProjectList -> responseOf(data = result.projects.map { it.toResponse() })
            is SprintResult.ProjectCreated -> responseOf(data = result.project.toResponse())
            is SprintResult.Remove ->
                responseOf(
                    message = "데이터 삭제에 성공하였습니다.",
                    data = SprintResponse.Remove(result.count),
                )
            is SprintResult.RemoveWithTasks ->
                responseOf(
                    message = "스프린트 및 하위 프로젝트, 태스크 삭제에 성공하였습니다.",
                    data = SprintResponse.RemoveWithTasks(result.sprintCount, result.projectCount, result.taskCount),
                )
            is SprintResult.Success -> responseOf<Unit>(message = "데이터 수정에 성공하였습니다.")
            is SprintResult.Failure.IdNotFound -> throw DomainException("id not found")
            is SprintResult.Failure.ProjectNotFound -> throw DomainException("project not found")
            is SprintResult.Failure.InvalidRequest -> throw DomainException("invalid request")
        }

    private fun SprintRequest.Create.toCommand(workspaceId: UUID) =
        SprintCommand.Create(
            workspaceId = WorkspaceId(workspaceId),
            name = name,
            startDate = startDate,
            endDate = endDate,
        )

    private fun SprintRequest.Update.toCommand(
        workspaceId: UUID,
        sprintId: UUID,
    ) = SprintCommand.Update(
        workspaceId = WorkspaceId(workspaceId),
        sprintId = sprintId,
        name = name,
        startDate = startDate,
        endDate = endDate,
        addProjectIds = addProjectIds,
        removeProjectIds = removeProjectIds,
    )

    private fun SprintRequest.CreateProject.toCommand(
        workspaceId: UUID,
        sprintId: UUID,
    ) = SprintCommand.CreateProject(
        workspaceId = WorkspaceId(workspaceId),
        sprintId = sprintId,
        name = name,
    )

    private fun SprintResult.SprintInfo.toResponse(): SprintResponse.Information =
        SprintResponse.Information(
            workspaceId = workspaceId.value,
            sprintId = sprintId,
            name = name,
            startDate = startDate,
            endDate = endDate,
        )

    private fun SprintResult.Project.toResponse(): SprintResponse.Project =
        SprintResponse.Project(
            projectId = projectId,
            name = name,
            progress =
                SprintResponse.Progress(
                    totalCount = progress.totalCount,
                    completedCount = progress.completedCount,
                    progress = progress.progress,
                ),
        )
}
