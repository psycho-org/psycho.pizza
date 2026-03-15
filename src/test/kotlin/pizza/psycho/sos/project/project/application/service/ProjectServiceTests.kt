package pizza.psycho.sos.project.project.application.service

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectRepository
import pizza.psycho.sos.project.project.application.port.out.ProjectSprintParticipationQuery
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.project.application.service.dto.ProjectCommand
import pizza.psycho.sos.project.project.application.service.dto.ProjectQuery
import pizza.psycho.sos.project.project.application.service.dto.ProjectResult
import pizza.psycho.sos.project.project.domain.model.entity.Project
import pizza.psycho.sos.project.sprint.application.policy.SprintTaskPolicy
import pizza.psycho.sos.project.sprint.domain.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
class ProjectServiceTests {
    private val projectRepository = mockk<ProjectRepository>()
    private val taskPort = mockk<TaskPort>()
    private val eventPublisher = mockk<DomainEventPublisher>()
    private val sprintTaskPolicy = mockk<SprintTaskPolicy>(relaxed = true)
    private val sprintParticipationQuery = mockk<ProjectSprintParticipationQuery>()
    private val projectService =
        ProjectService(projectRepository, eventPublisher, taskPort, sprintTaskPolicy, sprintParticipationQuery)

    @BeforeEach
    fun setUp() {
        mockkObject(Tx)
        every { Tx.writable(any<() -> Any>()) } answers { firstArg<() -> Any>().invoke() }
        every { Tx.readable(any<() -> Any>()) } answers { firstArg<() -> Any>().invoke() }
        justRun { eventPublisher.publishAndClear(any()) }
        justRun { eventPublisher.publishAndClearAll(any()) }
        justRun { eventPublisher.publishAndClear(any()) }
        justRun { eventPublisher.publishAndClearAll(any()) }
        justRun { eventPublisher.publish(any<TaskRemovedFromSprintEvent>()) }
        every { projectRepository.findActiveProjectIdsByTaskIds(any(), any()) } returns emptyList()
        every { projectRepository.findActiveTaskIdsByProjectId(any(), any()) } returns emptyList()
        every { projectRepository.findActiveTaskIdsByProjectId(any(), any(), any()) } returns PageImpl(emptyList())
        every { sprintParticipationQuery.findActiveSprintIdsByProjectId(any(), any()) } returns emptyList()
        every { sprintParticipationQuery.findActiveSprintIdsByProjectIds(any(), any()) } returns emptyMap()
        every { taskPort.findByIdIn(any<Collection<UUID>>(), any()) } answers {
            firstArg<Collection<UUID>>().map { id ->
                TaskSnapshot(
                    id = id,
                    title = "task-$id",
                    status = Status.TODO,
                    assigneeId = null,
                    dueDate = null,
                )
            }
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `프로젝트에 태스크 생성 시 Task 정보를 반환한다`() {
        val workspaceId = WorkspaceId(UUID.randomUUID())
        val projectId = UUID.randomUUID()
        val command =
            ProjectCommand.CreateTask(
                workspaceId = workspaceId,
                projectId = projectId,
                title = "새 태스크",
                description = "태스크 설명",
                assigneeId = UUID.randomUUID(),
                dueDate = Instant.parse("2026-12-01T00:00:00Z"),
            )

        val project =
            Project
                .create(
                    workspaceId = workspaceId,
                    name = "프로젝트",
                ).apply { id = projectId }

        val taskSnapshot =
            TaskSnapshot(
                id = UUID.randomUUID(),
                title = command.title,
                status = Status.TODO,
                assigneeId = command.assigneeId,
                dueDate = command.dueDate,
            )

        every { projectRepository.findActiveProjectByIdOrNull(projectId, workspaceId) } returns project
        every {
            taskPort.createTask(
                workspaceId = workspaceId.value,
                title = command.title,
                description = command.description,
                assigneeId = command.assigneeId,
                dueDate = command.dueDate,
            )
        } returns taskSnapshot

        val result = projectService.createTask(command)

        assertTrue(result is ProjectResult.Task)
        result as ProjectResult.Task
        assertEquals(taskSnapshot.id, result.id)
        assertEquals(taskSnapshot.title, result.title)
        verify {
            taskPort.createTask(
                workspaceId = workspaceId.value,
                title = command.title,
                description = command.description,
                assigneeId = command.assigneeId,
                dueDate = command.dueDate,
            )
        }
    }

    @Test
    fun `프로젝트 태스크 조회 시 활성 태스크 ID만 사용한다`() {
        val workspaceId = WorkspaceId(UUID.randomUUID())
        val projectId = UUID.randomUUID()
        val activeTaskId = UUID.randomUUID()
        val deletedTaskId = UUID.randomUUID()
        val pageable = PageRequest.of(0, 10)
        val command = ProjectQuery.FindTasksInProject(workspaceId, projectId, pageable)
        val project =
            Project
                .create(
                    workspaceId = workspaceId,
                    name = "프로젝트",
                ).apply {
                    id = projectId
                    addTask(activeTaskId)
                    addTask(deletedTaskId)
                }
        val taskSnapshot =
            TaskSnapshot(
                id = activeTaskId,
                title = "활성 태스크",
                status = Status.TODO,
                assigneeId = null,
                dueDate = null,
            )

        every { projectRepository.findActiveProjectByIdOrNull(projectId, workspaceId) } returns project
        every {
            projectRepository.findActiveTaskIdsByProjectId(
                projectId = projectId,
                workspaceId = workspaceId,
                pageable = pageable,
            )
        } returns PageImpl(listOf(activeTaskId), pageable, 1)
        every { taskPort.findByIdIn(listOf(activeTaskId), workspaceId) } returns listOf(taskSnapshot)

        val result = projectService.getTasksInProject(command)

        assertTrue(result is ProjectResult.TaskList)
        verify(exactly = 1) {
            projectRepository.findActiveTaskIdsByProjectId(
                projectId = projectId,
                workspaceId = workspaceId,
                pageable = pageable,
            )
        }
        verify(exactly = 1) { taskPort.findByIdIn(listOf(activeTaskId), workspaceId) }
    }

    @Test
    fun `프로젝트가 없으면 태스크 생성 시 IdNotFound를 반환한다`() {
        val workspaceId = WorkspaceId(UUID.randomUUID())
        val command =
            ProjectCommand.CreateTask(
                workspaceId = workspaceId,
                projectId = UUID.randomUUID(),
                title = "새 태스크",
                description = "설명",
            )

        every { projectRepository.findActiveProjectByIdOrNull(command.projectId, workspaceId) } returns null

        val result = projectService.createTask(command)

        assertTrue(result is ProjectResult.Failure.IdNotFound)
    }

    @Test
    fun `프로젝트와 하위 태스크 삭제 시 삭제된 개수를 반환한다`() {
        val workspaceId = WorkspaceId(UUID.randomUUID())
        val projectId = UUID.randomUUID()
        val deletedBy = UUID.randomUUID()
        val taskId1 = UUID.randomUUID()
        val taskId2 = UUID.randomUUID()

        val project =
            Project
                .create(
                    workspaceId = workspaceId,
                    name = "삭제 대상 프로젝트",
                ).apply {
                    id = projectId
                    addTask(taskId1)
                    addTask(taskId2)
                }

        every { projectRepository.findActiveProjectByIdOrNull(projectId, workspaceId) } returns project
        every { projectRepository.findActiveTaskIdsByProjectId(projectId, workspaceId) } returns listOf(taskId1, taskId2)
        every { taskPort.deleteByIdIn(listOf(taskId1, taskId2), deletedBy, workspaceId) } returns 2

        val command = ProjectCommand.Remove(workspaceId, projectId, deletedBy)

        val result = projectService.remove(command)

        assertTrue(result is ProjectResult.Remove)
        result as ProjectResult.Remove
        assertEquals(1, result.projectCount)
        assertEquals(2, result.taskCount)

        verify { taskPort.deleteByIdIn(match { it.containsAll(listOf(taskId1, taskId2)) }, deletedBy, workspaceId) }
    }

    @Test
    fun `프로젝트 삭제 시 다른 활성 프로젝트에 매핑된 태스크는 삭제하지 않는다`() {
        val workspaceId = WorkspaceId(UUID.randomUUID())
        val projectId = UUID.randomUUID()
        val otherProjectId = UUID.randomUUID()
        val deletedBy = UUID.randomUUID()
        val uniqueTaskId = UUID.randomUUID()
        val sharedTaskId = UUID.randomUUID()

        val project =
            Project
                .create(
                    workspaceId = workspaceId,
                    name = "삭제 대상 프로젝트",
                ).apply {
                    id = projectId
                    addTask(uniqueTaskId)
                    addTask(sharedTaskId)
                }

        every { projectRepository.findActiveProjectByIdOrNull(projectId, workspaceId) } returns project
        every { projectRepository.findActiveTaskIdsByProjectId(projectId, workspaceId) } returns listOf(uniqueTaskId, sharedTaskId)
        every {
            projectRepository.findActiveProjectIdsByTaskIds(listOf(uniqueTaskId, sharedTaskId), workspaceId)
        } returns
            listOf(
                TaskAssignment(uniqueTaskId, projectId),
                TaskAssignment(sharedTaskId, projectId),
                TaskAssignment(sharedTaskId, otherProjectId),
            )
        every { taskPort.deleteByIdIn(listOf(uniqueTaskId), deletedBy, workspaceId) } returns 1

        val result = projectService.remove(ProjectCommand.Remove(workspaceId, projectId, deletedBy))

        assertTrue(result is ProjectResult.Remove)
        result as ProjectResult.Remove
        assertEquals(1, result.taskCount)
        verify { taskPort.deleteByIdIn(listOf(uniqueTaskId), deletedBy, workspaceId) }
        verify(exactly = 1) {
            projectRepository.findActiveProjectIdsByTaskIds(listOf(uniqueTaskId, sharedTaskId), workspaceId)
        }
    }

    @Test
    fun `프로젝트 삭제 시 살아남는 태스크가 떠나는 sprint 에 대해 제거 이벤트를 발행한다`() {
        val workspaceId = WorkspaceId(UUID.randomUUID())
        val projectId = UUID.randomUUID()
        val otherProjectId = UUID.randomUUID()
        val deletedBy = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val otherSprintId = UUID.randomUUID()
        val sharedTaskId = UUID.randomUUID()

        val project =
            Project
                .create(
                    workspaceId = workspaceId,
                    name = "삭제 대상 프로젝트",
                ).apply {
                    id = projectId
                    addTask(sharedTaskId)
                }

        every { projectRepository.findActiveProjectByIdOrNull(projectId, workspaceId) } returns project
        every { projectRepository.findActiveTaskIdsByProjectId(projectId, workspaceId) } returns listOf(sharedTaskId)
        every {
            projectRepository.findActiveProjectIdsByTaskIds(listOf(sharedTaskId), workspaceId)
        } returns
            listOf(
                TaskAssignment(sharedTaskId, projectId),
                TaskAssignment(sharedTaskId, otherProjectId),
            )
        every { sprintParticipationQuery.findActiveSprintIdsByProjectId(projectId, workspaceId.value) } returns listOf(sprintId)
        every {
            sprintParticipationQuery.findActiveSprintIdsByProjectIds(setOf(otherProjectId), workspaceId.value)
        } returns mapOf(otherProjectId to setOf(otherSprintId))

        val result = projectService.remove(ProjectCommand.Remove(workspaceId, projectId, deletedBy))

        assertTrue(result is ProjectResult.Remove)
        verify(exactly = 1) {
            eventPublisher.publish(
                match<TaskRemovedFromSprintEvent> {
                    it.workspaceId == workspaceId.value &&
                        it.sprintId == sprintId &&
                        it.taskId == sharedTaskId &&
                        it.actorId == deletedBy
                },
            )
        }
        verify(exactly = 0) { sprintParticipationQuery.findActiveSprintIdsByProjectId(otherProjectId, workspaceId.value) }
    }

    @Test
    fun `프로젝트 삭제 시 이미 삭제된 태스크에 대해서는 sprint 제거 이벤트를 발행하지 않는다`() {
        val workspaceId = WorkspaceId(UUID.randomUUID())
        val projectId = UUID.randomUUID()
        val deletedBy = UUID.randomUUID()
        val deletedTaskId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()

        val project =
            Project
                .create(
                    workspaceId = workspaceId,
                    name = "삭제 대상 프로젝트",
                ).apply {
                    id = projectId
                    addTask(deletedTaskId)
                }

        every { projectRepository.findActiveProjectByIdOrNull(projectId, workspaceId) } returns project
        every { projectRepository.findActiveTaskIdsByProjectId(projectId, workspaceId) } returns emptyList()
        every { sprintParticipationQuery.findActiveSprintIdsByProjectId(projectId, workspaceId.value) } returns listOf(sprintId)

        val result = projectService.remove(ProjectCommand.Remove(workspaceId, projectId, deletedBy))

        assertTrue(result is ProjectResult.Remove)
        verify(exactly = 0) { eventPublisher.publish(any<TaskRemovedFromSprintEvent>()) }
        verify(exactly = 0) { taskPort.deleteByIdIn(any(), any(), any()) }
    }

    @Test
    fun `존재하지 않는 프로젝트 삭제 시 IdNotFound를 반환한다`() {
        val workspaceId = WorkspaceId(UUID.randomUUID())
        val command = ProjectCommand.Remove(workspaceId, UUID.randomUUID(), UUID.randomUUID())

        every { projectRepository.findActiveProjectByIdOrNull(command.projectId, workspaceId) } returns null

        val result = projectService.remove(command)

        assertTrue(result is ProjectResult.Failure.IdNotFound)
    }

    @Test
    fun `다른 프로젝트에 속한 태스크를 추가하려 하면 TaskAlreadyAssigned를 반환한다`() {
        val workspaceId = WorkspaceId(UUID.randomUUID())
        val projectId = UUID.randomUUID()
        val otherProjectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val project = Project.create(workspaceId = workspaceId, name = "프로젝트").apply { id = projectId }

        every { projectRepository.findActiveProjectByIdOrNull(projectId, workspaceId) } returns project
        every { taskPort.findByIdIn(listOf(taskId), workspaceId) } returns
            listOf(
                TaskSnapshot(
                    id = taskId,
                    title = "태스크",
                    status = Status.TODO,
                ),
            )
        every {
            projectRepository.findActiveProjectIdsByTaskIds(listOf(taskId), workspaceId)
        } returns listOf(TaskAssignment(taskId, otherProjectId))

        val result =
            projectService.modify(
                ProjectCommand.Update(
                    workspaceId = workspaceId,
                    projectId = projectId,
                    addTaskIds = listOf(taskId),
                ),
            )

        assertTrue(result is ProjectResult.Failure.TaskAlreadyAssigned)
        verify(exactly = 0) { eventPublisher.publishAndClear(any()) }
    }
}
