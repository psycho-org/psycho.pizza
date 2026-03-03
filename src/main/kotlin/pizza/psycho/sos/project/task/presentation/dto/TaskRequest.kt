package pizza.psycho.sos.project.task.presentation.dto

import java.time.Instant
import java.util.UUID

sealed interface TaskRequest {
    data class Create(
        val title: String,
        val description: String,
        val assigneeId: UUID? = null,
        val dueDate: Instant? = null,
    ) : TaskRequest
}
