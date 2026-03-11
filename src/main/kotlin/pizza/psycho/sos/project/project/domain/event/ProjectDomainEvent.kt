package pizza.psycho.sos.project.project.domain.event

import pizza.psycho.sos.common.event.DomainEvent
import java.time.Instant
import java.util.UUID

// Project 관련 도메인 이벤트의 상위 타입
sealed interface ProjectDomainEvent : DomainEvent {
    val workspaceId: UUID
    val actorId: UUID?
    val eventId: UUID
    override val occurredAt: Instant
}

// 프로젝트 간 Task 이동
data class TaskProjectChangedEvent(
    override val workspaceId: UUID,
    override val actorId: UUID?,
    val taskId: UUID,
    val fromProjectId: UUID,
    val toProjectId: UUID,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : ProjectDomainEvent
