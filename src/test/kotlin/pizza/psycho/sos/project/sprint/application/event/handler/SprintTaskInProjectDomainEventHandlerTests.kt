package pizza.psycho.sos.project.sprint.application.event.handler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.domain.event.TaskAddedToProjectEvent
import pizza.psycho.sos.project.project.domain.event.TaskRemovedFromProjectEvent
import pizza.psycho.sos.project.project.domain.event.TasksAddedToProjectEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskAddedToSprintEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import java.util.UUID

class SprintTaskInProjectDomainEventHandlerTests {
    private val sprintRepository = mockk<SprintRepository>()
    private val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)
    private val handler = SprintTaskInProjectDomainEventHandler(sprintRepository, eventPublisher)

    @Test
    fun `task move inside same sprint does not emit sprint add or remove`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val fromProjectId = UUID.randomUUID()
        val toProjectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val actorId = UUID.randomUUID()

        every {
            sprintRepository.findActiveSprintIdsByProjectId(fromProjectId, WorkspaceId(workspaceId))
        } returns listOf(sprintId)
        every {
            sprintRepository.findActiveSprintIdsByProjectId(toProjectId, WorkspaceId(workspaceId))
        } returns listOf(sprintId)
        every {
            sprintRepository.findActiveSprintIdsByTaskIds(listOf(taskId), WorkspaceId(workspaceId))
        } returns mapOf(taskId to setOf(sprintId))

        handler.handle(
            TaskRemovedFromProjectEvent(
                workspaceId = workspaceId,
                actorId = actorId,
                taskId = taskId,
                projectId = fromProjectId,
                eventId = UUID.randomUUID(),
            ),
        )

        handler.handle(
            TaskAddedToProjectEvent(
                workspaceId = workspaceId,
                actorId = actorId,
                taskId = taskId,
                projectId = toProjectId,
                eventId = UUID.randomUUID(),
            ),
        )

        verify(exactly = 0) { eventPublisher.publish(any<TaskRemovedFromSprintEvent>()) }
        verify(exactly = 0) { eventPublisher.publish(any<TaskAddedToSprintEvent>()) }
    }

    @Test
    fun `task move to different sprint emits removal and addition`() {
        val workspaceId = UUID.randomUUID()
        val sprintFrom = UUID.randomUUID()
        val sprintTo = UUID.randomUUID()
        val fromProjectId = UUID.randomUUID()
        val toProjectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val actorId = UUID.randomUUID()

        every {
            sprintRepository.findActiveSprintIdsByProjectId(fromProjectId, WorkspaceId(workspaceId))
        } returns listOf(sprintFrom)
        every {
            sprintRepository.findActiveSprintIdsByProjectId(toProjectId, WorkspaceId(workspaceId))
        } returns listOf(sprintTo)
        every {
            sprintRepository.findActiveSprintIdsByTaskIds(listOf(taskId), WorkspaceId(workspaceId))
        } returns emptyMap()

        handler.handle(
            TaskRemovedFromProjectEvent(
                workspaceId = workspaceId,
                actorId = actorId,
                taskId = taskId,
                projectId = fromProjectId,
                eventId = UUID.randomUUID(),
            ),
        )

        handler.handle(
            TaskAddedToProjectEvent(
                workspaceId = workspaceId,
                actorId = actorId,
                taskId = taskId,
                projectId = toProjectId,
                eventId = UUID.randomUUID(),
            ),
        )

        verify(exactly = 1) {
            eventPublisher.publish(
                match<TaskRemovedFromSprintEvent> {
                    it.sprintId == sprintFrom && it.taskId == taskId
                },
            )
        }
        verify(exactly = 1) {
            eventPublisher.publish(
                match<TaskAddedToSprintEvent> {
                    it.sprintId == sprintTo && it.taskId == taskId
                },
            )
        }
    }

    @Test
    fun `duplicate task added events only emit sprint add once`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()

        every {
            sprintRepository.findActiveSprintIdsByProjectId(projectId, WorkspaceId(workspaceId))
        } returns listOf(sprintId)
        val event =
            TaskAddedToProjectEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                taskId = taskId,
                projectId = projectId,
                eventId = UUID.randomUUID(),
            )

        handler.handle(event)
        handler.handle(event.copy(eventId = UUID.randomUUID()))
        verify(exactly = 1) { eventPublisher.publish(any<TaskAddedToSprintEvent>()) }
    }

    @Test
    fun `bulk task added event queries sprint membership once per project`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val taskId1 = UUID.randomUUID()
        val taskId2 = UUID.randomUUID()

        every {
            sprintRepository.findActiveSprintIdsByProjectId(projectId, WorkspaceId(workspaceId))
        } returns listOf(sprintId)

        handler.handle(
            TasksAddedToProjectEvent(
                workspaceId = workspaceId,
                actorId = UUID.randomUUID(),
                taskIds = listOf(taskId1, taskId2),
                projectId = projectId,
                eventId = UUID.randomUUID(),
            ),
        )

        verify(exactly = 1) {
            sprintRepository.findActiveSprintIdsByProjectId(projectId, WorkspaceId(workspaceId))
        }
        verify(exactly = 2) { eventPublisher.publish(any<TaskAddedToSprintEvent>()) }
    }

    @Test
    fun `project detached before task removal clears dedupe so re-add emits again`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val actorId = UUID.randomUUID()

        every {
            sprintRepository.findActiveSprintIdsByProjectId(projectId, WorkspaceId(workspaceId))
        } returnsMany
            listOf(
                listOf(sprintId),
                emptyList(),
                listOf(sprintId),
            )

        val addedEvent =
            TaskAddedToProjectEvent(
                workspaceId = workspaceId,
                actorId = actorId,
                taskId = taskId,
                projectId = projectId,
                eventId = UUID.randomUUID(),
            )

        handler.handle(addedEvent)
        handler.handle(
            TaskRemovedFromProjectEvent(
                workspaceId = workspaceId,
                actorId = actorId,
                taskId = taskId,
                projectId = projectId,
                eventId = UUID.randomUUID(),
            ),
        )
        handler.handle(addedEvent.copy(eventId = UUID.randomUUID()))

        verify(exactly = 2) { eventPublisher.publish(any<TaskAddedToSprintEvent>()) }
        verify(exactly = 0) {
            eventPublisher.publish(
                match { it is TaskRemovedFromSprintEvent },
            )
        }
    }

    @Test
    fun `task removed from sprint clears dedupe so re-entry emits add again`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val actorId = UUID.randomUUID()

        every {
            sprintRepository.findActiveSprintIdsByProjectId(projectId, WorkspaceId(workspaceId))
        } returns listOf(sprintId)

        val addedEvent =
            TaskAddedToProjectEvent(
                workspaceId = workspaceId,
                actorId = actorId,
                taskId = taskId,
                projectId = projectId,
                eventId = UUID.randomUUID(),
            )

        handler.handle(addedEvent)
        handler.handle(
            TaskRemovedFromSprintEvent(
                workspaceId = workspaceId,
                sprintId = sprintId,
                taskId = taskId,
                actorId = actorId,
                eventId = UUID.randomUUID(),
            ),
        )
        handler.handle(addedEvent.copy(eventId = UUID.randomUUID()))

        verify(exactly = 2) { eventPublisher.publish(any<TaskAddedToSprintEvent>()) }
    }
}
