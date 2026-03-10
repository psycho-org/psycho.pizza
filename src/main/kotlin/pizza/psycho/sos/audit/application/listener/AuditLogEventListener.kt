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
import pizza.psycho.sos.audit.domain.entity.AuditLog
import pizza.psycho.sos.audit.domain.vo.AuditEventType
import pizza.psycho.sos.audit.domain.vo.AuditTargetType
import pizza.psycho.sos.audit.infrastructure.persistence.AuditLogRepository
import java.time.Instant

@Component
class AuditLogEventListener(
    private val auditLogRepository: AuditLogRepository,
) {
    // --- 🔥 Sprint 관련 이벤트 리스너 ---

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: SprintGoalChangedEvent) {
        saveLog(
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
        saveLog(
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
        saveLog(
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
        saveLog(
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
        saveLog(
            event.workspaceId,
            event.actorId,
            AuditTargetType.TASK,
            event.taskId,
            AuditEventType.TASK_STATUS_CHANGED,
            event.fromStatus,
            event.toStatus,
            event.occurredAt,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: TaskAssigneeChangedEvent) {
        saveLog(
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
        saveLog(
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
        saveLog(
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
        saveLog(
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

    // --- 공통 저장 로직 ---

    private fun saveLog(
        workspaceId: java.util.UUID,
        actorId: java.util.UUID?,
        targetType: AuditTargetType,
        targetId: java.util.UUID,
        auditEventType: AuditEventType,
        fromValue: String?,
        toValue: String?,
        occurredAt: Instant,
    ) {
        val auditLog =
            AuditLog(
                workspaceId = workspaceId,
                actorId = actorId,
                targetType = targetType,
                targetId = targetId,
                auditEventType = auditEventType,
                fromValue = fromValue,
                toValue = toValue,
                occurredAt = occurredAt,
            )
        auditLogRepository.save(auditLog)
    }
}
