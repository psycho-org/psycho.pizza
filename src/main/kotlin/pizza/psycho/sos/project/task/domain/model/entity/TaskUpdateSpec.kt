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
    val title: Patch<String> = Patch.Unchanged,
    val description: Patch<String> = Patch.Unchanged,
    val status: Patch<Status> = Patch.Unchanged,
    val assigneeId: Patch<UUID> = Patch.Unchanged,
    val dueDate: Patch<Instant> = Patch.Unchanged,
    val priority: Patch<Priority> = Patch.Unchanged,
    val actorId: UUID?,
)
