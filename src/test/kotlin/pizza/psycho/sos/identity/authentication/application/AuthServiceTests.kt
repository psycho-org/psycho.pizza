package pizza.psycho.sos.identity.authentication.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import pizza.psycho.sos.identity.authentication.application.service.AuthService
import pizza.psycho.sos.identity.authentication.application.service.RefreshTokenService
import pizza.psycho.sos.identity.authentication.application.service.dto.AuthQuery
import pizza.psycho.sos.identity.authentication.application.service.dto.AuthResult
import pizza.psycho.sos.identity.security.token.AccessTokenProvider
import java.util.UUID

class AuthServiceTests {
    private val accountRepository = mock(AccountRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val accessTokenProvider = mock(AccessTokenProvider::class.java)
    private val refreshTokenService = mock(RefreshTokenService::class.java)
    private val authService = AuthService(accountRepository, passwordEncoder, accessTokenProvider, refreshTokenService)

    @Test
    fun `login returns invalid credentials failure when account is missing`() {
        val query =
            AuthQuery.Login(
                email = "missing@psycho.pizza",
                password = "Password123!",
            )
        `when`(accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("missing@psycho.pizza")).thenReturn(null)

        val result = authService.login(query)
        assertTrue(result is AuthResult.Login.Failure.InvalidCredentials)
    }

    @Test
    fun `login returns invalid credentials failure when password does not match`() {
        val query =
            AuthQuery.Login(
                email = "user@psycho.pizza",
                password = "WrongPassword!",
            )
        val account =
            Account.create(
                email = "user@psycho.pizza",
                passwordHash = "encoded-password",
                givenName = "Rick",
                familyName = "Sanchez",
            )
        account.id = UUID.fromString("00000000-0000-0000-0000-000000000222")

        `when`(accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("user@psycho.pizza")).thenReturn(account)
        `when`(passwordEncoder.matches("WrongPassword!", "encoded-password")).thenReturn(false)

        val result = authService.login(query)
        assertTrue(result is AuthResult.Login.Failure.InvalidCredentials)
    }

    @Test
    fun `login returns auth response when credentials are valid`() {
        val query =
            AuthQuery.Login(
                email = " User@Psycho.Pizza ",
                password = "Password123!",
            )
        val account =
            Account.create(
                email = "user@psycho.pizza",
                passwordHash = "encoded-password",
                givenName = "Rick",
                familyName = "Sanchez",
            )
        account.id = UUID.fromString("00000000-0000-0000-0000-000000000333")

        `when`(accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("user@psycho.pizza")).thenReturn(account)
        `when`(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true)
        `when`(accessTokenProvider.issueAccessToken(account)).thenReturn("mock-access-token")
        `when`(refreshTokenService.issue(UUID.fromString("00000000-0000-0000-0000-000000000333"))).thenReturn(
            "mock-refresh-token",
        )

        val result = authService.login(query)
        val response = result as AuthResult.Login.Authenticated

        assertEquals("mock-access-token", response.accessToken)
        assertEquals("mock-refresh-token", response.refreshToken)
        assertEquals("00000000-0000-0000-0000-000000000333", response.user.id)
        assertEquals("user@psycho.pizza", response.user.email)
        assertNotNull(response.user.firstName)
        assertNotNull(response.user.lastName)
    }

    @Test
    fun `refresh rotates token and returns authenticated result`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000444")
        val account =
            Account
                .create(
                    email = "refresh@psycho.pizza",
                    passwordHash = "encoded-password",
                    givenName = "Beth",
                    familyName = "Smith",
                ).apply { id = accountId }

        `when`(refreshTokenService.rotate("refresh-token")).thenReturn(
            RefreshTokenService.RotatedRefreshToken(
                accountId = accountId,
                refreshToken = "new-refresh-token",
            ),
        )
        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)
        `when`(accessTokenProvider.issueAccessToken(account)).thenReturn("new-access-token")

        val result = authService.refresh(AuthQuery.Refresh("refresh-token"))
        val response = result as AuthResult.Refresh.Authenticated

        assertEquals("new-access-token", response.accessToken)
        assertEquals("new-refresh-token", response.refreshToken)
        assertEquals(accountId.toString(), response.user.id)
    }

    @Test
    fun `logout revokes refresh token and returns logged out`() {
        val result = authService.logout(AuthQuery.Logout("refresh-token"))

        assertTrue(result is AuthResult.Logout.Success)
        verify(refreshTokenService).revoke("refresh-token")
    }
}
