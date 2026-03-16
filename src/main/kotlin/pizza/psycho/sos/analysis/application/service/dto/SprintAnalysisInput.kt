package pizza.psycho.sos.analysis.application.service.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.util.UUID

/*
 * 릴레이 서버 (또는 SQS로 넘길 최종 입력 DTO)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SprintAnalysisInput(
    val schemaVersion: String,
    val context: Context,
    val summary: Summary,
    val metrics: Metrics,
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Context(
        val workspaceId: UUID,
        val sprint: Sprint,
    ) {
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        data class Sprint(
            val id: UUID,
            val name: String,
            val periodDays: Int,
            val totalTasksCount: Int,
        )
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Summary(
        val statusSnapshot: StatusSnapshot,
    ) {
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        data class StatusSnapshot(
            val todoCount: Int,
            val inProgressCount: Int,
            val doneCount: Int,
            val canceledCount: Int,
        )
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Metrics(
        val completion: Completion,
        val stability: Stability,
        val flow: Flow,
    ) {
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        data class Completion(
            val unassignedTasksCount: Int,
        )

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        data class Stability(
            val sprintGoalChangeCount: Int,
            val sprintPeriodChangeCount: Int,
        )

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        data class Flow(
            val reworkEventsCount: Int,
            val todoToDoneDirectCount: Int,
            val scopeChurnEventsCount: Int,
            val canceledTasksCount: Int,
        )
    }
}
