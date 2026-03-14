package pizza.psycho.sos.identity.account.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.application.service.dto.UpdateNameAccountResult
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
import pizza.psycho.sos.identity.security.config.SecurityConfig
import pizza.psycho.sos.identity.security.filter.JwtAuthenticationFilter
import pizza.psycho.sos.identity.security.principal.ActiveAccountPrincipalQueryService
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import pizza.psycho.sos.identity.security.token.AccessTokenClaims
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import java.util.UUID
import pizza.psycho.sos.identity.account.application.service.dto.RegisterAccountResult as Register

@WebMvcTest(AccountController::class)
@AutoConfigureMockMvc
@Import(SecurityConfig::class, JwtAuthenticationFilter::class)
@ActiveProfiles("test")
class AccountSecurityTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var accountService: AccountService

    @MockitoBean
    private lateinit var accessTokenProvider: AccessTokenProvider

    @MockitoBean
    private lateinit var activeAccountPrincipalQueryService: ActiveAccountPrincipalQueryService

    @MockitoBean
    private lateinit var challengeService: ChallengeService

    @Test
    fun `password policy is accessible without authentication`() {
        mockMvc
            .perform(get("/api/v1/accounts/policies/password"))
            .andExpect(status().isOk)
    }

    @Test
    fun `update name requires authentication`() {
        mockMvc
            .perform(
                post("/api/v1/accounts/me/update/name")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"givenName":"Summer","familyName":"Smith"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `register is accessible without authentication`() {
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
    }

    @Test
    fun `withdraw requires authentication`() {
        mockMvc
            .perform(
                post("/api/v1/accounts/me/withdraw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"confirmationTokenId":"00000000-0000-0000-0000-ffffffffffff","password":"Password123!"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected endpoint remains unauthorized when token account is no longer active`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000812")
        `when`(accessTokenProvider.parse("deleted-access-token")).thenReturn(
            AccessTokenClaims(
                accountId = accountId,
                email = "stale@psycho.pizza",
            ),
        )
        `when`(activeAccountPrincipalQueryService.findActivePrincipalByAccountId(accountId)).thenReturn(null)

        mockMvc
            .perform(
                post("/api/v1/accounts/me/update/name")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer deleted-access-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"givenName":"Summer","familyName":"Smith"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected endpoint succeeds when token account is still active`() {
        val principal =
            AuthenticatedAccountPrincipal(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000813"),
                email = "active@psycho.pizza",
            )
        `when`(accessTokenProvider.parse("active-access-token")).thenReturn(
            AccessTokenClaims(
                accountId = principal.accountId,
                email = "stale@psycho.pizza",
            ),
        )
        `when`(activeAccountPrincipalQueryService.findActivePrincipalByAccountId(principal.accountId)).thenReturn(principal)
        `when`(
            accountService.updateName(
                AccountCommand.Update.Name(
                    accountId = principal.accountId,
                    givenName = "Summer",
                    familyName = "Smith",
                ),
            ),
        ).thenReturn(
            UpdateNameAccountResult.Success(
                givenName = "Summer",
                familyName = "Smith",
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/accounts/me/update/name")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer active-access-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"givenName":"Summer","familyName":"Smith"}"""),
            ).andExpect(status().isOk)
    }
}
