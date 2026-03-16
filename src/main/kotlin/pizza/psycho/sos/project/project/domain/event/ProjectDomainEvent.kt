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

// 프로젝트에 Task 추가
data class TaskAddedToProjectEvent(
    override val workspaceId: UUID,
    override val actorId: UUID?,
    val taskId: UUID,
    val projectId: UUID,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : ProjectDomainEvent

// 프로젝트에 여러 Task 추가
data class TasksAddedToProjectEvent(
    override val workspaceId: UUID,
    override val actorId: UUID?,
    val taskIds: List<UUID>,
    val projectId: UUID,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : ProjectDomainEvent

// 프로젝트에서 Task 제거
data class TaskRemovedFromProjectEvent(
    override val workspaceId: UUID,
    override val actorId: UUID?,
    val taskId: UUID,
    val projectId: UUID,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : ProjectDomainEvent

// 프로젝트에서 여러 Task 제거
data class TasksRemovedFromProjectEvent(
    override val workspaceId: UUID,
    override val actorId: UUID?,
    val taskIds: List<UUID>,
    val projectId: UUID,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : ProjectDomainEvent

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

data class ProjectDeletedEvent(
    override val workspaceId: UUID,
    override val actorId: UUID?,
    val projectId: UUID,
    val projectName: String,
    val reason: String? = null,
    override val eventId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : ProjectDomainEvent
