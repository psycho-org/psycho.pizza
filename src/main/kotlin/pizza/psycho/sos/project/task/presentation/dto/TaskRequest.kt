package pizza.psycho.sos.project.task.presentation.dto

import jakarta.validation.constraints.NotBlank
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
     * - 필드가 null이면: assigneeId/dueDate/priority 에 대해선 clear로 해석, 나머지는 변경 없음으로 해석
     */
    data class Update(
        val title: String? = null,
        val description: String? = null,
        val status: Status? = null,
        val assigneeId: UUID? = null,
        val dueDate: Instant? = null,
        val priority: Priority? = null,
    ) : TaskRequest
}
