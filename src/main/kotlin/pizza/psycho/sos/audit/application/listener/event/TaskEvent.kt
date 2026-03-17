package pizza.psycho.sos.audit.application.listener.event

import pizza.psycho.sos.common.event.DomainEvent
import java.time.Instant
import java.util.UUID

// TODO: sealed interface로 감싸기
// Task 상태 변경 (MVP-03)
data class TaskStatusChangedEvent(
    val workspaceId: UUID,
    val sprintId: UUID, // 🔥here!
    val actorId: UUID?,
    val taskId: UUID,
    val fromStatus: String,
    val toStatus: String,
    val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

// 담당자 변경
data class TaskAssigneeChangedEvent(
    val workspaceId: UUID,
    val actorId: UUID?,
    val taskId: UUID,
    val fromAssigneeId: String?,
    val toAssigneeId: String?,
    val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

// 마감일 변경
data class TaskDueDateChangedEvent(
    val workspaceId: UUID,
    val actorId: UUID?,
    val taskId: UUID,
    val fromDueDate: Instant?,
    val toDueDate: Instant?,
    val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

// 프로젝트 이동
data class TaskProjectChangedEvent(
    val workspaceId: UUID,
    val taskId: UUID,
    val actorId: UUID?,
    val fromProjectId: UUID,
    val toProjectId: UUID,
    val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

// Task 삭제
data class TaskDeletedEvent(
    val workspaceId: UUID,
    val taskId: UUID,
    val actorId: UUID?,
    val taskTitle: String, // 삭제 시 기록용
    val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
