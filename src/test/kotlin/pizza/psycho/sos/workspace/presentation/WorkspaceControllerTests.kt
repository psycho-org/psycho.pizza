package pizza.psycho.sos.workspace.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.identity.security.principal.ActiveAccountPrincipalQueryService
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import pizza.psycho.sos.workspace.application.dto.ActiveWorkspaceMembership
import pizza.psycho.sos.workspace.application.dto.WorkspaceMemberListItem
import pizza.psycho.sos.workspace.application.service.WorkspaceService
import pizza.psycho.sos.workspace.domain.model.membership.Role
import java.time.Instant
import java.util.UUID

@WebMvcTest(WorkspaceController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class WorkspaceControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var workspaceService: WorkspaceService

    @MockitoBean
    private lateinit var accessTokenProvider: AccessTokenProvider

    @MockitoBean
    private lateinit var activeAccountPrincipalQueryService: ActiveAccountPrincipalQueryService

    @Test
    fun `list returns active workspace memberships for authenticated account`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000201")
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = accountId,
                email = "user@psycho.pizza",
            )
        val workspaceId1 = UUID.fromString("00000000-0000-0000-0000-000000000301")
        val workspaceId2 = UUID.fromString("00000000-0000-0000-0000-000000000302")

        `when`(workspaceService.findActiveWorkspaceMembershipsByAccountId(accountId)).thenReturn(
            listOf(
                ActiveWorkspaceMembership(workspaceId1, "Alpha", Role.OWNER),
                ActiveWorkspaceMembership(workspaceId2, "Beta", Role.CREW),
            ),
        )

        withPrincipal(principal) {
            mockMvc
                .perform(get("/api/v1/workspaces"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].id").value(workspaceId1.toString()))
                .andExpect(jsonPath("$.data[0].title").value("Alpha"))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data[1].id").value(workspaceId2.toString()))
                .andExpect(jsonPath("$.data[1].title").value("Beta"))
                .andExpect(jsonPath("$.data[1].role").value("CREW"))
        }
    }

    @Test
    fun `listMembers returns workspace members for authenticated account`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000201")
        val workspaceId = UUID.fromString("00000000-0000-0000-0000-000000000301")
        val membershipId = UUID.fromString("00000000-0000-0000-0000-000000000401")
        val memberAccountId = UUID.fromString("00000000-0000-0000-0000-000000000402")
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = accountId,
                email = "user@psycho.pizza",
            )
        val joinedAt = Instant.parse("2026-03-14T11:26:28Z")

        `when`(workspaceService.listMembers(workspaceId, accountId)).thenReturn(
            listOf(
                WorkspaceMemberListItem(
                    membershipId = membershipId,
                    accountId = memberAccountId,
                    name = " Crew Member ",
                    role = Role.CREW,
                    joinedAt = joinedAt,
                ),
            ),
        )

        withPrincipal(principal) {
            mockMvc
                .perform(get("/api/v1/workspaces/$workspaceId/members"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].membershipId").value(membershipId.toString()))
                .andExpect(jsonPath("$.data[0].accountId").value(memberAccountId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Crew Member"))
                .andExpect(jsonPath("$.data[0].role").value("CREW"))
                .andExpect(jsonPath("$.data[0].joinedAt").value(joinedAt.toString()))
        }
    }

    private fun withPrincipal(
        principal: AuthenticatedAccountPrincipal,
        block: () -> Unit,
    ) {
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        SecurityContextHolder.setContext(context)
        try {
            block()
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
