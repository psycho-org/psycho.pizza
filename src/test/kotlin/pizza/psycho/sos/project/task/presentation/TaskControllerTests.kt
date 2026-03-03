package pizza.psycho.sos.project.task.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.common.config.PageableProperties
import pizza.psycho.sos.common.support.pagination.PageInfoSupport
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import pizza.psycho.sos.project.task.application.service.TaskService
import pizza.psycho.sos.project.task.application.service.dto.TaskCommand
import pizza.psycho.sos.project.task.application.service.dto.TaskResult
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.util.UUID

@WebMvcTest(
    controllers = [TaskController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TaskControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var taskService: TaskService

    @MockitoBean
    private lateinit var pageInfoSupport: PageInfoSupport

    @MockitoBean
    private lateinit var pageableProperties: PageableProperties

    @MockitoBean
    private lateinit var accessTokenProvider: AccessTokenProvider

    @Test
    fun `태스크 생성 시 201 상태코드와 생성된 태스크 정보를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val assigneeId = UUID.randomUUID()

        `when`(
            taskService.create(
                TaskCommand.AddTask(
                    workspaceId = workspaceId,
                    title = "새로운 태스크",
                    description = "태스크 설명",
                    assigneeId = assigneeId,
                    dueDate = null,
                ),
            ),
        ).thenReturn(
            TaskResult.TaskInformation(
                id = taskId,
                title = "새로운 태스크",
                description = "태스크 설명",
                status = Status.TODO,
                assignee =
                    TaskResult.Assignee(
                        id = assigneeId,
                        name = "홍길동",
                        email = "hong@example.com",
                    ),
                workspaceId = workspaceId,
                dueDate = null,
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/tasks")
                    .header("WORKSPACE_ID", workspaceId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "title": "새로운 태스크",
                            "description": "태스크 설명",
                            "assigneeId": "$assigneeId"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(taskId.toString()))
            .andExpect(jsonPath("$.data.title").value("새로운 태스크"))
            .andExpect(jsonPath("$.data.description").value("태스크 설명"))
            .andExpect(jsonPath("$.data.assignee.name").value("홍길동"))
    }

    @Test
    fun `태스크 목록 조회 시 페이지네이션된 태스크 리스트를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val taskId1 = UUID.randomUUID()
        val taskId2 = UUID.randomUUID()
        val pageable = PageRequest.of(0, 10)

        val taskListInfo1 =
            TaskResult.TaskListInfo(
                id = taskId1,
                title = "태스크 1",
                assignee = null,
                dueDate = null,
            )

        val taskListInfo2 =
            TaskResult.TaskListInfo(
                id = taskId2,
                title = "태스크 2",
                assignee = null,
                dueDate = null,
            )

        val page = PageImpl(listOf(taskListInfo1, taskListInfo2), pageable, 2)

        `when`(
            taskService.getAll(
                TaskCommand.FindTasks(workspaceId, pageable),
            ),
        ).thenReturn(TaskResult.TaskList(page))

        mockMvc
            .perform(
                get("/api/v1/tasks")
                    .header("WORKSPACE_ID", workspaceId.toString())
                    .param("page", "0")
                    .param("size", "10"),
            ).andExpect(status().isOk)

        verify(taskService).getAll(
            TaskCommand.FindTasks(workspaceId, pageable),
        )
    }

    @Test
    fun `특정 태스크 조회 시 태스크 상세 정보를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()

        `when`(
            taskService.getInformation(
                TaskCommand.FindTask(workspaceId, taskId),
            ),
        ).thenReturn(
            TaskResult.TaskInformation(
                id = taskId,
                title = "조회할 태스크",
                description = "태스크 설명",
                status = Status.IN_PROGRESS,
                assignee = null,
                workspaceId = workspaceId,
                dueDate = null,
            ),
        )

        mockMvc
            .perform(
                get("/api/v1/tasks/$taskId")
                    .header("WORKSPACE_ID", workspaceId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(taskId.toString()))
            .andExpect(jsonPath("$.data.title").value("조회할 태스크"))
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
    }

    @Test
    fun `존재하지 않는 태스크 조회 시 404 에러를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()

        `when`(
            taskService.getInformation(
                TaskCommand.FindTask(workspaceId, taskId),
            ),
        ).thenReturn(TaskResult.Failure.IdNotFound)

        mockMvc
            .perform(
                get("/api/v1/tasks/$taskId")
                    .header("WORKSPACE_ID", workspaceId.toString()),
            ).andExpect(status().is4xxClientError)
    }

    @Test
    fun `assigneeId 없이 태스크 생성이 가능하다`() {
        val workspaceId = UUID.randomUUID()
        val taskId = UUID.randomUUID()

        `when`(
            taskService.create(
                TaskCommand.AddTask(
                    workspaceId = workspaceId,
                    title = "담당자 없는 태스크",
                    description = "설명",
                    assigneeId = null,
                    dueDate = null,
                ),
            ),
        ).thenReturn(
            TaskResult.TaskInformation(
                id = taskId,
                title = "담당자 없는 태스크",
                description = "설명",
                status = Status.TODO,
                assignee = null,
                workspaceId = workspaceId,
                dueDate = null,
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/tasks")
                    .header("WORKSPACE_ID", workspaceId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "title": "담당자 없는 태스크",
                            "description": "설명"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.assignee").isEmpty)
    }
}
