package pizza.psycho.sos.project.task.presentation.dto

import pizza.psycho.sos.project.common.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

sealed interface TaskResponse {
    data class Information(
        val id: UUID,
        val title: String,
        val description: String,
        val status: Status,
        val assignee: Assignee? = null,
        val workspaceId: UUID,
        val dueDate: Instant?,
    ) : TaskResponse

    data class List(
        val id: UUID,
        val title: String,
        val assignee: Assignee? = null,
        val dueDate: Instant? = null,
    ) : TaskResponse

    data class Assignee(
        val id: UUID,
        val name: String,
        val email: String,
    )

    data class Remove(
        val count: Int,
    )
}
