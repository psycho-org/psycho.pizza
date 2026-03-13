package pizza.psycho.sos.project.project.presentation.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

sealed interface ProjectRequest {
    data class Create(
        @field:NotBlank
        val name: String,
    ) : ProjectRequest

    data class CreateTask(
        @field:NotBlank
        val title: String,
        @field:NotBlank
        val description: String,
        val assigneeId: UUID? = null,
        val dueDate: Instant? = null,
    ) : ProjectRequest

    data class Update(
        val name: String? = null,
        val addTaskIds: List<UUID> = emptyList(),
        val removeTaskIds: List<UUID> = emptyList(),
    ) : ProjectRequest

    /**
     * 프로젝트 간 Task 이동 요청
     */
    data class MoveTask(
        val toProjectId: UUID,
    ) : ProjectRequest
}
