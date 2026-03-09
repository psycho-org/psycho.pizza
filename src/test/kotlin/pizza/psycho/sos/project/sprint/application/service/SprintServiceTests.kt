package pizza.psycho.sos.project.sprint.application.service

import io.mockk.every
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
import pizza.psycho.sos.common.entity.BaseEntity
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectPort
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.sprint.application.service.dto.SprintCommand
import pizza.psycho.sos.project.sprint.application.service.dto.SprintResult
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
class SprintServiceTests {
    private val sprintRepository = mockk<SprintRepository>()
    private val projectPort = mockk<ProjectPort>()
    private val taskPort = mockk<TaskPort>()
    private val sprintService = SprintService(sprintRepository, projectPort, taskPort)

    private val workspaceId = WorkspaceId(UUID.randomUUID())
    private val sprintId = UUID.randomUUID()
    private val startDate = Instant.parse("2026-01-01T00:00:00Z")
    private val endDate = Instant.parse("2026-01-15T00:00:00Z")

    @BeforeEach
    fun setUp() {
        mockkObject(Tx)
        every { Tx.writable(any<() -> Any>()) } answers { firstArg<() -> Any>().invoke() }
        every { Tx.readable(any<() -> Any>()) } answers { firstArg<() -> Any>().invoke() }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `스프린트에 프로젝트 생성 시 ProjectCreated를 반환한다`() {
        val sprint =
            Sprint
                .create(
                    name = "Sprint A",
                    workspaceId = workspaceId,
                    startDate = startDate,
                    endDate = endDate,
                ).withId(sprintId)
        val snapshot =
            ProjectSnapshot(
                projectId = UUID.randomUUID(),
                workspaceId = workspaceId,
                name = "새 프로젝트",
                taskIds = emptyList(),
            )

        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns sprint
        every { projectPort.createProject(workspaceId, "새 프로젝트") } returns snapshot

        val result =
            sprintService.createProject(
                SprintCommand.CreateProject(workspaceId, sprintId, "새 프로젝트"),
            )

        assertTrue(result is SprintResult.ProjectCreated)
        result as SprintResult.ProjectCreated
        assertEquals(snapshot.projectId, result.project.projectId)
        verify { projectPort.createProject(workspaceId, "새 프로젝트") }
    }

    @Test
    fun `스프린트가 없으면 프로젝트 생성 시 IdNotFound를 반환한다`() {
        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns null

        val result =
            sprintService.createProject(
                SprintCommand.CreateProject(workspaceId, sprintId, "새 프로젝트"),
            )

        assertTrue(result is SprintResult.Failure.IdNotFound)
    }

    @Test
    fun `프로젝트 목록 조회 시 스프린트가 없으면 IdNotFound를 반환한다`() {
        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns null

        val result =
            sprintService.getProjectsInSprint(
                SprintCommand.GetProjects(workspaceId, sprintId),
            )

        assertTrue(result is SprintResult.Failure.IdNotFound)
    }

    @Test
    fun `프로젝트 목록 조회 시 프로젝트가 없으면 빈 리스트를 반환한다`() {
        val sprint =
            Sprint.create("Sprint A", workspaceId, startDate, endDate).withId(sprintId)
        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns sprint

        val result =
            sprintService.getProjectsInSprint(
                SprintCommand.GetProjects(workspaceId, sprintId),
            )

        assertTrue(result is SprintResult.ProjectList)
        result as SprintResult.ProjectList
        assertTrue(result.projects.isEmpty())
    }

    @Test
    fun `스프린트 삭제 시 프로젝트와 태스크 삭제 개수를 반환한다`() {
        val deletedBy = UUID.randomUUID()
        val projectId1 = UUID.randomUUID()
        val projectId2 = UUID.randomUUID()
        val sprint =
            Sprint.create("Sprint A", workspaceId, startDate, endDate).withId(sprintId).apply {
                addProjects(listOf(projectId1, projectId2))
            }
        val snapshot1 =
            ProjectSnapshot(
                projectId = projectId1,
                workspaceId = workspaceId,
                name = "Project 1",
                taskIds = listOf(UUID.randomUUID()),
            )
        val snapshot2 =
            ProjectSnapshot(
                projectId = projectId2,
                workspaceId = workspaceId,
                name = "Project 2",
                taskIds = listOf(UUID.randomUUID()),
            )

        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns sprint
        every { projectPort.findByIdIn(listOf(projectId1, projectId2), workspaceId) } returns listOf(snapshot1, snapshot2)
        every { taskPort.deleteByIdIn(any(), deletedBy, workspaceId) } returns 2
        every { projectPort.deleteByIdIn(any(), deletedBy, workspaceId) } returns 2

        val result =
            sprintService.removeWithProjects(
                SprintCommand.RemoveWithProjects(workspaceId, sprintId, deletedBy),
            )

        assertTrue(result is SprintResult.RemoveWithProjects)
        result as SprintResult.RemoveWithProjects
        assertEquals(2, result.projectCount)
        assertEquals(2, result.taskCount)
        verify { taskPort.deleteByIdIn(match { it.size == 2 }, deletedBy, workspaceId) }
        verify { projectPort.deleteByIdIn(match { it.size == 2 }, deletedBy, workspaceId) }
    }

    @Test
    fun `removeWithProjects는 스프린트가 없으면 IdNotFound를 반환한다`() {
        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns null

        val result =
            sprintService.removeWithProjects(
                SprintCommand.RemoveWithProjects(workspaceId, sprintId, UUID.randomUUID()),
            )

        assertTrue(result is SprintResult.Failure.IdNotFound)
    }
}

private fun Sprint.withId(id: UUID): Sprint =
    apply {
        val field = BaseEntity::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(this, id)
    }
