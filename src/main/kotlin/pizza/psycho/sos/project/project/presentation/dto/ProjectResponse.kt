package pizza.psycho.sos.project.project.presentation.dto

import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

sealed interface ProjectResponse {
    data class Information(
        val workspaceId: UUID,
        val projectId: UUID,
        val name: String,
        val progress: Progress,
    ) : ProjectResponse

    data class Progress(
        val totalCount: Int,
        val completedCount: Int,
        val progress: Double,
    )

    data class Task(
        val id: UUID,
        val title: String,
        val status: Status,
        val assignee: Assignee? = null,
        val dueDate: Instant? = null,
    ) : ProjectResponse

    data class Assignee(
        val id: UUID,
        val name: String,
        val email: String,
    )

    data class Remove(
        val count: Int,
    )

    data class RemoveWithTasks(
        val projectCount: Int,
        val taskCount: Int,
    )
}
