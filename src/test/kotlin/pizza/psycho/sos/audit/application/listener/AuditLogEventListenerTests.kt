package pizza.psycho.sos.audit.application.listener

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.audit.application.listener.event.TaskAssigneeChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskStatusChangedEvent
import pizza.psycho.sos.audit.domain.vo.AuditEventType
import pizza.psycho.sos.audit.domain.vo.AuditTargetType
import pizza.psycho.sos.audit.infrastructure.persistence.AuditLogRepository
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditLogEventListenerTests {
    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    // 🔥 TestTransaction 강제 커밋으로 인해 자동 롤백이 안 되므로, 매 테스트가 끝나면 데이터를 직접 비워줍니다.
    @AfterEach
    fun tearDown() {
        auditLogRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("Task 상태 변경 이벤트가 발행되면 AuditLog가 성공적으로 저장되어야 한다")
    fun shouldSaveAuditLogWhenTaskStatusChangedEventPublished() {
        // given
        val eventId = UUID.randomUUID() // 🔥 eventId 추가
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val actorId = UUID.randomUUID()

        val event =
            TaskStatusChangedEvent(
                eventId = eventId, // 🔥 eventId 파라미터 추가
                workspaceId = workspaceId,
                taskId = taskId,
                actorId = actorId,
                fromStatus = "TODO",
                toStatus = "IN_PROGRESS",
            )

        // when
        eventPublisher.publishEvent(event)

        // 트랜잭션을 명시적으로 종료(커밋)하여 리스너를 실행시킴
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // then
        val logs = auditLogRepository.findAll()
        assertThat(logs).hasSize(1)

        val actual = logs[0]
        assertThat(actual.eventId).isEqualTo(eventId) // 🔥 eventId 검증 추가
        assertThat(actual.workspaceId).isEqualTo(workspaceId)
        assertThat(actual.targetId).isEqualTo(taskId)
        assertThat(actual.targetType).isEqualTo(AuditTargetType.TASK)
        assertThat(actual.auditEventType).isEqualTo(AuditEventType.TASK_STATUS_CHANGED)
        assertThat(actual.fromValue).isEqualTo("TODO")
        assertThat(actual.toValue).isEqualTo("IN_PROGRESS")
        assertThat(actual.actorId).isEqualTo(actorId)

        // 🔥 TODO 해결: occurredAt이 이벤트 생성 시각과 동일하게 잘 저장되었는지 검증
        assertThat(actual.occurredAt).isEqualTo(event.occurredAt)
    }

    @Test
    @DisplayName("하나의 요청에서 발생한 다수의 이벤트는 동일한 eventId로 AuditLog에 저장되어야 한다")
    fun shouldGroupMultipleEventsWithSameEventId() {
        // given
        val sharedEventId = UUID.randomUUID()
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val actorId = UUID.randomUUID()

        // 첫 번째 이벤트: 상태 변경
        val statusEvent =
            TaskStatusChangedEvent(
                eventId = sharedEventId,
                workspaceId = workspaceId,
                taskId = taskId,
                actorId = actorId,
                fromStatus = "TODO",
                toStatus = "IN_PROGRESS",
            )

        // 두 번째 이벤트: 담당자 변경
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
        eventPublisher.publishEvent(statusEvent)
        eventPublisher.publishEvent(assigneeEvent)

        // 강제 커밋으로 AFTER_COMMIT 리스너 실행 유도
        if (TestTransaction.isActive()) {
            TestTransaction.flagForCommit()
            TestTransaction.end()
        }

        // then
        val logs = auditLogRepository.findAll()
        assertThat(logs).hasSize(2)

        // 모든 로그가 동일한 sharedEventId를 가지는지 검증
        assertThat(logs).allMatch { it.eventId == sharedEventId }

        // 각 이벤트가 알맞은 타입으로 저장되었는지 검증
        val eventTypes = logs.map { it.auditEventType }
        assertThat(eventTypes).containsExactlyInAnyOrder(
            AuditEventType.TASK_STATUS_CHANGED,
            AuditEventType.TASK_ASSIGNEE_CHANGED,
        )
    }
}
