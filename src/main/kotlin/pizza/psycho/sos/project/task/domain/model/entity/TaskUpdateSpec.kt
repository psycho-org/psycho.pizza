package pizza.psycho.sos.project.task.domain.model.entity

import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.project.task.domain.model.vo.Priority
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

/**
 * Task 업데이트 요구사항을 표현하는 도메인 전용 스펙
 */
data class TaskUpdateSpec(
    val title: Patch<String> = Patch.Undefined,
    val description: Patch<String> = Patch.Undefined,
    val status: Patch<Status> = Patch.Undefined,
    val assigneeId: Patch<UUID> = Patch.Undefined,
    val dueDate: Patch<Instant> = Patch.Undefined,
    val priority: Patch<Priority> = Patch.Undefined,
    val actorId: UUID?,
)
