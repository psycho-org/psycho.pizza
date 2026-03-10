package pizza.psycho.sos.audit.application.listener

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
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

    @Test
    @DisplayName("Task 상태 변경 이벤트가 발행되면 AuditLog가 성공적으로 저장되어야 한다")
    fun shouldSaveAuditLogWhenTaskStatusChangedEventPublished() {
        // given
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val actorId = UUID.randomUUID()

        val event =
            TaskStatusChangedEvent(
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
        assertThat(actual.workspaceId).isEqualTo(workspaceId)
        assertThat(actual.targetId).isEqualTo(taskId)
        assertThat(actual.targetType).isEqualTo(AuditTargetType.TASK)
        assertThat(actual.auditEventType).isEqualTo(AuditEventType.TASK_STATUS_CHANGED)
        assertThat(actual.fromValue).isEqualTo("TODO")
        assertThat(actual.toValue).isEqualTo("IN_PROGRESS")
        assertThat(actual.actorId).isEqualTo(actorId)
    }
}
