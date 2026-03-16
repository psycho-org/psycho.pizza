package pizza.psycho.sos.project.sprint.domain.event

import pizza.psycho.sos.common.event.DomainEvent
import java.time.Instant
import java.util.UUID

sealed interface SprintDomainEvent : DomainEvent {
    val workspaceId: UUID
    val sprintId: UUID
    val actorId: UUID?
    val eventId: UUID
    override val occurredAt: Instant
}

// 스프린트 목표 변경
data class SprintGoalChangedEvent(
    override val workspaceId: UUID,
    override val sprintId: UUID,
    override val actorId: UUID?,
    val fromGoal: String,
    val toGoal: String,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : SprintDomainEvent

// 스프린트 기간 변경
data class SprintPeriodChangedEvent(
    override val workspaceId: UUID,
    override val sprintId: UUID,
    override val actorId: UUID?,
    val fromPeriod: String,
    val toPeriod: String,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : SprintDomainEvent

// 스프린트에 Task 추가
data class TaskAddedToSprintEvent(
    override val workspaceId: UUID,
    override val sprintId: UUID,
    val taskId: UUID,
    override val actorId: UUID?,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : SprintDomainEvent

// 스프린트에서 Task 제거
data class TaskRemovedFromSprintEvent(
    override val workspaceId: UUID,
    override val sprintId: UUID,
    val taskId: UUID,
    override val actorId: UUID?,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : SprintDomainEvent

data class SprintDeletedEvent(
    override val workspaceId: UUID,
    override val sprintId: UUID,
    override val actorId: UUID?,
    val sprintName: String,
    val reason: String? = null,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : SprintDomainEvent
