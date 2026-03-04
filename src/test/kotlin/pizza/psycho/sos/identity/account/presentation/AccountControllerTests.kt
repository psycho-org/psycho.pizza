package pizza.psycho.sos.identity.account.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.application.service.dto.AccountResult
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import java.util.UUID

@WebMvcTest(AccountController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AccountControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var accountService: AccountService

    @MockitoBean
    private lateinit var accessTokenProvider: AccessTokenProvider

    @Test
    fun `update display name returns updated display name from service payload`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                email = "user@psycho.pizza",
            )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        `when`(
            accountService.updateDisplayName(
                AccountCommand.UpdateDisplayName(
                    accountId = principal.accountId,
                    displayName = "  Pickle Rick  ",
                ),
            ),
        ).thenReturn(
            AccountResult.Updated.DisplayName(
                displayName = "Pickle Rick",
            ),
        )

        mockMvc
            .perform(
                patch("/api/v1/accounts/me/display-name")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"displayName":"  Pickle Rick  "}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.displayName").value("Pickle Rick"))
    }

    @Test
    fun `update display name returns bad request when invalid`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                email = "user@psycho.pizza",
            )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        `when`(
            accountService.updateDisplayName(
                AccountCommand.UpdateDisplayName(
                    accountId = principal.accountId,
                    displayName = "invalid",
                ),
            ),
        ).thenReturn(AccountResult.Failure.InvalidDisplayName)

        mockMvc
            .perform(
                patch("/api/v1/accounts/me/display-name")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"displayName":"invalid"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `update display name returns not found when account does not exist`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                email = "user@psycho.pizza",
            )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        `when`(
            accountService.updateDisplayName(
                AccountCommand.UpdateDisplayName(
                    accountId = principal.accountId,
                    displayName = "Summer",
                ),
            ),
        ).thenReturn(AccountResult.Failure.AccountNotFound)

        mockMvc
            .perform(
                patch("/api/v1/accounts/me/display-name")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"displayName":"Summer"}"""),
            ).andExpect(status().isNotFound)
    }
}
