package pizza.psycho.sos.project.sprint.presentation.dto

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

    data class Remove(
        val count: Int,
    )

    data class RemoveWithTasks(
        val sprintCount: Int,
        val projectCount: Int,
        val taskCount: Int,
    )
}
