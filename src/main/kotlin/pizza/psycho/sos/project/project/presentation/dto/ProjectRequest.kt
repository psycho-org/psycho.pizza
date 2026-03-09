package pizza.psycho.sos.project.project.presentation.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

sealed interface ProjectRequest {
    data class Create(
        @field:NotBlank
        val name: String,
    ) : ProjectRequest

    data class Update(
        val name: String? = null,
        val addTaskIds: List<UUID> = emptyList(),
        val removeTaskIds: List<UUID> = emptyList(),
    ) : ProjectRequest
}
