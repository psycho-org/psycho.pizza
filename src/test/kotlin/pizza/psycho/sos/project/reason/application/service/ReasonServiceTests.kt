package pizza.psycho.sos.project.reason.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import pizza.psycho.sos.project.reason.application.port.inbound.usecase.command.RecordReasonCommand
import pizza.psycho.sos.project.reason.domain.model.entity.Reason
import pizza.psycho.sos.project.reason.domain.model.vo.EventType
import pizza.psycho.sos.project.reason.domain.model.vo.TargetType
import pizza.psycho.sos.project.reason.domain.repository.ReasonRepository
import java.util.UUID

class ReasonServiceTests {
    private val reasonRepository = mockk<ReasonRepository>(relaxed = true)
    private val reasonService = ReasonService(reasonRepository)

    @Test
    fun `같은 eventId 의 reason 이 이미 있으면 저장하지 않는다`() {
        val eventId = UUID.randomUUID()
        every { reasonRepository.existsByEventId(eventId) } returns true

        reasonService.record(
            RecordReasonCommand(
                reason = "삭제 사유",
                workspaceId = UUID.randomUUID(),
                targetId = UUID.randomUUID(),
                targetType = TargetType.TASK,
                eventId = eventId,
                eventType = EventType.DELETE,
            ),
        )

        verify(exactly = 0) { reasonRepository.save(any<Reason>()) }
    }

    @Test
    fun `처음 보는 eventId 의 reason 은 저장한다`() {
        val eventId = UUID.randomUUID()
        every { reasonRepository.existsByEventId(eventId) } returns false

        reasonService.record(
            RecordReasonCommand(
                reason = "삭제 사유",
                workspaceId = UUID.randomUUID(),
                targetId = UUID.randomUUID(),
                targetType = TargetType.PROJECT,
                eventId = eventId,
                eventType = EventType.DELETE,
            ),
        )

        verify(exactly = 1) { reasonRepository.save(any<Reason>()) }
    }
}
