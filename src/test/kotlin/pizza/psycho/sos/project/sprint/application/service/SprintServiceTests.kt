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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.common.entity.BaseEntity
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectPort
import pizza.psycho.sos.project.project.application.port.out.dto.ProjectSnapshot
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.sprint.application.policy.SprintTaskPolicy
import pizza.psycho.sos.project.sprint.application.service.dto.SprintCommand
import pizza.psycho.sos.project.sprint.application.service.dto.SprintQuery
import pizza.psycho.sos.project.sprint.application.service.dto.SprintResult
import pizza.psycho.sos.project.sprint.domain.event.SprintPeriodChangedEvent
import pizza.psycho.sos.project.sprint.domain.event.TaskRemovedFromSprintEvent
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import pizza.psycho.sos.project.task.application.port.out.TaskPort
import pizza.psycho.sos.project.task.application.port.out.dto.TaskSnapshot
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
class SprintServiceTests {
    private val sprintRepository = mockk<SprintRepository>()
    private val projectPort = mockk<ProjectPort>()
    private val taskPort = mockk<TaskPort>()
    private val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)
    private val sprintTaskPolicy = mockk<SprintTaskPolicy>(relaxed = true)
    private val sprintService = SprintService(sprintRepository, projectPort, taskPort, eventPublisher, sprintTaskPolicy)

    private val workspaceId = WorkspaceId(UUID.randomUUID())
    private val sprintId = UUID.randomUUID()
    private val startDate = Instant.parse("2026-01-01T00:00:00Z")
    private val endDate = Instant.parse("2026-01-15T00:00:00Z")

    @BeforeEach
    fun setUp() {
        mockkObject(Tx)
        every { Tx.writable(any<() -> Any>()) } answers { firstArg<() -> Any>().invoke() }
        every { Tx.readable(any<() -> Any>()) } answers { firstArg<() -> Any>().invoke() }
        every { projectPort.findActiveProjectIdsByTaskIds(any(), any()) } returns emptyList()
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
                    goal = "Goal A",
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
                SprintQuery.FindProjectsInSprint(workspaceId, sprintId),
            )

        assertTrue(result is SprintResult.Failure.IdNotFound)
    }

    @Test
    fun `프로젝트 목록 조회 시 프로젝트가 없으면 빈 리스트를 반환한다`() {
        val sprint =
            Sprint.create("Sprint A", workspaceId, "Goal A", startDate, endDate).withId(sprintId)
        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns sprint

        val result =
            sprintService.getProjectsInSprint(
                SprintQuery.FindProjectsInSprint(workspaceId, sprintId),
            )

        assertTrue(result is SprintResult.ProjectList)
        result as SprintResult.ProjectList
        assertTrue(result.projects.isEmpty())
    }

    @Test
    fun `스프린트 목록을 페이지로 반환한다`() {
        val sprint = Sprint.create("Sprint A", workspaceId, "Goal A", startDate, endDate).withId(sprintId)
        val pageable = PageRequest.of(0, 10)
        every { sprintRepository.findActiveSprints(workspaceId, pageable) } returns PageImpl(listOf(sprint))

        val result =
            sprintService.getSprints(SprintQuery.FindAll(workspaceId, pageable))

        assertTrue(result is SprintResult.SprintPage)
        result as SprintResult.SprintPage
        assertEquals(1, result.page.totalElements)
    }

    @Test
    fun `modify - 기간 입력이 없으면 changePeriod를 호출하지 않는다`() {
        val sprint =
            Sprint.create("Sprint A", workspaceId, "Goal A", startDate, endDate).withId(sprintId)
        val actorId = UUID.randomUUID()
        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns sprint

        val result =
            sprintService.modify(
                SprintCommand.Update(
                    workspaceId = workspaceId,
                    sprintId = sprintId,
                    by = actorId,
                ),
            )

        assertTrue(result is SprintResult.Success)
        assertTrue(sprint.domainEvents().none { it is SprintPeriodChangedEvent })
    }

    @Test
    fun `modify - goal을 null로 초기화할 수 있다`() {
        val sprint =
            Sprint.create("Sprint A", workspaceId, "Goal A", startDate, endDate).withId(sprintId)
        val actorId = UUID.randomUUID()
        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns sprint

        val result =
            sprintService.modify(
                SprintCommand.Update(
                    workspaceId = workspaceId,
                    sprintId = sprintId,
                    goal = Patch.Clear,
                    by = actorId,
                ),
            )

        assertTrue(result is SprintResult.Success)
        assertEquals(null, sprint.goal)
    }

    @Test
    fun `스프린트 삭제 시 프로젝트와 태스크 삭제 개수를 반환한다`() {
        val deletedBy = UUID.randomUUID()
        val projectId1 = UUID.randomUUID()
        val projectId2 = UUID.randomUUID()
        val sprint =
            Sprint.create("Sprint A", workspaceId, "Goal A", startDate, endDate).withId(sprintId).apply {
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
        every { sprintRepository.deleteById(sprintId, deletedBy, workspaceId) } returns 1

        val result =
            sprintService.remove(
                SprintCommand.Remove(workspaceId, sprintId, deletedBy),
            )

        assertTrue(result is SprintResult.Remove)
        result as SprintResult.Remove
        assertEquals(2, result.projectCount)
        assertEquals(2, result.taskCount)
        verify { taskPort.deleteByIdIn(match { it.size == 2 }, deletedBy, workspaceId) }
        verify { projectPort.deleteByIdIn(match { it.size == 2 }, deletedBy, workspaceId) }
        verify { sprintRepository.deleteById(sprintId, deletedBy, workspaceId) }
    }

    @Test
    fun `스프린트 삭제 시 외부 프로젝트와 공유된 태스크는 삭제하지 않는다`() {
        val deletedBy = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val otherProjectId = UUID.randomUUID()
        val uniqueTaskId = UUID.randomUUID()
        val sharedTaskId = UUID.randomUUID()
        val sprint =
            Sprint.create("Sprint A", workspaceId, "Goal A", startDate, endDate).withId(sprintId).apply {
                addProject(projectId)
            }
        val snapshot =
            ProjectSnapshot(
                projectId = projectId,
                workspaceId = workspaceId,
                name = "Project 1",
                taskIds = listOf(uniqueTaskId, sharedTaskId),
            )

        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns sprint
        every { projectPort.findByIdIn(listOf(projectId), workspaceId) } returns listOf(snapshot)
        every {
            projectPort.findActiveProjectIdsByTaskIds(listOf(uniqueTaskId, sharedTaskId), workspaceId)
        } returns
            listOf(
                TaskAssignment(uniqueTaskId, projectId),
                TaskAssignment(sharedTaskId, projectId),
                TaskAssignment(sharedTaskId, otherProjectId),
            )
        every { taskPort.deleteByIdIn(listOf(uniqueTaskId), deletedBy, workspaceId) } returns 1
        every { projectPort.deleteByIdIn(listOf(projectId), deletedBy, workspaceId) } returns 1
        every { sprintRepository.deleteById(sprintId, deletedBy, workspaceId) } returns 1

        val result = sprintService.remove(SprintCommand.Remove(workspaceId, sprintId, deletedBy))

        assertTrue(result is SprintResult.Remove)
        result as SprintResult.Remove
        assertEquals(1, result.projectCount)
        assertEquals(1, result.taskCount)
        verify { taskPort.deleteByIdIn(listOf(uniqueTaskId), deletedBy, workspaceId) }
    }

    @Test
    fun `스프린트 삭제 시 포함된 태스크들에 대해 제거 이벤트를 발행한다`() {
        val deletedBy = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val uniqueTaskId = UUID.randomUUID()
        val sharedTaskId = UUID.randomUUID()
        val sprint =
            Sprint.create("Sprint A", workspaceId, "Goal A", startDate, endDate).withId(sprintId).apply {
                addProject(projectId)
            }
        val snapshot =
            ProjectSnapshot(
                projectId = projectId,
                workspaceId = workspaceId,
                name = "Project 1",
                taskIds = listOf(uniqueTaskId, sharedTaskId),
            )

        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns sprint
        every { projectPort.findByIdIn(listOf(projectId), workspaceId) } returns listOf(snapshot)
        every { taskPort.deleteByIdIn(any(), deletedBy, workspaceId) } returns 1
        every { projectPort.deleteByIdIn(listOf(projectId), deletedBy, workspaceId) } returns 1
        every { sprintRepository.deleteById(sprintId, deletedBy, workspaceId) } returns 1

        val result = sprintService.remove(SprintCommand.Remove(workspaceId, sprintId, deletedBy))

        assertTrue(result is SprintResult.Remove)
        verify(exactly = 1) {
            eventPublisher.publish(
                match<TaskRemovedFromSprintEvent> {
                    it.sprintId == sprintId && it.taskId == uniqueTaskId && it.actorId == deletedBy
                },
            )
        }
        verify(exactly = 1) {
            eventPublisher.publish(
                match<TaskRemovedFromSprintEvent> {
                    it.sprintId == sprintId && it.taskId == sharedTaskId && it.actorId == deletedBy
                },
            )
        }
    }

    @Test
    fun `remove는 스프린트가 없으면 IdNotFound를 반환한다`() {
        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns null

        val result =
            sprintService.remove(
                SprintCommand.Remove(workspaceId, sprintId, UUID.randomUUID()),
            )

        assertTrue(result is SprintResult.Failure.IdNotFound)
    }

    @Test
    fun `modify - 프로젝트 제거 시 backlog 대상 task 를 식별해 이동시킨다`() {
        val actorId = UUID.randomUUID()
        val removedProjectId = UUID.randomUUID()
        val keptProjectId = UUID.randomUUID()
        val sharedTaskId = UUID.randomUUID()
        val backlogTaskId = UUID.randomUUID()
        val sprint =
            Sprint.create("Sprint A", workspaceId, "Goal A", startDate, endDate).withId(sprintId).apply {
                addProjects(listOf(removedProjectId, keptProjectId))
            }
        val removedProject =
            ProjectSnapshot(
                projectId = removedProjectId,
                workspaceId = workspaceId,
                name = "Removed",
                taskIds = listOf(sharedTaskId, backlogTaskId),
            )
        val keptProject =
            ProjectSnapshot(
                projectId = keptProjectId,
                workspaceId = workspaceId,
                name = "Kept",
                taskIds = listOf(sharedTaskId),
            )

        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns sprint
        every { projectPort.findByIdIn(listOf(removedProjectId, keptProjectId), workspaceId) } returns listOf(removedProject, keptProject)
        every { sprintTaskPolicy.tasksMovingToBacklog(listOf(removedProject), listOf(keptProject)) } returns setOf(backlogTaskId)
        every { taskPort.moveToBacklog(setOf(backlogTaskId), actorId, workspaceId) } returns Unit

        val result =
            sprintService.modify(
                SprintCommand.Update(
                    workspaceId = workspaceId,
                    sprintId = sprintId,
                    removeProjectIds = listOf(removedProjectId),
                    by = actorId,
                ),
            )

        assertTrue(result is SprintResult.Success)
        verify { taskPort.moveToBacklog(setOf(backlogTaskId), actorId, workspaceId) }
    }

    @Test
    fun `modify - 프로젝트 추가 시 새로 sprint 에 들어오는 task 에 대해 이벤트를 등록한다`() {
        val actorId = UUID.randomUUID()
        val keptProjectId = UUID.randomUUID()
        val addedProjectId = UUID.randomUUID()
        val existingTaskId = UUID.randomUUID()
        val addedTaskId = UUID.randomUUID()
        val sprint =
            Sprint.create("Sprint A", workspaceId, "Goal A", startDate, endDate).withId(sprintId).apply {
                addProjects(listOf(keptProjectId))
            }
        val keptProject =
            ProjectSnapshot(
                projectId = keptProjectId,
                workspaceId = workspaceId,
                name = "Kept",
                taskIds = listOf(existingTaskId),
            )
        val addedProject =
            ProjectSnapshot(
                projectId = addedProjectId,
                workspaceId = workspaceId,
                name = "Added",
                taskIds = listOf(existingTaskId, addedTaskId),
            )

        every { sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) } returns sprint
        every { projectPort.findByIdIn(listOf(keptProjectId), workspaceId) } returns listOf(keptProject)
        every { projectPort.findByIdIn(listOf(addedProjectId), workspaceId) } returns listOf(addedProject)
        every { sprintTaskPolicy.tasksEnteringSprint(listOf(keptProject), listOf(addedProject)) } returns setOf(addedTaskId)
        every { taskPort.findByIdIn(listOf(addedTaskId), workspaceId) } returns
            listOf(
                TaskSnapshot(
                    id = addedTaskId,
                    title = "Task",
                    status = pizza.psycho.sos.project.task.domain.model.vo.Status.TODO,
                    dueDate = startDate.plusSeconds(3600),
                ),
            )

        val result =
            sprintService.modify(
                SprintCommand.Update(
                    workspaceId = workspaceId,
                    sprintId = sprintId,
                    addProjectIds = listOf(addedProjectId),
                    by = actorId,
                ),
            )

        assertTrue(result is SprintResult.Success)
        assertTrue(sprint.domainEvents().any { it is pizza.psycho.sos.project.sprint.domain.event.TaskAddedToSprintEvent })
    }
}

private fun Sprint.withId(id: UUID): Sprint =
    apply {
        val field = BaseEntity::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(this, id)
    }
