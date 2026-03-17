package pizza.psycho.sos.analysis.application.listener

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.analysis.application.service.AnalysisMetricCountService
import pizza.psycho.sos.analysis.domain.vo.AnalysisEventSubtype
import pizza.psycho.sos.audit.application.listener.event.SprintGoalChangedEvent
import pizza.psycho.sos.audit.application.listener.event.SprintPeriodChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskAddedToSprintEvent
import pizza.psycho.sos.audit.application.listener.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.audit.application.listener.event.TaskStatusChangedEvent

/*
 * Domain Event를 수신하여 AnalysisMetricCount를 카운팅하는 이벤트 리스너
 */
@Component
class AnalysisMetricCountEventListener(
    private val analysisMetricCountService: AnalysisMetricCountService,
) {
    /*
     * Sprint Goal 변경
     * - from_value가 비어있지 않으면 카운팅
     */
    @TransactionalEventListener(
        // NOTE: 트랜잭션 있으면 commit 후 / 없으면 즉시 실행
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: SprintGoalChangedEvent) {
        if (event.fromGoal.isNotBlank()) {
            analysisMetricCountService.increase(
                workspaceId = event.workspaceId,
                sprintId = event.sprintId,
                eventSubtype = AnalysisEventSubtype.GOAL_UPDATED,
            )
        }
    }

    /*
     * Sprint 기간 변경
     * -
     */
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: SprintPeriodChangedEvent) {
        if (hasPeriodChanged(event)) {
            analysisMetricCountService.increase(
                workspaceId = event.workspaceId,
                sprintId = event.sprintId,
                eventSubtype = AnalysisEventSubtype.PERIOD_UPDATED,
            )
        }
    }

    private fun hasPeriodChanged(event: SprintPeriodChangedEvent): Boolean {
        val startDateChanged = event.fromStartDate.compareTo(event.toStartDate) != 0
        val endDateChanged = event.fromEndDate.compareTo(event.toEndDate) != 0

        return startDateChanged || endDateChanged
    }

    /*
     * Task 상태 변경
     */
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: TaskStatusChangedEvent) {
        val from = event.fromStatus
        val to = event.toStatus

        // Done -> In Progress / Todo
        if (from == STATUS_DONE && (to == STATUS_IN_PROGRESS || to == STATUS_TODO)) {
            analysisMetricCountService.increase(
                workspaceId = event.workspaceId,
                sprintId = event.sprintId,
                eventSubtype = AnalysisEventSubtype.STATUS_REGRESSION_FROM_DONE,
            )
        }

        // Todo -> Done
        if (from == STATUS_TODO && to == STATUS_DONE) {
            analysisMetricCountService.increase(
                workspaceId = event.workspaceId,
                sprintId = event.sprintId,
                eventSubtype = AnalysisEventSubtype.TODO_TO_DONE_DIRECT,
            )
        }

        // Todo/In Progress/Done -> Canceled
        if (from != STATUS_CANCELED && to == STATUS_CANCELED) {
            analysisMetricCountService.increase(
                workspaceId = event.workspaceId,
                sprintId = event.sprintId,
                eventSubtype = AnalysisEventSubtype.STATUS_CHANGED_TO_CANCELED,
            )
        }
    }

    private companion object {
        const val STATUS_TODO = "TODO"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_DONE = "DONE"
        const val STATUS_CANCELED = "CANCELED"
    }

    /*
     * Sprint 범위 변경 (Task 추가)
     */
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: TaskAddedToSprintEvent) {
        val occurredInSprint =
            !event.occurredAt.isBefore(event.sprintStartDate) &&
                !event.occurredAt.isAfter(event.sprintEndDate)

        if (!occurredInSprint) return

        analysisMetricCountService.increase(
            workspaceId = event.workspaceId,
            sprintId = event.sprintId,
            eventSubtype = AnalysisEventSubtype.SCOPE_CHURN,
        )
    }

    /*
     * Sprint 범위 변경 (Task 제거)
     */
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun on(event: TaskRemovedFromSprintEvent) {
        val occurredInSprint =
            !event.occurredAt.isBefore(event.sprintStartDate) &&
                !event.occurredAt.isAfter(event.sprintEndDate)

        if (!occurredInSprint) return

        analysisMetricCountService.increase(
            workspaceId = event.workspaceId,
            sprintId = event.sprintId,
            eventSubtype = AnalysisEventSubtype.SCOPE_CHURN,
        )
    }
}
