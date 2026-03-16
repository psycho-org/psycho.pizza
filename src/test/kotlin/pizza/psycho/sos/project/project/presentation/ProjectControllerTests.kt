package pizza.psycho.sos.project.project.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.common.config.PageableProperties
import pizza.psycho.sos.common.support.pagination.PageInfoSupport
import pizza.psycho.sos.identity.security.principal.ActiveAccountPrincipalQueryService
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.service.ProjectService
import pizza.psycho.sos.project.project.application.service.dto.ProjectCommand
import pizza.psycho.sos.project.project.application.service.dto.ProjectQuery
import pizza.psycho.sos.project.project.application.service.dto.ProjectResult
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

@WebMvcTest(
    controllers = [ProjectController::class],
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProjectControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var projectService: ProjectService

    @MockitoBean
    private lateinit var pageInfoSupport: PageInfoSupport

    @MockitoBean
    private lateinit var pageableProperties: PageableProperties

    @MockitoBean
    private lateinit var accessTokenProvider: AccessTokenProvider

    @MockitoBean
    private lateinit var activeAccountPrincipalQueryService: ActiveAccountPrincipalQueryService

    // ------------------------------------------------------------------------------------------------
    // POST /api/v1/workspaces/{workspaceId}/projects
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `프로젝트 생성 시 생성된 프로젝트 정보를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        `when`(
            projectService.create(
                ProjectCommand.Create(
                    workspaceId = WorkspaceId(workspaceId),
                    name = "새 프로젝트",
                ),
            ),
        ).thenReturn(
            ProjectResult.ProjectInfo(
                workspaceId = WorkspaceId(workspaceId),
                projectId = projectId,
                name = "새 프로젝트",
                progress = ProjectResult.Progress(totalCount = 0, completedCount = 0, progress = 0.0),
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/workspaces/$workspaceId/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "name": "새 프로젝트"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.projectId").value(projectId.toString()))
            .andExpect(jsonPath("$.data.name").value("새 프로젝트"))
            .andExpect(jsonPath("$.data.progress.totalCount").value(0))
    }

    // ------------------------------------------------------------------------------------------------
    // GET /api/v1/workspaces/{workspaceId}/projects/{projectId}
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `프로젝트 조회 시 프로젝트 상세 정보를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        `when`(
            projectService.getProject(
                ProjectQuery.Find(WorkspaceId(workspaceId), projectId),
            ),
        ).thenReturn(
            ProjectResult.ProjectInfo(
                workspaceId = WorkspaceId(workspaceId),
                projectId = projectId,
                name = "조회할 프로젝트",
                progress = ProjectResult.Progress(totalCount = 5, completedCount = 3, progress = 0.6),
            ),
        )

        mockMvc
            .perform(
                get("/api/v1/workspaces/$workspaceId/projects/$projectId"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.projectId").value(projectId.toString()))
            .andExpect(jsonPath("$.data.name").value("조회할 프로젝트"))
            .andExpect(jsonPath("$.data.progress.totalCount").value(5))
            .andExpect(jsonPath("$.data.progress.completedCount").value(3))
    }

    @Test
    fun `존재하지 않는 프로젝트 조회 시 에러를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        `when`(
            projectService.getProject(
                ProjectQuery.Find(WorkspaceId(workspaceId), projectId),
            ),
        ).thenReturn(ProjectResult.Failure.IdNotFound)

        mockMvc
            .perform(
                get("/api/v1/workspaces/$workspaceId/projects/$projectId"),
            ).andExpect(status().is4xxClientError)
    }

    // ------------------------------------------------------------------------------------------------
    // GET /api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `프로젝트 내 태스크 목록 조회 시 페이지네이션된 리스트를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val taskId1 = UUID.randomUUID()
        val taskId2 = UUID.randomUUID()
        val pageable = PageRequest.of(0, 10)

        val task1 =
            ProjectResult.Task(
                id = taskId1,
                title = "태스크 1",
                status = Status.TODO,
                assignee = null,
                dueDate = null,
            )
        val task2 =
            ProjectResult.Task(
                id = taskId2,
                title = "태스크 2",
                status = Status.DONE,
                assignee = ProjectResult.Assignee(id = UUID.randomUUID(), name = "홍길동", email = "hong@example.com"),
                dueDate = Instant.parse("2026-12-31T00:00:00Z"),
            )

        val page = PageImpl(listOf(task1, task2), pageable, 2)

        `when`(
            projectService.getTasksInProject(
                ProjectQuery.FindTasksInProject(WorkspaceId(workspaceId), projectId, pageable),
            ),
        ).thenReturn(ProjectResult.TaskList(page))

        mockMvc
            .perform(
                get("/api/v1/workspaces/$workspaceId/projects/$projectId/tasks")
                    .param("page", "0")
                    .param("size", "10"),
            ).andExpect(status().isOk)

        verify(projectService).getTasksInProject(
            ProjectQuery.FindTasksInProject(WorkspaceId(workspaceId), projectId, pageable),
        )
    }

    @Test
    fun `존재하지 않는 프로젝트의 태스크 목록 조회 시 에러를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val pageable = PageRequest.of(0, 10)

        `when`(
            projectService.getTasksInProject(
                ProjectQuery.FindTasksInProject(WorkspaceId(workspaceId), projectId, pageable),
            ),
        ).thenReturn(ProjectResult.Failure.IdNotFound)

        mockMvc
            .perform(
                get("/api/v1/workspaces/$workspaceId/projects/$projectId/tasks")
                    .param("page", "0")
                    .param("size", "10"),
            ).andExpect(status().is4xxClientError)
    }

    @Test
    fun `프로젝트에 태스크 생성 시 actor를 포함해 결과를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val dueDate = Instant.parse("2026-12-31T00:00:00Z")

        `when`(
            projectService.createTask(
                ProjectCommand.CreateTask(
                    workspaceId = WorkspaceId(workspaceId),
                    projectId = projectId,
                    title = "새 태스크",
                    description = "설명",
                    assigneeId = null,
                    dueDate = dueDate,
                    createdBy = accountId,
                ),
            ),
        ).thenReturn(
            ProjectResult.Task(
                id = taskId,
                title = "새 태스크",
                status = Status.TODO,
                assignee = null,
                dueDate = dueDate,
            ),
        )

        withPrincipal(accountId) {
            mockMvc
                .perform(
                    post("/api/v1/workspaces/$workspaceId/projects/$projectId/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "새 태스크",
                                "description": "설명",
                                "dueDate": "$dueDate"
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(taskId.toString()))
                .andExpect(jsonPath("$.data.title").value("새 태스크"))
        }
    }

    // ------------------------------------------------------------------------------------------------
    // PATCH /api/v1/workspaces/{workspaceId}/projects/{projectId}
    // ------------------------------------------------------------------------------------------------

    @Test
    fun `프로젝트 수정 시 성공 메시지를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val addTaskId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        `when`(
            projectService.modify(
                ProjectCommand.Update(
                    workspaceId = WorkspaceId(workspaceId),
                    projectId = projectId,
                    name = "수정된 프로젝트",
                    addTaskIds = listOf(addTaskId),
                    removeTaskIds = emptyList(),
                    updatedBy = accountId,
                ),
            ),
        ).thenReturn(ProjectResult.Success)

        withPrincipal(accountId) {
            mockMvc
                .perform(
                    patch("/api/v1/workspaces/$workspaceId/projects/$projectId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "name": "수정된 프로젝트",
                                "addTaskIds": ["$addTaskId"],
                                "removeTaskIds": []
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("데이터 수정에 성공하였습니다."))
        }
    }

    @Test
    fun `존재하지 않는 프로젝트 수정 시 에러를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        `when`(
            projectService.modify(
                ProjectCommand.Update(
                    workspaceId = WorkspaceId(workspaceId),
                    projectId = projectId,
                    name = "수정",
                    addTaskIds = emptyList(),
                    removeTaskIds = emptyList(),
                    updatedBy = accountId,
                ),
            ),
        ).thenReturn(ProjectResult.Failure.IdNotFound)

        withPrincipal(accountId) {
            mockMvc
                .perform(
                    patch("/api/v1/workspaces/$workspaceId/projects/$projectId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "name": "수정"
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().is4xxClientError)
        }
    }

    @Test
    fun `프로젝트와 하위 태스크 삭제 시 삭제 결과를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(
            projectService.remove(
                ProjectCommand.Remove(WorkspaceId(workspaceId), projectId, userId, "삭제 사유"),
            ),
        ).thenReturn(ProjectResult.Remove(projectCount = 1, taskCount = 3))

        withPrincipal(userId) {
            mockMvc
                .perform(
                    delete("/api/v1/workspaces/$workspaceId/projects/$projectId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"reason":"삭제 사유"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("프로젝트 및 하위 태스크 삭제에 성공하였습니다."))
                .andExpect(jsonPath("$.data.projectCount").value(1))
                .andExpect(jsonPath("$.data.taskCount").value(3))
        }
    }

    @Test
    fun `존재하지 않는 프로젝트와 하위 태스크 삭제 시 에러를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(
            projectService.remove(
                ProjectCommand.Remove(WorkspaceId(workspaceId), projectId, userId, "삭제 사유"),
            ),
        ).thenReturn(ProjectResult.Failure.IdNotFound)

        withPrincipal(userId) {
            mockMvc
                .perform(
                    delete("/api/v1/workspaces/$workspaceId/projects/$projectId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"reason":"삭제 사유"}"""),
                ).andExpect(status().is4xxClientError)
        }
    }

    private fun withPrincipal(
        accountId: UUID,
        block: () -> Unit,
    ) {
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication =
            UsernamePasswordAuthenticationToken(
                AuthenticatedAccountPrincipal(accountId = accountId, email = "test@psycho.pizza"),
                null,
                emptyList(),
            )
        SecurityContextHolder.setContext(context)
        try {
            block()
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
