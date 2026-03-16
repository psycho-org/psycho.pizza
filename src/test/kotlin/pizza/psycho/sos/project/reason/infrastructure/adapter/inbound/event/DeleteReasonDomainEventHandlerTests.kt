package pizza.psycho.sos.project.reason.infrastructure.adapter.inbound.event

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import pizza.psycho.sos.project.project.domain.event.ProjectDeletedEvent
import pizza.psycho.sos.project.reason.application.port.inbound.usecase.RecordReasonUseCase
import pizza.psycho.sos.project.reason.application.port.inbound.usecase.command.RecordReasonCommand
import pizza.psycho.sos.project.sprint.domain.event.SprintDeletedEvent
import pizza.psycho.sos.project.task.domain.event.TaskDeletedEvent
import java.util.UUID

class DeleteReasonDomainEventHandlerTests {
    private val recordReasonUseCase = mockk<RecordReasonUseCase>(relaxed = true)
    private val handler = DeleteReasonDomainEventHandler(recordReasonUseCase)

    @Test
    fun `project deleted event를 delete reason event로 변환한다`() {
        val event =
            ProjectDeletedEvent(
                workspaceId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                projectId = UUID.randomUUID(),
                projectName = "프로젝트",
                reason = "삭제 사유",
                eventId = UUID.randomUUID(),
            )

        handler.handle(event)

        verify(exactly = 1) {
            recordReasonUseCase.record(
                match<RecordReasonCommand> {
                    it.targetId == event.projectId &&
                        it.workspaceId == event.workspaceId &&
                        it.reason == "삭제 사유"
                },
            )
        }
    }

    @Test
    fun `sprint deleted event를 delete reason event로 변환한다`() {
        val event =
            SprintDeletedEvent(
                workspaceId = UUID.randomUUID(),
                sprintId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                sprintName = "스프린트",
                reason = "삭제 사유",
                eventId = UUID.randomUUID(),
            )

        handler.handle(event)

        verify(exactly = 1) {
            recordReasonUseCase.record(
                match<RecordReasonCommand> {
                    it.targetId == event.sprintId &&
                        it.workspaceId == event.workspaceId &&
                        it.reason == "삭제 사유"
                },
            )
        }
    }

    @Test
    fun `task deleted event를 delete reason event로 변환한다`() {
        val event =
            TaskDeletedEvent(
                workspaceId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                taskId = UUID.randomUUID(),
                taskTitle = "태스크",
                reason = "삭제 사유",
                eventId = UUID.randomUUID(),
            )

        handler.handle(event)

        verify(exactly = 1) {
            recordReasonUseCase.record(
                match<RecordReasonCommand> {
                    it.targetId == event.taskId &&
                        it.workspaceId == event.workspaceId &&
                        it.reason == "삭제 사유"
                },
            )
        }
    }

    @Test
    fun `reason이 없으면 delete reason event를 발행하지 않는다`() {
        val event =
            TaskDeletedEvent(
                workspaceId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                taskId = UUID.randomUUID(),
                taskTitle = "태스크",
                reason = null,
                eventId = UUID.randomUUID(),
            )

        handler.handle(event)

        verify(exactly = 0) { recordReasonUseCase.record(any<RecordReasonCommand>()) }
    }
}
