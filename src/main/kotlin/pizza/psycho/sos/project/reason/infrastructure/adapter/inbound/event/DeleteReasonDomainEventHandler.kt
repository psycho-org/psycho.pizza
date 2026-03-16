package pizza.psycho.sos.project.reason.infrastructure.adapter.inbound.event

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.project.project.domain.event.ProjectDeletedEvent
import pizza.psycho.sos.project.reason.application.port.inbound.usecase.RecordReasonUseCase
import pizza.psycho.sos.project.reason.application.port.inbound.usecase.command.RecordReasonCommand
import pizza.psycho.sos.project.reason.domain.model.vo.EventType
import pizza.psycho.sos.project.reason.domain.model.vo.TargetType
import pizza.psycho.sos.project.sprint.domain.event.SprintDeletedEvent
import pizza.psycho.sos.project.task.domain.event.TaskDeletedEvent

@Component
class DeleteReasonDomainEventHandler(
    private val recordReasonUseCase: RecordReasonUseCase,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: ProjectDeletedEvent) {
        event.reason?.let { reason ->
            recordReasonUseCase.record(
                RecordReasonCommand(
                    reason = reason,
                    workspaceId = event.workspaceId,
                    targetId = event.projectId,
                    targetType = TargetType.PROJECT,
                    eventId = event.eventId,
                    eventType = EventType.DELETE,
                ),
            )
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: SprintDeletedEvent) {
        event.reason?.let { reason ->
            recordReasonUseCase.record(
                RecordReasonCommand(
                    reason = reason,
                    workspaceId = event.workspaceId,
                    targetId = event.sprintId,
                    targetType = TargetType.SPRINT,
                    eventId = event.eventId,
                    eventType = EventType.DELETE,
                ),
            )
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: TaskDeletedEvent) {
        event.reason?.let { reason ->
            recordReasonUseCase.record(
                RecordReasonCommand(
                    reason = reason,
                    workspaceId = event.workspaceId,
                    targetId = event.taskId,
                    targetType = TargetType.TASK,
                    eventId = event.eventId,
                    eventType = EventType.DELETE,
                ),
            )
        }
    }
}
