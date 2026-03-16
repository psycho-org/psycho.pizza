package pizza.psycho.sos.project.sprint.infrastructure.adapter.out.event

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.project.sprint.domain.event.SprintDomainEvent
import pizza.psycho.sos.project.sprint.domain.event.SprintGoalChangedEvent
import pizza.psycho.sos.project.sprint.domain.event.SprintPeriodChangedEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskAddedToSprintEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.audit.application.listener.event.SprintGoalChangedEvent as AuditSprintGoalChangedEvent
import pizza.psycho.sos.audit.application.listener.event.SprintPeriodChangedEvent as AuditSprintPeriodChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskAddedToSprintEvent as AuditTaskAddedToSprintEvent
import pizza.psycho.sos.audit.application.listener.event.TaskRemovedFromSprintEvent as AuditTaskRemovedFromSprintEvent

@Component
class SprintDomainEventPublishingHandler(
    private val eventPublisher: DomainEventPublisher,
) {
    private val log by loggerDelegate()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: SprintDomainEvent) {
        when (event) {
            is SprintGoalChangedEvent ->
                eventPublisher
                    .publish(
                        AuditSprintGoalChangedEvent(
                            workspaceId = event.workspaceId,
                            sprintId = event.sprintId,
                            actorId = event.actorId,
                            fromGoal = event.fromGoal,
                            toGoal = event.toGoal,
                            eventId = event.eventId,
                            occurredAt = event.occurredAt,
                        ),
                    ).also { log.info("Published SprintGoalChangedEvent to Audit module: {}", event) }

            is SprintPeriodChangedEvent ->
                eventPublisher
                    .publish(
                        AuditSprintPeriodChangedEvent(
                            workspaceId = event.workspaceId,
                            sprintId = event.sprintId,
                            actorId = event.actorId,
                            fromPeriod = event.fromPeriod,
                            toPeriod = event.toPeriod,
                            eventId = event.eventId,
                            occurredAt = event.occurredAt,
                        ),
                    ).also { log.info("Published SprintPeriodChangedEvent to Audit module: {}", event) }

            is TaskAddedToSprintEvent ->
                eventPublisher
                    .publish(
                        AuditTaskAddedToSprintEvent(
                            workspaceId = event.workspaceId,
                            sprintId = event.sprintId,
                            taskId = event.taskId,
                            actorId = event.actorId,
                            eventId = event.eventId,
                            occurredAt = event.occurredAt,
                        ),
                    ).also { log.info("Published TaskAddedToSprintEvent to Audit module: {}", event) }

            is TaskRemovedFromSprintEvent ->
                eventPublisher
                    .publish(
                        AuditTaskRemovedFromSprintEvent(
                            workspaceId = event.workspaceId,
                            sprintId = event.sprintId,
                            taskId = event.taskId,
                            actorId = event.actorId,
                            eventId = event.eventId,
                            occurredAt = event.occurredAt,
                        ),
                    ).also { log.info("Published TaskRemovedFromSprintEvent to Audit module: {}", event) }
        }
    }
}
