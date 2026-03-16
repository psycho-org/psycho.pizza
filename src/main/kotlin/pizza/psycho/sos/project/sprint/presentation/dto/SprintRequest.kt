package pizza.psycho.sos.project.sprint.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import pizza.psycho.sos.common.patch.Patch
import java.time.Instant
import java.util.UUID

sealed interface SprintRequest {
    data class Create(
        @field:NotBlank
        val name: String,
        val goal: String? = null,
        @field:NotNull
        val startDate: Instant,
        @field:NotNull
        val endDate: Instant,
    ) : SprintRequest

    data class Update(
        val name: String? = null,
        val startDate: Instant? = null,
        val endDate: Instant? = null,
        val addProjectIds: List<UUID> = emptyList(),
        val removeProjectIds: List<UUID> = emptyList(),
        val goal: Patch<String> = Patch.Unchanged,
    ) : SprintRequest

    data class CreateProject(
        @field:NotBlank
        val name: String,
    ) : SprintRequest

    data class Delete(
        @field:NotBlank
        val reason: String,
    ) : SprintRequest
}
