package pizza.psycho.sos.audit.application.listener

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import pizza.psycho.sos.audit.application.listener.event.SprintGoalChangedEvent
import pizza.psycho.sos.audit.application.listener.event.SprintPeriodChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskAddedToSprintEvent
import pizza.psycho.sos.audit.application.listener.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.audit.application.listener.event.TaskStatusChangedEvent
import pizza.psycho.sos.audit.domain.vo.AuditEventType
import pizza.psycho.sos.audit.domain.vo.AuditTargetType
import pizza.psycho.sos.audit.infrastructure.persistence.AuditLogRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class AuditLogEventListenerTests {
    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @AfterEach
    fun tearDown() {
        auditLogRepository.deleteAllInBatch()
    }

    private fun publish(event: Any) {
        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(event)
        }
    }

    @Test
    @DisplayName("SprintGoalChangedEvent 발생 시 AuditLog 저장")
    fun sprintGoalChangedEvent_shouldSaveAuditLog() {
        val event =
            SprintGoalChangedEvent(
                workspaceId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                sprintId = UUID.randomUUID(),
                fromGoal = "old",
                toGoal = "new",
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
            )

        publish(event)

        val log = auditLogRepository.findAll().single()

        assertThat(log.targetType).isEqualTo(AuditTargetType.SPRINT)
        assertThat(log.auditEventType).isEqualTo(AuditEventType.SPRINT_GOAL_CHANGED)
        assertThat(log.fromValue).isEqualTo("old")
        assertThat(log.toValue).isEqualTo("new")
        assertThat(log.eventId).isEqualTo(event.eventId)
        assertThat(log.occurredAt).isCloseTo(event.occurredAt, within(1, ChronoUnit.SECONDS))
    }

    @Test
    @DisplayName("SprintPeriodChangedEvent 발생 시 AuditLog 저장")
    fun sprintPeriodChangedEvent_shouldSaveAuditLog() {
        val event =
            SprintPeriodChangedEvent(
                workspaceId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                sprintId = UUID.randomUUID(),
                fromStartDate = Instant.parse("2026-01-01T00:00:00Z"),
                fromEndDate = Instant.parse("2026-01-14T00:00:00Z"),
                toStartDate = Instant.parse("2026-01-02T00:00:00Z"),
                toEndDate = Instant.parse("2026-01-15T00:00:00Z"),
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
            )

        publish(event)

        val log = auditLogRepository.findAll().single()

        assertThat(log.targetType).isEqualTo(AuditTargetType.SPRINT)
        assertThat(log.auditEventType).isEqualTo(AuditEventType.SPRINT_PERIOD_CHANGED)
        assertThat(log.fromValue).contains("~")
        assertThat(log.toValue).contains("~")
    }

    @Test
    @DisplayName("TaskAddedToSprintEvent 발생 시 AuditLog 저장")
    fun taskAddedToSprintEvent_shouldSaveAuditLog() {
        val event =
            TaskAddedToSprintEvent(
                workspaceId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                sprintId = UUID.randomUUID(),
                taskId = UUID.randomUUID(),
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
                sprintStartDate = Instant.now(),
                sprintEndDate = Instant.now(),
            )

        publish(event)

        val log = auditLogRepository.findAll().single()

        assertThat(log.targetType).isEqualTo(AuditTargetType.SPRINT)
        assertThat(log.auditEventType).isEqualTo(AuditEventType.TASK_ADDED_TO_SPRINT)
        assertThat(log.toValue).isEqualTo(event.taskId.toString())
    }

    @Test
    @DisplayName("TaskRemovedFromSprintEvent 발생 시 AuditLog 저장")
    fun taskRemovedFromSprintEvent_shouldSaveAuditLog() {
        val event =
            TaskRemovedFromSprintEvent(
                workspaceId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                sprintId = UUID.randomUUID(),
                taskId = UUID.randomUUID(),
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
                sprintStartDate = Instant.now(),
                sprintEndDate = Instant.now(),
            )

        publish(event)

        val log = auditLogRepository.findAll().single()

        assertThat(log.targetType).isEqualTo(AuditTargetType.SPRINT)
        assertThat(log.auditEventType).isEqualTo(AuditEventType.TASK_REMOVED_FROM_SPRINT)
        assertThat(log.fromValue).isEqualTo(event.taskId.toString())
    }

    @Test
    @DisplayName("TaskStatusChangedEvent 발생 시 AuditLog 저장")
    fun taskStatusChangedEvent_shouldSaveAuditLog() {
        val event =
            TaskStatusChangedEvent(
                workspaceId = UUID.randomUUID(),
                sprintId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                taskId = UUID.randomUUID(),
                fromStatus = "TODO",
                toStatus = "DONE",
                eventId = UUID.randomUUID(),
                occurredAt = Instant.now(),
            )

        publish(event)

        val log = auditLogRepository.findAll().single()

        assertThat(log.targetType).isEqualTo(AuditTargetType.TASK)
        assertThat(log.auditEventType).isEqualTo(AuditEventType.TASK_STATUS_CHANGED)
        assertThat(log.fromValue).isEqualTo("TODO")
        assertThat(log.toValue).isEqualTo("DONE")
    }
}
