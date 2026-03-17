package pizza.psycho.sos.audit.application.listener

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.audit.application.listener.event.SprintGoalChangedEvent
import pizza.psycho.sos.audit.application.listener.event.SprintPeriodChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskAddedToSprintEvent
import pizza.psycho.sos.audit.application.listener.event.TaskAssigneeChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskDeletedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskDueDateChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskProjectChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.audit.application.listener.event.TaskStatusChangedEvent
import pizza.psycho.sos.audit.application.service.AuditLogService
import pizza.psycho.sos.audit.domain.vo.AuditEventType
import pizza.psycho.sos.audit.domain.vo.AuditTargetType

/*
 * Domain Event를 수신하여 AuditLog를 저장하는 이벤트 리스너
 */
@Component
class AuditLogEventListener(
    private val auditLogService: AuditLogService,
) {
    // --- Sprint ---

    // MVP-03
    @TransactionalEventListener(
        // NOTE: 트랜잭션 안에서 실행되면 AFTER_COMMIT 실행, 트랜잭션 없으면 즉시 실행
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: SprintGoalChangedEvent) {
        auditLogService.createAuditLog(
            workspaceId = event.workspaceId,
            actorId = event.actorId,
            targetType = AuditTargetType.SPRINT,
            targetId = event.sprintId,
            auditEventType = AuditEventType.SPRINT_GOAL_CHANGED,
            fromValue = event.fromGoal,
            toValue = event.toGoal,
            eventId = event.eventId,
            occurredAt = event.occurredAt,
        )
    }

    // MVP-03
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: SprintPeriodChangedEvent) {
        auditLogService.createAuditLog(
            workspaceId = event.workspaceId,
            actorId = event.actorId,
            targetType = AuditTargetType.SPRINT,
            targetId = event.sprintId,
            auditEventType = AuditEventType.SPRINT_PERIOD_CHANGED,
            fromValue = "${event.fromStartDate}~${event.fromEndDate}",
            toValue = "${event.toStartDate}~${event.toEndDate}",
            eventId = event.eventId,
            occurredAt = event.occurredAt,
        )
    }

    // MVP-03
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: TaskAddedToSprintEvent) {
        auditLogService.createAuditLog(
            workspaceId = event.workspaceId,
            actorId = event.actorId,
            targetType = AuditTargetType.SPRINT,
            targetId = event.sprintId,
            auditEventType = AuditEventType.TASK_ADDED_TO_SPRINT,
            fromValue = null,
            toValue = event.taskId.toString(),
            eventId = event.eventId,
            occurredAt = event.occurredAt,
        )
    }

    // MVP-03
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: TaskRemovedFromSprintEvent) {
        auditLogService.createAuditLog(
            workspaceId = event.workspaceId,
            actorId = event.actorId,
            targetType = AuditTargetType.SPRINT,
            targetId = event.sprintId,
            auditEventType = AuditEventType.TASK_REMOVED_FROM_SPRINT,
            fromValue = event.taskId.toString(),
            toValue = null,
            eventId = event.eventId,
            occurredAt = event.occurredAt,
        )
    }

    // --- Task ---

    // MVP-03
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: TaskStatusChangedEvent) {
        auditLogService.createAuditLog(
            workspaceId = event.workspaceId,
            actorId = event.actorId,
            targetType = AuditTargetType.TASK,
            targetId = event.taskId,
            auditEventType = AuditEventType.TASK_STATUS_CHANGED,
            fromValue = event.fromStatus,
            toValue = event.toStatus,
            eventId = event.eventId,
            occurredAt = event.occurredAt,
        )
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: TaskAssigneeChangedEvent) {
        /*
        auditLogService.createAuditLog(
            workspaceId = event.workspaceId,
            actorId = event.actorId,
            targetType = AuditTargetType.TASK,
            targetId = event.taskId,
            auditEventType = AuditEventType.TASK_ASSIGNEE_CHANGED,
            fromValue = event.fromAssigneeId,
            toValue = event.toAssigneeId,
            eventId = event.eventId,
            occurredAt = event.occurredAt,
        )
         */
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: TaskDueDateChangedEvent) {
        /*
        auditLogService.createAuditLog(
            workspaceId = event.workspaceId,
            actorId = event.actorId,
            targetType = AuditTargetType.TASK,
            targetId = event.taskId,
            auditEventType = AuditEventType.TASK_DUE_DATE_CHANGED,
            fromValue = event.fromDueDate?.toString(),
            toValue = event.toDueDate?.toString(),
            eventId = event.eventId,
            occurredAt = event.occurredAt,
        )
         */
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: TaskProjectChangedEvent) {
        /*
        auditLogService.createAuditLog(
            workspaceId = event.workspaceId,
            actorId = event.actorId,
            targetType = AuditTargetType.TASK,
            targetId = event.taskId,
            auditEventType = AuditEventType.TASK_PROJECT_CHANGED,
            fromValue = event.fromProjectId.toString(),
            toValue = event.toProjectId.toString(),
            eventId = event.eventId,
            occurredAt = event.occurredAt,
        )
         */
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: TaskDeletedEvent) {
        /*
        auditLogService.createAuditLog(
            workspaceId = event.workspaceId,
            actorId = event.actorId,
            targetType = AuditTargetType.TASK,
            targetId = event.taskId,
            auditEventType = AuditEventType.TASK_DELETED,
            fromValue = event.taskTitle,
            toValue = null,
            eventId = event.eventId,
            occurredAt = event.occurredAt,
        )
         */
    }
}
