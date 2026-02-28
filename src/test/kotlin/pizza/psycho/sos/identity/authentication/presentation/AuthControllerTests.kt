package pizza.psycho.sos.identity.authentication.presentation

import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pizza.psycho.sos.identity.authentication.application.service.AuthService
import pizza.psycho.sos.identity.authentication.application.service.dto.AuthQuery
import pizza.psycho.sos.identity.authentication.application.service.dto.AuthResult
import pizza.psycho.sos.identity.security.config.JwtProperties
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import java.util.UUID

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var jwtProperties: JwtProperties

    @MockitoBean
    private lateinit var accessTokenProvider: AccessTokenProvider

    @Test
    fun `login returns authenticated payload and refresh cookie`() {
        configureCookieProperties()
        `when`(
            authService.login(
                AuthQuery.Login(
                    email = "user@psycho.pizza",
                    password = "Password123!",
                ),
            ),
        ).thenReturn(
            AuthResult.Login.Authenticated(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                user =
                    AuthResult.User(
                        id = UUID.fromString("00000000-0000-0000-0000-000000000111").toString(),
                        email = "user@psycho.pizza",
                        firstName = "Rick",
                        lastName = "Sanchez",
                    ),
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"user@psycho.pizza","password":"Password123!"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.user.email").value("user@psycho.pizza"))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=refresh-token")))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
    }

    @Test
    fun `login returns unauthorized on invalid credentials`() {
        configureCookieProperties()
        `when`(
            authService.login(
                AuthQuery.Login(
                    email = "user@psycho.pizza",
                    password = "wrong",
                ),
            ),
        ).thenReturn(AuthResult.Login.Failure.InvalidCredentials)

        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"user@psycho.pizza","password":"wrong"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh rotates cookie and returns access token`() {
        configureCookieProperties()
        `when`(authService.refresh(AuthQuery.Refresh("old-refresh-token"))).thenReturn(
            AuthResult.Refresh.Authenticated(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
                user =
                    AuthResult.User(
                        id = UUID.fromString("00000000-0000-0000-0000-000000000222").toString(),
                        email = "refresh@psycho.pizza",
                        firstName = "Beth",
                        lastName = "Smith",
                    ),
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .cookie(jakarta.servlet.http.Cookie("refresh_token", "old-refresh-token")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=new-refresh-token")))
    }

    @Test
    fun `refresh returns unauthorized when token is invalid`() {
        configureCookieProperties()
        `when`(authService.refresh(AuthQuery.Refresh("invalid-refresh-token"))).thenReturn(
            AuthResult.Refresh.Failure.InvalidRefreshToken,
        )

        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .cookie(jakarta.servlet.http.Cookie("refresh_token", "invalid-refresh-token")),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh returns unauthorized when cookie is missing`() {
        configureCookieProperties()
        `when`(authService.refresh(AuthQuery.Refresh(""))).thenReturn(AuthResult.Refresh.Failure.InvalidRefreshToken)

        mockMvc
            .perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `logout clears refresh cookie`() {
        configureCookieProperties()
        `when`(authService.logout(AuthQuery.Logout("refresh-token"))).thenReturn(AuthResult.Logout.Success)

        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .cookie(jakarta.servlet.http.Cookie("refresh_token", "refresh-token")),
            ).andExpect(status().isOk)
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=")))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")))

        verify(authService).logout(AuthQuery.Logout("refresh-token"))
    }

    private fun configureCookieProperties() {
        `when`(jwtProperties.refreshCookieName).thenReturn("refresh_token")
        `when`(jwtProperties.refreshCookiePath).thenReturn("/api/v1/auth")
        `when`(jwtProperties.refreshCookieSameSite).thenReturn("Lax")
        `when`(jwtProperties.refreshCookieSecure).thenReturn(false)
        `when`(jwtProperties.refreshTokenValiditySeconds).thenReturn(1209600L)
    }
}
