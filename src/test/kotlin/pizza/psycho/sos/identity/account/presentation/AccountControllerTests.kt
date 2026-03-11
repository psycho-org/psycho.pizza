package pizza.psycho.sos.identity.account.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import java.util.UUID
import pizza.psycho.sos.identity.account.application.service.dto.RegisterAccountResult as Register
import pizza.psycho.sos.identity.account.application.service.dto.UpdateNameAccountResult as UpdateName
import pizza.psycho.sos.identity.account.application.service.dto.WithdrawAccountResult as Withdraw

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
    fun `register returns email and name fields from service payload`() {
        `when`(
            accountService.register(
                AccountCommand.Register(
                    confirmationTokenId = UUID.fromString("00000000-0000-0000-0000-ffffffffffff"),
                    password = "Password123!",
                    firstName = "Rick",
                    lastName = "Sanchez",
                ),
            ),
        ).thenReturn(
            Register.Success(
                email = "user@psycho.pizza",
                givenName = "Rick",
                familyName = "Sanchez",
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/accounts/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "confirmationTokenId":"00000000-0000-0000-0000-ffffffffffff",
                          "password":"Password123!",
                          "givenName":"Rick",
                          "familyName":"Sanchez"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value("user@psycho.pizza"))
            .andExpect(jsonPath("$.data.givenName").value("Rick"))
            .andExpect(jsonPath("$.data.familyName").value("Sanchez"))
    }

    @Test
    fun `update name returns success when service accepts normalized names`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                email = "user@psycho.pizza",
            )
        `when`(
            accountService.updateName(
                AccountCommand.Update.Name(
                    accountId = principal.accountId,
                    givenName = "  Morty  ",
                    familyName = "  Smith  ",
                ),
            ),
        ).thenReturn(
            UpdateName.Success(
                givenName = "Morty",
                familyName = "Smith",
            ),
        )

        withPrincipal(principal) {
            mockMvc
                .perform(
                    post("/api/v1/accounts/me/update/name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"givenName":"  Morty  ","familyName":"  Smith  "}"""),
                ).andExpect(status().isOk)
        }
    }

    @Test
    fun `update name returns bad request when invalid`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                email = "user@psycho.pizza",
            )
        `when`(
            accountService.updateName(
                AccountCommand.Update.Name(
                    accountId = principal.accountId,
                    givenName = "invalid",
                    familyName = "Smith",
                ),
            ),
        ).thenReturn(UpdateName.Failure.InvalidName)

        withPrincipal(principal) {
            mockMvc
                .perform(
                    post("/api/v1/accounts/me/update/name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"givenName":"invalid","familyName":"Smith"}"""),
                ).andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `update name returns not found when account does not exist`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                email = "user@psycho.pizza",
            )
        `when`(
            accountService.updateName(
                AccountCommand.Update.Name(
                    accountId = principal.accountId,
                    givenName = "Summer",
                    familyName = "Smith",
                ),
            ),
        ).thenReturn(UpdateName.Failure.AccountNotFound)

        withPrincipal(principal) {
            mockMvc
                .perform(
                    post("/api/v1/accounts/me/update/name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"givenName":"Summer","familyName":"Smith"}"""),
                ).andExpect(status().isNotFound)
        }
    }

    @Test
    fun `withdraw returns success when service returns success`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000811"),
                email = "user@psycho.pizza",
            )
        `when`(
            accountService.withdraw(
                AccountCommand.Withdraw(
                    accountId = principal.accountId,
                    confirmationTokenId = UUID.fromString("00000000-0000-0000-0000-ffffffffffff"),
                    password = "Password123!",
                ),
            ),
        ).thenReturn(Withdraw.Success)

        withPrincipal(principal) {
            mockMvc
                .perform(
                    post("/api/v1/accounts/me/withdraw")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"confirmationTokenId":"00000000-0000-0000-0000-ffffffffffff","password":"Password123!"}"""),
                ).andExpect(status().isOk)
        }
    }

    @Test
    fun `withdraw returns unauthorized when credentials are invalid`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000822"),
                email = "user@psycho.pizza",
            )
        `when`(
            accountService.withdraw(
                AccountCommand.Withdraw(
                    accountId = principal.accountId,
                    confirmationTokenId = UUID.fromString("00000000-0000-0000-0000-ffffffffffff"),
                    password = "wrong-password",
                ),
            ),
        ).thenReturn(Withdraw.Failure.InvalidCredentials)

        withPrincipal(principal) {
            mockMvc
                .perform(
                    post("/api/v1/accounts/me/withdraw")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"confirmationTokenId":"00000000-0000-0000-0000-ffffffffffff","password":"wrong-password"}"""),
                ).andExpect(status().isUnauthorized)
        }
    }

    @Test
    fun `withdraw returns conflict when owner workspace exists`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000833"),
                email = "user@psycho.pizza",
            )
        `when`(
            accountService.withdraw(
                AccountCommand.Withdraw(
                    accountId = principal.accountId,
                    confirmationTokenId = UUID.fromString("00000000-0000-0000-0000-ffffffffffff"),
                    password = "Password123!",
                ),
            ),
        ).thenReturn(Withdraw.Failure.OwnerWorkspaceExists)

        withPrincipal(principal) {
            mockMvc
                .perform(
                    post("/api/v1/accounts/me/withdraw")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"confirmationTokenId":"00000000-0000-0000-0000-ffffffffffff","password":"Password123!"}"""),
                ).andExpect(status().isConflict)
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
