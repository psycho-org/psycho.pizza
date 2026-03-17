package pizza.psycho.sos.project.task.presentation.dto

import jakarta.validation.constraints.NotBlank
import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.project.task.domain.model.vo.Priority
import pizza.psycho.sos.project.task.domain.model.vo.Status
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

    /**
     * 부분 수정용 요청
     * - 필드를 생략하면 Patch.Unchanged
     * - "field": null 이면 Patch.Clear (nullable 필드에서 의미 있음)
     * - "field": value 이면 Patch.Value(value)
     */
    data class Update(
        val title: Patch<String> = Patch.Unchanged,
        val description: Patch<String> = Patch.Unchanged,
        val status: Patch<Status> = Patch.Unchanged,
        val assigneeId: Patch<UUID> = Patch.Unchanged,
        val dueDate: Patch<Instant> = Patch.Unchanged,
        val priority: Patch<Priority> = Patch.Unchanged,
    ) : TaskRequest

    data class Delete(
        @field:NotBlank
        val reason: String,
    ) : TaskRequest
}
