package pizza.psycho.sos.identity.authentication.application.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.config.JwtProperties
import java.util.UUID

class JwtTokenServiceTests {
    private val properties =
        JwtProperties().apply {
            issuer = "psycho-sos-test"
            accessTokenValiditySeconds = 3600
            secret = "test-secret-key-with-at-least-thirty-two-bytes-123"
        }
    private val jwtTokenService = JwtTokenService(properties)

    @Test
    fun `issueAccessToken creates token that can be parsed into authentication`() {
        val account =
            Account
                .create(
                    email = "user@psycho.pizza",
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).apply {
                    id = UUID.fromString("00000000-0000-0000-0000-000000000222")
                }

        val accessToken = jwtTokenService.issueAccessToken(account)
        val authentication = jwtTokenService.toAuthentication(accessToken)

        assertNotNull(authentication)
        val principal = authentication!!.principal as AuthenticatedAccountPrincipal
        assertEquals("00000000-0000-0000-0000-000000000222", principal.accountId.toString())
        assertEquals("user@psycho.pizza", principal.email)
    }

    @Test
    fun `toAuthentication returns null for malformed token`() {
        val authentication = jwtTokenService.toAuthentication("not-a-jwt")

        assertNull(authentication)
    }
}
