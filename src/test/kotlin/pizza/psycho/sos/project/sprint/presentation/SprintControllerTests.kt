package pizza.psycho.sos.project.sprint.presentation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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
import pizza.psycho.sos.common.response.pagedResponseOf
import pizza.psycho.sos.common.support.pagination.PageInfoSupport
import pizza.psycho.sos.identity.security.principal.ActiveAccountPrincipalQueryService
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.application.service.SprintService
import pizza.psycho.sos.project.sprint.application.service.dto.SprintCommand
import pizza.psycho.sos.project.sprint.application.service.dto.SprintQuery
import pizza.psycho.sos.project.sprint.application.service.dto.SprintResult
import pizza.psycho.sos.project.sprint.presentation.dto.SprintResponse
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
    private lateinit var pageInfoSupport: PageInfoSupport

    @MockitoBean
    private lateinit var accessTokenProvider: AccessTokenProvider

    @MockitoBean
    private lateinit var activeAccountPrincipalQueryService: ActiveAccountPrincipalQueryService

    @Test
    fun `스프린트 목록을 페이지로 조회한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val pageable = PageRequest.of(0, 10)
        val startDate = Instant.parse("2026-01-01T00:00:00Z")
        val endDate = Instant.parse("2026-01-10T00:00:00Z")

        val sprintInfo =
            SprintResult.SprintInfo(
                workspaceId = WorkspaceId(workspaceId),
                sprintId = sprintId,
                name = "Sprint",
                goal = "Goal",
                startDate = startDate,
                endDate = endDate,
            )

        val sprintPage = PageImpl(listOf(sprintInfo), pageable, 1)
        val mappedPage = sprintPage.map { it.toResponseDto() }
        val pagedResponse = pagedResponseOf(mappedPage)
        val query = SprintQuery.FindAll(WorkspaceId(workspaceId), pageable)

        doReturn(SprintResult.SprintPage(sprintPage)).`when`(sprintService).getSprints(any())
        doReturn(pagedResponse).`when`(pageInfoSupport).toPageResponse(any<Page<SprintResponse.Information>>())

        mockMvc
            .perform(
                get("/api/v1/workspaces/$workspaceId/sprints")
                    .param("page", "1")
                    .param("size", "10"),
            ).andExpect(status().isOk)

        val captor = argumentCaptor<SprintQuery.FindAll>()
        verify(sprintService).getSprints(captor.capture())
        assertEquals(workspaceId, captor.firstValue.workspaceId.value)
    }

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
                    goal = "새 목표",
                ),
            ),
        ).thenReturn(
            SprintResult.SprintInfo(
                workspaceId = WorkspaceId(workspaceId),
                sprintId = sprintId,
                name = "새 스프린트",
                goal = "새 목표",
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
                            "endDate": "$endDate",
                            "goal": "새 목표"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.sprintId").value(sprintId.toString()))
            .andExpect(jsonPath("$.data.name").value("새 스프린트"))
            .andExpect(jsonPath("$.data.goal").value("새 목표"))
    }

    @Test
    fun `스프린트 조회 시 상세 정보를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val startDate = Instant.parse("2026-01-01T00:00:00Z")
        val endDate = Instant.parse("2026-01-15T00:00:00Z")

        `when`(
            sprintService.getSprint(
                SprintQuery.Find(WorkspaceId(workspaceId), sprintId),
            ),
        ).thenReturn(
            SprintResult.SprintInfo(
                workspaceId = WorkspaceId(workspaceId),
                sprintId = sprintId,
                name = "조회 스프린트",
                goal = "조회 목표",
                startDate = startDate,
                endDate = endDate,
            ),
        )

        mockMvc
            .perform(
                get("/api/v1/workspaces/$workspaceId/sprints/$sprintId"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("조회 스프린트"))
            .andExpect(jsonPath("$.data.goal").value("조회 목표"))
    }

    @Test
    fun `스프린트 내 프로젝트 목록을 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        `when`(
            sprintService.getProjectsInSprint(SprintQuery.FindProjectsInSprint(WorkspaceId(workspaceId), sprintId)),
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
        val accountId = UUID.randomUUID()

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
                    by = accountId,
                ),
            ),
        ).thenReturn(SprintResult.Success)

        mockMvc
            .perform(
                patch("/api/v1/workspaces/$workspaceId/sprints/$sprintId")
                    .param("account", accountId.toString())
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
            .andExpect(jsonPath("$.message").value("Data modification was successful."))
    }

    @Test
    fun `스프린트와 하위 프로젝트 삭제 시 결과를 반환한다`() {
        val workspaceId = UUID.randomUUID()
        val sprintId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(
            sprintService.remove(
                SprintCommand.Remove(WorkspaceId(workspaceId), sprintId, userId),
            ),
        ).thenReturn(SprintResult.Remove(sprintCount = 1, projectCount = 2, taskCount = 5))

        mockMvc
            .perform(
                delete("/api/v1/workspaces/$workspaceId/sprints/$sprintId")
                    .param("account", userId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.projectCount").value(2))
            .andExpect(jsonPath("$.data.taskCount").value(5))
        verify(sprintService).remove(
            SprintCommand.Remove(WorkspaceId(workspaceId), sprintId, userId),
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
                delete("/api/v1/workspaces/$workspaceId/sprints/$sprintId")
                    .param("account", userId.toString()),
            ).andExpect(status().is4xxClientError)
    }
}

private fun SprintResult.SprintInfo.toResponseDto(): SprintResponse.Information =
    SprintResponse.Information(
        workspaceId = workspaceId.value,
        sprintId = sprintId,
        name = name,
        goal = goal,
        startDate = startDate,
        endDate = endDate,
    )
