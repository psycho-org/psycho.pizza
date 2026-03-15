package pizza.psycho.sos.audit.application.listener.event

import pizza.psycho.sos.common.event.DomainEvent
import java.time.Instant
import java.util.UUID

// TODO: sealed interface로 감싸기
// 스프린트 목표 변경 (MVP-03)
data class SprintGoalChangedEvent(
    val workspaceId: UUID,
    val sprintId: UUID,
    val actorId: UUID?,
    val fromGoal: String,
    val toGoal: String,
    val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

// 스프린트 기간 변경 (MVP-03)
data class SprintPeriodChangedEvent(
    val workspaceId: UUID,
    val sprintId: UUID,
    val actorId: UUID?,
    val fromStartDate: Instant, // 🔥here!
    val fromEndDate: Instant, // 🔥here!
    val toStartDate: Instant, // 🔥here!
    val toEndDate: Instant, // 🔥here!
    val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

// 스프린트에 Task 추가 (MVP-03)
data class TaskAddedToSprintEvent(
    val workspaceId: UUID,
    val sprintId: UUID,
    val taskId: UUID,
    val actorId: UUID?,
    val sprintStartDate: Instant, // 🔥here!
    val sprintEndDate: Instant, // 🔥here!
    val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

// 스프린트에서 Task 제거 (MVP-03)
data class TaskRemovedFromSprintEvent(
    val workspaceId: UUID,
    val sprintId: UUID,
    val taskId: UUID,
    val actorId: UUID?,
    val sprintStartDate: Instant, // 🔥here!
    val sprintEndDate: Instant, // 🔥here!
    val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
