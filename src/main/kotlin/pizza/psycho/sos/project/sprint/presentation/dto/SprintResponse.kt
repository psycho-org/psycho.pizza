package pizza.psycho.sos.project.sprint.presentation.dto

import pizza.psycho.sos.project.task.domain.model.vo.Priority
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

sealed interface SprintResponse {
    data class Information(
        val workspaceId: UUID,
        val sprintId: UUID,
        val name: String,
        val goal: String? = null,
        val startDate: Instant,
        val endDate: Instant,
    ) : SprintResponse

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

    data class Remove(
        val sprintCount: Int,
        val projectCount: Int,
        val taskCount: Int,
    )
}
