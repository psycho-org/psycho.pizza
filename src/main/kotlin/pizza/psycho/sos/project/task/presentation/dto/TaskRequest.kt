package pizza.psycho.sos.project.task.presentation.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

sealed interface TaskRequest {
    data class Create(
        @field:NotBlank
        val title: String,
        @field:NotBlank
        val description: String,
        val assigneeId: UUID? = null,
        val dueDate: Instant? = null,
    ) : TaskRequest
}
