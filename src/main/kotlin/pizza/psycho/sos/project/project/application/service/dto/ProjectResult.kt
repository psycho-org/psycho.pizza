package pizza.psycho.sos.project.project.application.service.dto

import org.springframework.data.domain.Page
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

sealed interface ProjectResult {
    data class ProjectInfo(
        val workspaceId: WorkspaceId,
        val projectId: UUID,
        val name: String,
        val progress: Progress,
    ) : ProjectResult

    data class ProjectInfoWithTask(
        val workspaceId: WorkspaceId,
        val projectId: UUID,
        val name: String,
        val progress: Progress,
    )

    data class Task(
        val id: UUID,
        val title: String,
        val status: Status,
        val assignee: Assignee? = null,
        val dueDate: Instant? = null,
        val isWithinSprintPeriod: Boolean? = null,
    ) : ProjectResult

    data class TaskList(
        val page: Page<Task>,
    ) : ProjectResult

    data class Progress(
        val totalCount: Int,
        val completedCount: Int,
        val progress: Double,
    ) : ProjectResult

    data class Remove(
        val count: Int,
    ) : ProjectResult

    data class RemoveWithTasks(
        val projectCount: Int,
        val taskCount: Int,
    ) : ProjectResult

    data object Success : ProjectResult

    data class Assignee(
        val id: UUID,
        val name: String,
        val email: String,
    )

    sealed interface Failure : ProjectResult {
        data object IdNotFound : Failure

        data object TaskNotFound : Failure

        data object InvalidRequest : Failure

        data object TaskAlreadyAssigned : Failure
    }
}
