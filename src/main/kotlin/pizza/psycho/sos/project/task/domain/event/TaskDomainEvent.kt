package pizza.psycho.sos.project.task.domain.event

import pizza.psycho.sos.common.event.DomainEvent
import java.time.Instant
import java.util.UUID

// Task 관련 도메인 이벤트의 상위 타입
sealed interface TaskDomainEvent : DomainEvent {
    val workspaceId: UUID
    val taskId: UUID
    val actorId: UUID?
    val eventId: UUID
    override val occurredAt: Instant
}

// Task 상태 변경
data class TaskStatusChangedEvent(
    override val workspaceId: UUID,
    override val actorId: UUID?,
    override val taskId: UUID,
    val fromStatus: String,
    val toStatus: String,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : TaskDomainEvent

// 담당자 변경
data class TaskAssigneeChangedEvent(
    override val workspaceId: UUID,
    override val actorId: UUID?,
    override val taskId: UUID,
    val fromAssigneeId: String?,
    val toAssigneeId: String?,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : TaskDomainEvent

// 마감일 변경
data class TaskDueDateChangedEvent(
    override val workspaceId: UUID,
    override val actorId: UUID?,
    override val taskId: UUID,
    val fromDueDate: Instant?,
    val toDueDate: Instant?,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : TaskDomainEvent

// Task 삭제
data class TaskDeletedEvent(
    override val workspaceId: UUID,
    override val actorId: UUID?,
    override val taskId: UUID,
    val taskTitle: String, // 삭제 시 기록용
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : TaskDomainEvent
