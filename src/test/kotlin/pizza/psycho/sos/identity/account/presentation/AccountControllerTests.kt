package pizza.psycho.sos.identity.account.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import java.util.UUID
import pizza.psycho.sos.identity.account.application.service.dto.UpdateDisplayNameAccountResult as UpdateDisplayName
import pizza.psycho.sos.identity.account.application.service.dto.WithdrawAccountResult as Withdraw

@WebMvcTest(AccountController::class)
@AutoConfigureMockMvc
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
                AccountCommand.Update.DisplayName(
                    accountId = principal.accountId,
                    displayName = "  Pickle Rick  ",
                ),
            ),
        ).thenReturn(
            UpdateDisplayName.Success(
                displayName = "Pickle Rick",
            ),
        )

        mockMvc
            .perform(
                patch("/api/v1/accounts/me/update/display-name")
                    .with(authentication(authentication))
                    .with(csrf())
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
                AccountCommand.Update.DisplayName(
                    accountId = principal.accountId,
                    displayName = "invalid",
                ),
            ),
        ).thenReturn(UpdateDisplayName.Failure.InvalidDisplayName)

        mockMvc
            .perform(
                patch("/api/v1/accounts/me/update/display-name")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"displayName":"invalid"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("ACCOUNT_INVALID_DISPLAY_NAME"))
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
                AccountCommand.Update.DisplayName(
                    accountId = principal.accountId,
                    displayName = "Summer",
                ),
            ),
        ).thenReturn(UpdateDisplayName.Failure.AccountNotFound)

        mockMvc
            .perform(
                patch("/api/v1/accounts/me/update/display-name")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"displayName":"Summer"}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
    }

    @Test
    fun `withdraw returns success when service returns success`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000811"),
                email = "user@psycho.pizza",
            )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        `when`(
            accountService.withdraw(
                AccountCommand.Withdraw(
                    accountId = principal.accountId,
                    confirmationTokenId = UUID.fromString("00000000-0000-0000-0000-ffffffffffff"),
                    password = "Password123!",
                ),
            ),
        ).thenReturn(Withdraw.Success)

        mockMvc
            .perform(
                post("/api/v1/accounts/me/withdraw")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"confirmationTokenId":"00000000-0000-0000-0000-ffffffffffff","password":"Password123!"}"""),
            ).andExpect(status().isOk)
    }

    @Test
    fun `withdraw returns unauthorized when credentials are invalid`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000822"),
                email = "user@psycho.pizza",
            )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        `when`(
            accountService.withdraw(
                AccountCommand.Withdraw(
                    accountId = principal.accountId,
                    confirmationTokenId = UUID.fromString("00000000-0000-0000-0000-ffffffffffff"),
                    password = "wrong-password",
                ),
            ),
        ).thenReturn(Withdraw.Failure.InvalidCredentials)

        mockMvc
            .perform(
                post("/api/v1/accounts/me/withdraw")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"confirmationTokenId":"00000000-0000-0000-0000-ffffffffffff","password":"wrong-password"}"""),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("ACCOUNT_INVALID_CREDENTIALS"))
    }

    @Test
    fun `withdraw returns precondition failed when owner workspace exists`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000833"),
                email = "user@psycho.pizza",
            )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        `when`(
            accountService.withdraw(
                AccountCommand.Withdraw(
                    accountId = principal.accountId,
                    confirmationTokenId = UUID.fromString("00000000-0000-0000-0000-ffffffffffff"),
                    password = "Password123!",
                ),
            ),
        ).thenReturn(Withdraw.Failure.OwnerWorkspaceExists)

        mockMvc
            .perform(
                post("/api/v1/accounts/me/withdraw")
                    .with(authentication(authentication))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"confirmationTokenId":"00000000-0000-0000-0000-ffffffffffff","password":"Password123!"}"""),
            ).andExpect(status().isPreconditionFailed)
            .andExpect(jsonPath("$.code").value("ACCOUNT_WITHDRAWAL_BLOCKED_BY_OWNED_WORKSPACE"))
    }
}
