package pizza.psycho.sos.project.sprint.application.event.handler

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.project.sprint.domain.event.SprintGoalChangedEvent
import pizza.psycho.sos.project.sprint.domain.event.SprintPeriodChangedEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskAddedToSprintEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.project.sprint.infrastructure.adapter.out.event.SprintDomainEventPublishingHandler
import java.util.UUID
import pizza.psycho.sos.audit.application.listener.event.SprintGoalChangedEvent as AuditSprintGoalChangedEvent
import pizza.psycho.sos.audit.application.listener.event.SprintPeriodChangedEvent as AuditSprintPeriodChangedEvent
import pizza.psycho.sos.audit.application.listener.event.TaskAddedToSprintEvent as AuditTaskAddedToSprintEvent
import pizza.psycho.sos.audit.application.listener.event.TaskRemovedFromSprintEvent as AuditTaskRemovedFromSprintEvent

class SprintDomainEventPublishingHandlerTests {
    private val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)
    private val handler = SprintDomainEventPublishingHandler(eventPublisher)

    @Test
    fun `sprint domain events are handled after commit`() {
        val method =
            SprintDomainEventPublishingHandler::class.java.getDeclaredMethod(
                "handle",
                pizza.psycho.sos.project.sprint.domain.event.SprintDomainEvent::class.java,
            )

        val annotation = method.getAnnotation(TransactionalEventListener::class.java)

        kotlin.test.assertNotNull(annotation)
        kotlin.test.assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase)
    }

    @Test
    fun `sprint goal changed event is republished as audit event`() {
        val event =
            SprintGoalChangedEvent(
                workspaceId = UUID.randomUUID(),
                sprintId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                fromGoal = "before",
                toGoal = "after",
                eventId = UUID.randomUUID(),
            )

        handler.handle(event)

        verify(exactly = 1) {
            eventPublisher.publish(
                match<AuditSprintGoalChangedEvent> {
                    it.workspaceId == event.workspaceId &&
                        it.sprintId == event.sprintId &&
                        it.actorId == event.actorId &&
                        it.fromGoal == event.fromGoal &&
                        it.toGoal == event.toGoal &&
                        it.eventId == event.eventId
                },
            )
        }
    }

    @Test
    fun `sprint period changed event is republished as audit event`() {
        val event =
            SprintPeriodChangedEvent(
                workspaceId = UUID.randomUUID(),
                sprintId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                fromPeriod = "before",
                toPeriod = "after",
                eventId = UUID.randomUUID(),
            )

        handler.handle(event)

        verify(exactly = 1) {
            eventPublisher.publish(
                match<AuditSprintPeriodChangedEvent> {
                    it.workspaceId == event.workspaceId &&
                        it.sprintId == event.sprintId &&
                        it.actorId == event.actorId &&
                        it.fromPeriod == event.fromPeriod &&
                        it.toPeriod == event.toPeriod &&
                        it.eventId == event.eventId
                },
            )
        }
    }

    @Test
    fun `task added to sprint event is republished as audit event`() {
        val event =
            TaskAddedToSprintEvent(
                workspaceId = UUID.randomUUID(),
                sprintId = UUID.randomUUID(),
                taskId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                eventId = UUID.randomUUID(),
            )

        handler.handle(event)

        verify(exactly = 1) {
            eventPublisher.publish(
                match<AuditTaskAddedToSprintEvent> {
                    it.workspaceId == event.workspaceId &&
                        it.sprintId == event.sprintId &&
                        it.taskId == event.taskId &&
                        it.actorId == event.actorId &&
                        it.eventId == event.eventId
                },
            )
        }
    }

    @Test
    fun `task removed from sprint event is republished as audit event`() {
        val event =
            TaskRemovedFromSprintEvent(
                workspaceId = UUID.randomUUID(),
                sprintId = UUID.randomUUID(),
                taskId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                eventId = UUID.randomUUID(),
            )

        handler.handle(event)

        verify(exactly = 1) {
            eventPublisher.publish(
                match<AuditTaskRemovedFromSprintEvent> {
                    it.workspaceId == event.workspaceId &&
                        it.sprintId == event.sprintId &&
                        it.taskId == event.taskId &&
                        it.actorId == event.actorId &&
                        it.eventId == event.eventId
                },
            )
        }
    }
}
