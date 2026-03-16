package pizza.psycho.sos.project.sprint.application.service.dto

import org.springframework.data.domain.Page
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.domain.model.vo.Priority
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

sealed interface SprintResult {
    data class SprintInfo(
        val workspaceId: WorkspaceId,
        val sprintId: UUID,
        val name: String,
        val goal: String? = null,
        val startDate: Instant,
        val endDate: Instant,
    ) : SprintResult

    data class Progress(
        val totalCount: Int,
        val completedCount: Int,
        val progress: Double,
    )

    data class Project(
        val projectId: UUID,
        val name: String,
        val progress: Progress,
    )

    data class ProjectList(
        val projects: List<Project>,
    ) : SprintResult

    data class Task(
        val id: UUID,
        val title: String,
        val status: Status,
        val priority: Priority? = null,
        val projectId: UUID,
        val projectName: String,
        val assigneeId: UUID? = null,
        val dueDate: Instant? = null,
    )

    data class TaskList(
        val tasks: List<Task>,
    ) : SprintResult

    data class SprintPage(
        val page: Page<SprintInfo>,
    ) : SprintResult

    data class ProjectCreated(
        val project: Project,
    ) : SprintResult

    data class Remove(
        val sprintCount: Int,
        val projectCount: Int,
        val taskCount: Int,
    ) : SprintResult

    data object Success : SprintResult

    sealed interface Failure : SprintResult {
        data object IdNotFound : Failure

        data object ProjectNotFound : Failure

        data object InvalidRequest : Failure
    }
}
