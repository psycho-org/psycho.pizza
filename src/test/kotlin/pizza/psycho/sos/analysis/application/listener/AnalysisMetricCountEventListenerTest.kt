package pizza.psycho.sos.analysis.application.listener

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import pizza.psycho.sos.analysis.domain.vo.AnalysisEventSubtype
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisMetricCountRepository
import pizza.psycho.sos.audit.application.listener.event.SprintGoalChangedEvent
import pizza.psycho.sos.audit.application.listener.event.SprintPeriodChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskAddedToSprintEvent
import pizza.psycho.sos.audit.application.listener.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.audit.application.listener.event.TaskStatusChangedEvent
import java.time.Instant
import java.util.UUID

@SpringBootTest
class AnalysisMetricCountEventListenerTest {
    @Autowired
    private lateinit var listener: AnalysisMetricCountEventListener

    @Autowired
    private lateinit var repository: AnalysisMetricCountRepository

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Test
    fun `Sprint Goal 변경 시 fromGoal이 비어있지 않으면 GOAL_UPDATED count가 증가한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()

        listener.on(
            SprintGoalChangedEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                sprintId = sprintId,
                fromGoal = "기존 목표",
                toGoal = "변경 목표",
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
            ),
        )

        val saved =
            repository.findByWorkspaceIdAndSprintIdAndEventSubtype(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = AnalysisEventSubtype.GOAL_UPDATED,
            )

        assertThat(saved).isNotNull
        assertThat(saved!!.count).isEqualTo(1)
    }

    @Test
    fun `Sprint Goal 변경 시 fromGoal이 비어있으면 count를 증가시키지 않는다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()

        listener.on(
            SprintGoalChangedEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                sprintId = sprintId,
                fromGoal = "",
                toGoal = "변경 목표",
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
            ),
        )

        val saved =
            repository.findByWorkspaceIdAndSprintIdAndEventSubtype(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = AnalysisEventSubtype.GOAL_UPDATED,
            )

        assertThat(saved).isNull()
    }

    @Test
    fun `Sprint 기간이 실제로 바뀌면 PERIOD_UPDATED count가 증가한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()

        listener.on(
            SprintPeriodChangedEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                sprintId = sprintId,
                fromStartDate = Instant.parse("2026-03-01T00:00:00Z"),
                fromEndDate = Instant.parse("2026-03-14T00:00:00Z"),
                toStartDate = Instant.parse("2026-03-02T00:00:00Z"),
                toEndDate = Instant.parse("2026-03-15T00:00:00Z"),
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
            ),
        )

        val saved =
            repository.findByWorkspaceIdAndSprintIdAndEventSubtype(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = AnalysisEventSubtype.PERIOD_UPDATED,
            )

        assertThat(saved).isNotNull
        assertThat(saved!!.count).isEqualTo(1)
    }

    @Test
    fun `Sprint 기간이 바뀌지 않았으면 PERIOD_UPDATED count를 증가시키지 않는다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val start = Instant.parse("2026-03-01T00:00:00Z")
        val end = Instant.parse("2026-03-14T00:00:00Z")

        listener.on(
            SprintPeriodChangedEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                sprintId = sprintId,
                fromStartDate = start,
                fromEndDate = end,
                toStartDate = start,
                toEndDate = end,
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
            ),
        )

        val saved =
            repository.findByWorkspaceIdAndSprintIdAndEventSubtype(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = AnalysisEventSubtype.PERIOD_UPDATED,
            )

        assertThat(saved).isNull()
    }

    @Test
    fun `Done에서 In Progress로 회귀하면 STATUS_REGRESSION_FROM_DONE count가 증가한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()

        listener.on(
            TaskStatusChangedEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                sprintId = sprintId,
                taskId = UUID.randomUUID(),
                fromStatus = "DONE",
                toStatus = "IN_PROGRESS",
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
            ),
        )

        val saved =
            repository.findByWorkspaceIdAndSprintIdAndEventSubtype(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = AnalysisEventSubtype.STATUS_REGRESSION_FROM_DONE,
            )

        assertThat(saved).isNotNull
        assertThat(saved!!.count).isEqualTo(1)
    }

    @Test
    fun `Todo에서 Done으로 바로 가면 TODO_TO_DONE_DIRECT count가 증가한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()

        listener.on(
            TaskStatusChangedEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                sprintId = sprintId,
                taskId = UUID.randomUUID(),
                fromStatus = "TODO",
                toStatus = "DONE",
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
            ),
        )

        val saved =
            repository.findByWorkspaceIdAndSprintIdAndEventSubtype(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = AnalysisEventSubtype.TODO_TO_DONE_DIRECT,
            )

        assertThat(saved).isNotNull
        assertThat(saved!!.count).isEqualTo(1)
    }

    @Test
    fun `Canceled가 아닌 상태에서 Canceled로 변경되면 STATUS_CHANGED_TO_CANCELED count가 증가한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()

        listener.on(
            TaskStatusChangedEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                sprintId = sprintId,
                taskId = UUID.randomUUID(),
                fromStatus = "DONE",
                toStatus = "CANCELED",
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
            ),
        )

        val saved =
            repository.findByWorkspaceIdAndSprintIdAndEventSubtype(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = AnalysisEventSubtype.STATUS_CHANGED_TO_CANCELED,
            )

        assertThat(saved).isNotNull
        assertThat(saved!!.count).isEqualTo(1)
    }

    @Test
    fun `Sprint 기간 안에서 Task 추가가 발생하면 SCOPE_CHURN count가 증가한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val sprintStart = Instant.parse("2026-03-01T00:00:00Z")
        val sprintEnd = Instant.parse("2026-03-14T23:59:59Z")
        val occurredAt = Instant.parse("2026-03-05T10:00:00Z")

        listener.on(
            TaskAddedToSprintEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                sprintId = sprintId,
                taskId = UUID.randomUUID(),
                sprintStartDate = sprintStart,
                sprintEndDate = sprintEnd,
                eventId = UUID.randomUUID(),
                occurredAt = occurredAt,
            ),
        )

        val saved =
            repository.findByWorkspaceIdAndSprintIdAndEventSubtype(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = AnalysisEventSubtype.SCOPE_CHURN,
            )

        assertThat(saved).isNotNull
        assertThat(saved!!.count).isEqualTo(1)
    }

    @Test
    fun `Sprint 기간 밖에서 Task 제거가 발생하면 SCOPE_CHURN count를 증가시키지 않는다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val sprintStart = Instant.parse("2026-03-01T00:00:00Z")
        val sprintEnd = Instant.parse("2026-03-14T23:59:59Z")
        val occurredAt = Instant.parse("2026-03-20T10:00:00Z")

        listener.on(
            TaskRemovedFromSprintEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                sprintId = sprintId,
                taskId = UUID.randomUUID(),
                sprintStartDate = sprintStart,
                sprintEndDate = sprintEnd,
                eventId = UUID.randomUUID(),
                occurredAt = occurredAt,
            ),
        )

        val saved =
            repository.findByWorkspaceIdAndSprintIdAndEventSubtype(
                workspaceId = workspaceId,
                sprintId = sprintId,
                eventSubtype = AnalysisEventSubtype.SCOPE_CHURN,
            )

        assertThat(saved).isNull()
    }
}
