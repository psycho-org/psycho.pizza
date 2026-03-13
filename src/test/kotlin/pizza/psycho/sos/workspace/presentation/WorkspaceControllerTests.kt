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
import pizza.psycho.sos.workspace.application.service.WorkspaceService
import pizza.psycho.sos.workspace.domain.model.membership.Role
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
