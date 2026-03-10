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

@Component
class AuditLogEventListener(
    private val auditLogService: AuditLogService,
) {
    // --- 🔥 Sprint 관련 이벤트 리스너 ---

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: SprintGoalChangedEvent) {
        auditLogService.createAuditLog(
            event.workspaceId,
            event.actorId,
            AuditTargetType.SPRINT,
            event.sprintId,
            AuditEventType.SPRINT_GOAL_CHANGED,
            event.fromGoal,
            event.toGoal,
            event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: SprintPeriodChangedEvent) {
        auditLogService.createAuditLog(
            event.workspaceId,
            event.actorId,
            AuditTargetType.SPRINT,
            event.sprintId,
            AuditEventType.SPRINT_PERIOD_CHANGED,
            event.fromPeriod,
            event.toPeriod,
            event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: TaskAddedToSprintEvent) {
        auditLogService.createAuditLog(
            event.workspaceId,
            event.actorId,
            AuditTargetType.SPRINT,
            event.sprintId,
            AuditEventType.TASK_ADDED_TO_SPRINT,
            null,
            event.taskId.toString(),
            event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: TaskRemovedFromSprintEvent) {
        auditLogService.createAuditLog(
            event.workspaceId,
            event.actorId,
            AuditTargetType.SPRINT,
            event.sprintId,
            AuditEventType.TASK_REMOVED_FROM_SPRINT,
            event.taskId.toString(),
            null,
            event.occurredAt,
        )
    }

    // --- 🔥 Task 관련 이벤트 리스너 ---

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: TaskStatusChangedEvent) {
        auditLogService.createAuditLog(
            workspaceId = event.workspaceId,
            actorId = event.actorId,
            targetType = AuditTargetType.TASK,
            targetId = event.taskId,
            auditEventType = AuditEventType.TASK_STATUS_CHANGED,
            fromValue = event.fromStatus,
            toValue = event.toStatus,
            occurredAt = event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: TaskAssigneeChangedEvent) {
        auditLogService.createAuditLog(
            event.workspaceId,
            event.actorId,
            AuditTargetType.TASK,
            event.taskId,
            AuditEventType.TASK_ASSIGNEE_CHANGED,
            event.fromAssigneeId,
            event.toAssigneeId,
            event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: TaskDueDateChangedEvent) {
        auditLogService.createAuditLog(
            event.workspaceId,
            event.actorId,
            AuditTargetType.TASK,
            event.taskId,
            AuditEventType.TASK_DUE_DATE_CHANGED,
            event.fromDueDate?.toString(),
            event.toDueDate?.toString(),
            event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: TaskProjectChangedEvent) {
        auditLogService.createAuditLog(
            event.workspaceId,
            event.actorId,
            AuditTargetType.TASK,
            event.taskId,
            AuditEventType.TASK_PROJECT_CHANGED,
            event.fromProjectId.toString(),
            event.toProjectId.toString(),
            event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: TaskDeletedEvent) {
        auditLogService.createAuditLog(
            event.workspaceId,
            event.actorId,
            AuditTargetType.TASK,
            event.taskId,
            AuditEventType.TASK_DELETED,
            event.taskTitle,
            null,
            event.occurredAt,
        )
    }
}
