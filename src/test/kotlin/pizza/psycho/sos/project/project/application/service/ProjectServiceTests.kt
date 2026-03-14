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
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectRepository
import pizza.psycho.sos.project.project.application.port.out.ProjectSprintParticipationQuery
import pizza.psycho.sos.project.project.application.service.dto.ProjectCommand
import pizza.psycho.sos.project.project.application.service.dto.ProjectResult
import pizza.psycho.sos.project.project.domain.model.entity.Project
import pizza.psycho.sos.project.sprint.domain.policy.SprintTaskPeriodPolicy
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
    private val projectSprintParticipationQuery = mockk<ProjectSprintParticipationQuery>()
    private val sprintTaskPeriodPolicy = mockk<SprintTaskPeriodPolicy>()
    private val projectService =
        ProjectService(
            projectRepository,
            eventPublisher,
            taskPort,
            projectSprintParticipationQuery,
            sprintTaskPeriodPolicy,
        )

    @BeforeEach
    fun setUp() {
        mockkObject(Tx)
        every { Tx.writable(any<() -> Any>()) } answers { firstArg<() -> Any>().invoke() }
        every { Tx.readable(any<() -> Any>()) } answers { firstArg<() -> Any>().invoke() }
        justRun { eventPublisher.publishAndClear(any()) }
        justRun { eventPublisher.publishAndClearAll(any()) }
        every {
            projectSprintParticipationQuery.findActiveSprintPeriodsByProjectId(any(), any())
        } returns emptyList()
        every {
            sprintTaskPeriodPolicy.isTaskDueDateWithinSprint(any(), any())
        } returns true
        justRun { eventPublisher.publishAndClear(any()) }
        justRun { eventPublisher.publishAndClearAll(any()) }
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
        every { taskPort.deleteByIdIn(listOf(taskId1, taskId2), deletedBy, workspaceId) } returns 2

        val command = ProjectCommand.RemoveWithTasks(workspaceId, projectId, deletedBy)

        val result = projectService.removeWithTasks(command)

        assertTrue(result is ProjectResult.RemoveWithTasks)
        result as ProjectResult.RemoveWithTasks
        assertEquals(1, result.projectCount)
        assertEquals(2, result.taskCount)

        verify { taskPort.deleteByIdIn(match { it.containsAll(listOf(taskId1, taskId2)) }, deletedBy, workspaceId) }
    }

    @Test
    fun `존재하지 않는 프로젝트 삭제 시 IdNotFound를 반환한다`() {
        val workspaceId = WorkspaceId(UUID.randomUUID())
        val command = ProjectCommand.RemoveWithTasks(workspaceId, UUID.randomUUID(), UUID.randomUUID())

        every { projectRepository.findActiveProjectByIdOrNull(command.projectId, workspaceId) } returns null

        val result = projectService.removeWithTasks(command)

        assertTrue(result is ProjectResult.Failure.IdNotFound)
    }
}
