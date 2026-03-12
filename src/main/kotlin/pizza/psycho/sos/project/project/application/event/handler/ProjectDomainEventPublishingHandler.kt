package pizza.psycho.sos.project.project.application.event.handler

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.project.project.domain.event.ProjectDomainEvent
import pizza.psycho.sos.project.project.domain.event.TaskProjectChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskProjectChangedEvent as AuditTaskProjectChangedEvent

@Component
class ProjectDomainEventPublishingHandler(
    private val eventPublisher: DomainEventPublisher,
) {
    private val log by loggerDelegate()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ProjectDomainEvent) =
        when (event) {
            is TaskProjectChangedEvent ->
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
}
