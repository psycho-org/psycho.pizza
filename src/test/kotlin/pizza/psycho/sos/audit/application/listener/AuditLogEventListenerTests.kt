package pizza.psycho.sos.audit.application.listener

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import pizza.psycho.sos.audit.application.listener.event.TaskAssigneeChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskStatusChangedEvent
import pizza.psycho.sos.audit.domain.vo.AuditEventType
import pizza.psycho.sos.audit.domain.vo.AuditTargetType
import pizza.psycho.sos.audit.infrastructure.persistence.AuditLogRepository
import java.time.temporal.ChronoUnit
import java.util.UUID

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

    @Test
    @DisplayName("Task 상태 변경 이벤트가 발행되면 AuditLog가 성공적으로 저장되어야 한다")
    fun shouldSaveAuditLogWhenTaskStatusChangedEventPublished() {
        // given
        val eventId = UUID.randomUUID()
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val actorId = UUID.randomUUID()

        val event =
            TaskStatusChangedEvent(
                eventId = eventId,
                workspaceId = workspaceId,
                taskId = taskId,
                actorId = actorId,
                fromStatus = "TODO",
                toStatus = "IN_PROGRESS",
            )

        // when
        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(event)
        }

        // then
        val logs = auditLogRepository.findAll()
        assertThat(logs).hasSize(1)

        val actual = logs[0]
        assertThat(actual.eventId).isEqualTo(eventId)
        assertThat(actual.workspaceId).isEqualTo(workspaceId)
        assertThat(actual.targetId).isEqualTo(taskId)
        assertThat(actual.targetType).isEqualTo(AuditTargetType.TASK)
        assertThat(actual.auditEventType).isEqualTo(AuditEventType.TASK_STATUS_CHANGED)
        assertThat(actual.fromValue).isEqualTo("TODO")
        assertThat(actual.toValue).isEqualTo("IN_PROGRESS")
        assertThat(actual.actorId).isEqualTo(actorId)
        // NOTE: DB 저장 시 시간 정밀도(나노초)가 잘리면서 CI/CD에서 간헐적으로 실패하는 것을 방지합니다.
        assertThat(actual.occurredAt).isCloseTo(event.occurredAt, within(1, ChronoUnit.SECONDS))
    }

    @Test
    @DisplayName("하나의 요청에서 발생한 다수의 이벤트는 동일한 eventId로 AuditLog에 저장되어야 한다")
    fun shouldGroupMultipleEventsWithSameEventId() {
        // given
        val sharedEventId = UUID.randomUUID()
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val actorId = UUID.randomUUID()

        val statusEvent =
            TaskStatusChangedEvent(
                eventId = sharedEventId,
                workspaceId = workspaceId,
                taskId = taskId,
                actorId = actorId,
                fromStatus = "TODO",
                toStatus = "IN_PROGRESS",
            )

        val assigneeEvent =
            TaskAssigneeChangedEvent(
                eventId = sharedEventId,
                workspaceId = workspaceId,
                taskId = taskId,
                actorId = actorId,
                fromAssigneeId = null,
                toAssigneeId = actorId.toString(),
            )

        // when
        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(statusEvent)
            eventPublisher.publishEvent(assigneeEvent)
        }

        // then
        val logs = auditLogRepository.findAll()

        assertThat(logs).hasSize(2)
        assertThat(logs).allMatch { it.eventId == sharedEventId }

        val eventTypes = logs.map { it.auditEventType }
        assertThat(eventTypes).containsExactlyInAnyOrder(
            AuditEventType.TASK_STATUS_CHANGED,
            AuditEventType.TASK_ASSIGNEE_CHANGED,
        )
    }
}
