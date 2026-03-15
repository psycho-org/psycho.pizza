package pizza.psycho.sos.project.task.application.event.handler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.project.task.application.port.out.TaskSprintParticipationQuery
import pizza.psycho.sos.project.task.domain.event.TaskDeletedEvent
import pizza.psycho.sos.project.task.domain.event.TaskStatusChangedEvent
import java.util.UUID
import pizza.psycho.sos.audit.application.listener.event.TaskDeletedEvent as AuditTaskDeletedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskStatusChangedEvent as AuditTaskStatusChangedEvent

class TaskDomainEventPublishingHandlerTests {
    private val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)
    private val sprintMembershipQuery = mockk<TaskSprintParticipationQuery>()
    private val sprintMembershipRegistry = TaskEventSprintMembershipRegistry()
    private val handler = TaskDomainEventPublishingHandler(eventPublisher, sprintMembershipQuery, sprintMembershipRegistry)

    @Test
    fun `task deleted event uses sprint membership captured before commit`() {
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val event =
            TaskDeletedEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                taskId = taskId,
                taskTitle = "deleted task",
                eventId = UUID.randomUUID(),
            )

        sprintMembershipRegistry.register(event.eventId, true)
        handler.handle(event)

        verify(exactly = 1) {
            eventPublisher.publish(
                match<AuditTaskDeletedEvent> {
                    it.taskId == taskId && it.workspaceId == workspaceId
                },
            )
        }
    }

    @Test
    fun `task status changed event uses sprint membership captured before commit`() {
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val event =
            TaskStatusChangedEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                taskId = taskId,
                fromStatus = "IN_PROGRESS",
                toStatus = "TODO",
                eventId = UUID.randomUUID(),
            )

        sprintMembershipRegistry.register(event.eventId, true)
        handler.handle(event)

        verify(exactly = 1) {
            eventPublisher.publish(
                match<AuditTaskStatusChangedEvent> {
                    it.taskId == taskId && it.fromStatus == "IN_PROGRESS" && it.toStatus == "TODO"
                },
            )
        }
    }

    @Test
    fun `non sprint task event is skipped`() {
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val event =
            TaskDeletedEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                taskId = taskId,
                taskTitle = "deleted task",
                eventId = UUID.randomUUID(),
            )

        every { sprintMembershipQuery.existsActiveSprintByTaskId(taskId, workspaceId) } returns false

        handler.handle(event)

        verify(exactly = 0) { eventPublisher.publish(any<AuditTaskDeletedEvent>()) }
    }
}
