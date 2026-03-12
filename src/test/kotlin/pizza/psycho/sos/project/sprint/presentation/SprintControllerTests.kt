package pizza.psycho.sos.project.sprint.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.identity.security.principal.ActiveAccountPrincipalQueryService
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.application.service.SprintService
import pizza.psycho.sos.project.sprint.application.service.dto.SprintCommand
import pizza.psycho.sos.project.sprint.application.service.dto.SprintResult
import java.time.Instant
import java.util.UUID

@WebMvcTest(
    controllers = [SprintController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SprintControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var sprintService: SprintService

    @MockitoBean
    private lateinit var accessTokenProvider: AccessTokenProvider

    @MockitoBean
    private lateinit var activeAccountPrincipalQueryService: ActiveAccountPrincipalQueryService

    @Test
    fun `스프린트 생성 시 생성된 정보가 반환된다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val startDate = Instant.parse("2026-01-01T00:00:00Z")
        val endDate = Instant.parse("2026-01-15T00:00:00Z")

        `when`(
            sprintService.create(
                SprintCommand.Create(
                    workspaceId = WorkspaceId(workspaceId),
                    name = "새 스프린트",
                    startDate = startDate,
                    endDate = endDate,
                ),
            ),
        ).thenReturn(
            SprintResult.SprintInfo(
                workspaceId = WorkspaceId(workspaceId),
                sprintId = sprintId,
                name = "새 스프린트",
                startDate = startDate,
                endDate = endDate,
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/workspaces/$workspaceId/sprints")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "name": "새 스프린트",
                            "startDate": "$startDate",
                            "endDate": "$endDate"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.sprintId").value(sprintId.toString()))
            .andExpect(jsonPath("$.data.name").value("새 스프린트"))
    }

    @Test
    fun `스프린트 조회 시 상세 정보를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val startDate = Instant.parse("2026-01-01T00:00:00Z")
        val endDate = Instant.parse("2026-01-15T00:00:00Z")

        `when`(
            sprintService.getSprint(
                SprintCommand.Get(WorkspaceId(workspaceId), sprintId),
            ),
        ).thenReturn(
            SprintResult.SprintInfo(
                workspaceId = WorkspaceId(workspaceId),
                sprintId = sprintId,
                name = "조회 스프린트",
                startDate = startDate,
                endDate = endDate,
            ),
        )

        mockMvc
            .perform(
                get("/api/v1/workspaces/$workspaceId/sprints/$sprintId"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("조회 스프린트"))
    }

    @Test
    fun `스프린트 내 프로젝트 목록을 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        `when`(
            sprintService.getProjectsInSprint(SprintCommand.GetProjects(WorkspaceId(workspaceId), sprintId)),
        ).thenReturn(
            SprintResult.ProjectList(
                listOf(
                    SprintResult.Project(
                        projectId = projectId,
                        name = "프로젝트",
                        progress = SprintResult.Progress(totalCount = 2, completedCount = 1, progress = 0.5),
                    ),
                ),
            ),
        )

        mockMvc
            .perform(
                get("/api/v1/workspaces/$workspaceId/sprints/$sprintId/projects"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].projectId").value(projectId.toString()))
    }

    @Test
    fun `스프린트에서 프로젝트 생성 시 결과를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        `when`(
            sprintService.createProject(
                SprintCommand.CreateProject(WorkspaceId(workspaceId), sprintId, "새 프로젝트"),
            ),
        ).thenReturn(
            SprintResult.ProjectCreated(
                SprintResult.Project(
                    projectId = projectId,
                    name = "새 프로젝트",
                    progress = SprintResult.Progress(totalCount = 0, completedCount = 0, progress = 0.0),
                ),
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/workspaces/$workspaceId/sprints/$sprintId/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "name": "새 프로젝트" }"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.projectId").value(projectId.toString()))
    }

    @Test
    fun `스프린트 수정 시 성공 메시지를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val addProjectId = UUID.randomUUID()

        `when`(
            sprintService.modify(
                SprintCommand.Update(
                    workspaceId = WorkspaceId(workspaceId),
                    sprintId = sprintId,
                    name = "수정된 스프린트",
                    startDate = null,
                    endDate = null,
                    addProjectIds = listOf(addProjectId),
                    removeProjectIds = emptyList(),
                ),
            ),
        ).thenReturn(SprintResult.Success)

        mockMvc
            .perform(
                patch("/api/v1/workspaces/$workspaceId/sprints/$sprintId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "name": "수정된 스프린트",
                            "addProjectIds": ["$addProjectId"],
                            "removeProjectIds": []
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("데이터 수정에 성공하였습니다."))
    }

    @Test
    fun `스프린트 삭제 시 삭제 결과를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(
            sprintService.remove(
                SprintCommand.Remove(WorkspaceId(workspaceId), sprintId, userId),
            ),
        ).thenReturn(SprintResult.Remove(1))

        mockMvc
            .perform(
                delete("/api/v1/workspaces/$workspaceId/sprints/$sprintId/$userId"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.count").value(1))
    }

    @Test
    fun `스프린트와 하위 프로젝트 삭제 시 결과를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(
            sprintService.removeWithTasks(
                SprintCommand.RemoveWithTasks(WorkspaceId(workspaceId), sprintId, userId),
            ),
        ).thenReturn(SprintResult.RemoveWithTasks(sprintCount = 1, projectCount = 2, taskCount = 5))

        mockMvc
            .perform(
                delete("/api/v1/workspaces/$workspaceId/sprints/$sprintId/$userId/with-tasks"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.projectCount").value(2))
            .andExpect(jsonPath("$.data.taskCount").value(5))
        verify(sprintService).removeWithTasks(
            SprintCommand.RemoveWithTasks(WorkspaceId(workspaceId), sprintId, userId),
        )
    }

    @Test
    fun `존재하지 않는 스프린트 삭제 시 에러를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(
            sprintService.remove(
                SprintCommand.Remove(WorkspaceId(workspaceId), sprintId, userId),
            ),
        ).thenReturn(SprintResult.Failure.IdNotFound)

        mockMvc
            .perform(
                delete("/api/v1/workspaces/$workspaceId/sprints/$sprintId/$userId"),
            ).andExpect(status().is4xxClientError)
    }
}
