package pizza.psycho.sos.project.project.infrastructure.adapter.out.event

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.project.project.application.port.out.ProjectSprintParticipationQuery
import pizza.psycho.sos.project.project.domain.event.ProjectDomainEvent
import pizza.psycho.sos.project.project.domain.event.TaskAddedToProjectEvent
import pizza.psycho.sos.project.project.domain.event.TaskProjectChangedEvent
import pizza.psycho.sos.project.project.domain.event.TaskRemovedFromProjectEvent
import pizza.psycho.sos.project.project.domain.event.TasksAddedToProjectEvent
import pizza.psycho.sos.project.project.domain.event.TasksRemovedFromProjectEvent
import pizza.psycho.sos.audit.application.listener.event.TaskProjectChangedEvent as AuditTaskProjectChangedEvent

@Component
class ProjectDomainEventPublishingHandler(
    private val eventPublisher: DomainEventPublisher,
    private val sprintMembershipQuery: ProjectSprintParticipationQuery,
) {
    private val log by loggerDelegate()

    private fun isAnyProjectInActiveSprint(event: TaskProjectChangedEvent): Boolean {
        val fromInSprint =
            sprintMembershipQuery.existsActiveSprintByProjectId(
                projectId = event.fromProjectId,
                workspaceId = event.workspaceId,
            )

        if (fromInSprint) return true

        val toInSprint =
            sprintMembershipQuery.existsActiveSprintByProjectId(
                projectId = event.toProjectId,
                workspaceId = event.workspaceId,
            )

        return toInSprint
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProjectDomainEvent) {
        when (event) {
            is TaskProjectChangedEvent -> {
                // 스프린트에 속하지 않는 Project 간의 Task 이동 이벤트는 audit 으로 전달하지 않는다
                if (!isAnyProjectInActiveSprint(event)) {
                    log.debug("Skip TaskProjectChangedEvent for non-sprint projects: $event")
                    return
                }

                eventPublisher
                    .publish(
                        AuditTaskProjectChangedEvent(
                            workspaceId = event.workspaceId,
                            taskId = event.taskId,
                            actorId = event.actorId,
                            fromProjectId = event.fromProjectId,
                            toProjectId = event.toProjectId,
                            eventId = event.eventId,
                            occurredAt = event.occurredAt,
                        ),
                    ).also { log.info("Published TaskProjectChangedEvent to Audit module: $event") }
            }

            // Sprint 관련 처리에서 사용하지 않는 Project 이벤트들
            is TaskAddedToProjectEvent,
            is TaskRemovedFromProjectEvent,
            is TasksAddedToProjectEvent,
            is TasksRemovedFromProjectEvent,
            -> Unit
        }
    }
}
