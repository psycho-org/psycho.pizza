package pizza.psycho.sos.analysis.application.service.dto

import java.util.UUID

/*
 * 릴레이 서버 (또는 SQS로 넘길 최종 입력 DTO)
 */
data class SprintAnalysisInput(
    val schemaVersion: String,
    val context: Context,
    val summary: Summary,
    val metrics: Metrics,
) {
    data class Context(
        val workspaceId: UUID,
        val sprint: Sprint,
    ) {
        data class Sprint(
            val id: UUID,
            val name: String,
            val periodDays: Int,
            val activeMembersCount: Int,
            val totalTasksCount: Int,
        )
    }

    data class Summary(
        val statusSnapshot: StatusSnapshot,
    ) {
        data class StatusSnapshot(
            val todoCount: Int,
            val inProgressCount: Int,
            val doneCount: Int,
            val canceledCount: Int,
        )
    }

    data class Metrics(
        val completion: Completion,
        val stability: Stability,
        val flow: Flow,
    ) {
        data class Completion(
            val unassignedTasksCount: Int,
        )

        data class Stability(
            val sprintGoalChangeCount: Int,
            val sprintPeriodChangeCount: Int,
        )

        data class Flow(
            val reworkEventsCount: Int,
            val todoToDoneDirectCount: Int,
            val scopeChurnEventsCount: Int,
            val canceledTasksCount: Int,
        )
    }
}
