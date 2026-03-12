package pizza.psycho.sos.project.task.application.event.handler

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.support.log.loggerDelegate
import pizza.psycho.sos.project.task.domain.event.TaskAssigneeChangedEvent
import pizza.psycho.sos.project.task.domain.event.TaskDeletedEvent
import pizza.psycho.sos.project.task.domain.event.TaskDomainEvent
import pizza.psycho.sos.project.task.domain.event.TaskDueDateChangedEvent
import pizza.psycho.sos.project.task.domain.event.TaskStatusChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskAssigneeChangedEvent as AuditTaskAssigneeChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskDeletedEvent as AuditTaskDeletedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskDueDateChangedEvent as AuditTaskDueDateChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskStatusChangedEvent as AuditTaskStatusChangedEvent

@Component
class TaskDomainEventPublishingHandler(
    private val eventPublisher: DomainEventPublisher,
) {
    private val log by loggerDelegate()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: TaskDomainEvent) =
        when (event) {
            is TaskStatusChangedEvent ->
                eventPublisher
                    .publish(
                        AuditTaskStatusChangedEvent(
                            workspaceId = event.workspaceId,
                            actorId = event.actorId,
                            taskId = event.taskId,
                            fromStatus = event.fromStatus,
                            toStatus = event.toStatus,
                            eventId = event.eventId,
                            occurredAt = event.occurredAt,
                        ),
                    ).also { log.info("Task status changed event: $event") }

            is TaskAssigneeChangedEvent ->
                eventPublisher
                    .publish(
                        AuditTaskAssigneeChangedEvent(
                            workspaceId = event.workspaceId,
                            actorId = event.actorId,
                            taskId = event.taskId,
                            fromAssigneeId = event.fromAssigneeId,
                            toAssigneeId = event.toAssigneeId,
                            eventId = event.eventId,
                            occurredAt = event.occurredAt,
                        ),
                    ).also { log.info("Task assignee changed event: $event") }

            is TaskDueDateChangedEvent ->
                eventPublisher
                    .publish(
                        AuditTaskDueDateChangedEvent(
                            workspaceId = event.workspaceId,
                            actorId = event.actorId,
                            taskId = event.taskId,
                            fromDueDate = event.fromDueDate,
                            toDueDate = event.toDueDate,
                            eventId = event.eventId,
                            occurredAt = event.occurredAt,
                        ),
                    ).also { log.info("Task due date changed event: $event") }

            is TaskDeletedEvent ->
                eventPublisher
                    .publish(
                        AuditTaskDeletedEvent(
                            workspaceId = event.workspaceId,
                            actorId = event.actorId,
                            taskId = event.taskId,
                            taskTitle = event.taskTitle,
                            eventId = event.eventId,
                            occurredAt = event.occurredAt,
                        ),
                    ).also { log.info("Task deleted event: $event") }
        }
}
