package pizza.psycho.sos.project.task.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
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
import pizza.psycho.sos.project.sprint.application.policy.SprintTaskPolicy
import pizza.psycho.sos.project.sprint.domain.policy.SprintTaskPeriodPolicy
import pizza.psycho.sos.project.task.application.port.out.TaskSprintParticipationQuery
import pizza.psycho.sos.project.task.application.service.dto.TaskCommand
import pizza.psycho.sos.project.task.application.service.dto.TaskQuery
import pizza.psycho.sos.project.task.application.service.dto.TaskResult
import pizza.psycho.sos.project.task.domain.model.entity.Task
import pizza.psycho.sos.project.task.domain.repository.TaskRepository
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
class TaskServiceTests {
    private val taskRepository = mockk<TaskRepository>()
    private val domainEventPublisher = mockk<DomainEventPublisher>()
    private val sprintTaskPolicy = mockk<SprintTaskPolicy>(relaxed = true)
    private val taskSprintParticipationQuery = mockk<TaskSprintParticipationQuery>()
    private val sprintTaskPeriodPolicy = mockk<SprintTaskPeriodPolicy>()
    private val taskService =
        TaskService(
            taskRepository,
            domainEventPublisher,
            sprintTaskPolicy,
            taskSprintParticipationQuery,
            sprintTaskPeriodPolicy,
        )

    @BeforeEach
    fun setup() {
        // Tx object를 모킹
        mockkObject(Tx)
        // Tx.writable을 모킹: 람다 함수를 받아서 바로 실행
        every { Tx.writable(any<() -> Any>()) } answers {
            val lambda = firstArg<() -> Any>()
            lambda()
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `태스크 생성 시 정상적으로 저장되고 TaskInformation을 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val assigneeId = UUID.randomUUID()
        val command =
            TaskCommand.AddTask(
                workspaceId = workspaceId,
                title = "새로운 태스크",
                description = "태스크 설명",
                assigneeId = assigneeId,
                dueDate = Instant.now().plusSeconds(3600), // 1시간 후
            )

        val taskId = UUID.randomUUID()
        val taskSlot = slot<Task>()

        every { taskRepository.save(capture(taskSlot)) } answers {
            taskSlot.captured.apply { id = taskId }
        }

        val result = taskService.create(command)

        assertTrue(result is TaskResult.TaskInformation)
        val taskInfo = result as TaskResult.TaskInformation
        assertEquals(taskId, taskInfo.id)
        assertEquals(command.title, taskInfo.title)
        assertEquals(command.description, taskInfo.description)

        verify { taskRepository.save(any()) }
    }

    @Test
    fun `워크스페이스의 모든 태스크를 페이지네이션으로 조회한다`() {
        val workspaceId = UUID.randomUUID()
        val pageable = PageRequest.of(0, 10)
        val command = TaskQuery.FindTasks(workspaceId, pageable)

        val task1 =
            Task
                .create(
                    title = "태스크 1",
                    description = "설명 1",
                    assigneeId = null,
                    workspaceId = workspaceId,
                ).apply { id = UUID.randomUUID() }

        val task2 =
            Task
                .create(
                    title = "태스크 2",
                    description = "설명 2",
                    assigneeId = null,
                    workspaceId = workspaceId,
                ).apply { id = UUID.randomUUID() }

        val page = PageImpl(listOf(task1, task2), pageable, 2)

        every { taskRepository.findAllActiveTasks(WorkspaceId(workspaceId), pageable) } returns page

        val taskList = taskService.getAll(command)
        assertEquals(2, taskList.page.content.size)
        assertEquals("태스크 1", taskList.page.content[0].title)
        assertEquals("태스크 2", taskList.page.content[1].title)
    }

    @Test
    fun `특정 태스크 ID로 조회 시 태스크 정보를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val command = TaskQuery.FindTask(workspaceId, taskId)

        val task =
            Task
                .create(
                    title = "조회할 태스크",
                    description = "태스크 설명",
                    assigneeId = null,
                    workspaceId = workspaceId,
                ).apply {
                    id = taskId
                }

        every { taskRepository.findActiveTaskByIdOrNull(taskId, WorkspaceId(workspaceId)) } returns task

        val result = taskService.getInformation(command)

        assertTrue(result is TaskResult.TaskInformation)
        val taskInfo = result as TaskResult.TaskInformation
        assertEquals(taskId, taskInfo.id)
        assertEquals("조회할 태스크", taskInfo.title)
    }

    @Test
    fun `존재하지 않는 태스크 조회 시 IdNotFound를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val command = TaskQuery.FindTask(workspaceId, taskId)

        every { taskRepository.findActiveTaskByIdOrNull(taskId, WorkspaceId(workspaceId)) } returns null

        val result = taskService.getInformation(command)

        assertTrue(result is TaskResult.Failure.IdNotFound)
    }
}
