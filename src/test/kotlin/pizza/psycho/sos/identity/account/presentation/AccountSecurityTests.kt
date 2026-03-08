package pizza.psycho.sos.identity.account.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.security.config.SecurityConfig
import pizza.psycho.sos.identity.security.filter.JwtAuthenticationFilter
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
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

    @Test
    fun `update display name requires authentication`() {
        mockMvc
            .perform(
                patch("/api/v1/accounts/me/update/display-name")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"displayName":"Summer"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `register is accessible without authentication`() {
        `when`(
            accountService.register(
                AccountCommand.Register(
                    email = "user@psycho.pizza",
                    password = "Password123!",
                    firstName = "Rick",
                    lastName = "Sanchez",
                ),
            ),
        ).thenReturn(
            Register.Success(
                email = "user@psycho.pizza",
                displayName = "Rick Sanchez",
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/accounts/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email":"user@psycho.pizza",
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
                    .content("""{"password":"Password123!"}"""),
            ).andExpect(status().isUnauthorized)
    }
}
