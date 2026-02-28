package pizza.psycho.sos.identity.security.token

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.security.config.JwtProperties
import pizza.psycho.sos.identity.security.principal.AuthenticatedAccountPrincipal
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID

class AccessTokenProviderTests {
    private val properties =
        JwtProperties().apply {
            issuer = "psycho-sos-test"
            accessTokenValiditySeconds = 3600
            secret = "test-secret-key-with-at-least-thirty-two-bytes-123"
        }
    private val accessTokenProvider = AccessTokenProvider(properties)

    @Test
    fun `issueAccessToken creates token that can be parsed into authentication`() {
        val account =
            Account.Companion
                .create(
                    email = "user@psycho.pizza",
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).apply {
                    id = UUID.fromString("00000000-0000-0000-0000-000000000222")
                }

        val accessToken = accessTokenProvider.issueAccessToken(account)
        val authentication = accessTokenProvider.toAuthentication(accessToken)

        Assertions.assertNotNull(authentication)
        val principal = authentication!!.principal as AuthenticatedAccountPrincipal
        Assertions.assertEquals("00000000-0000-0000-0000-000000000222", principal.accountId.toString())
        Assertions.assertEquals("user@psycho.pizza", principal.email)
    }

    @Test
    fun `toAuthentication returns null for malformed token`() {
        val authentication = accessTokenProvider.toAuthentication("not-a-jwt")

        Assertions.assertNull(authentication)
    }

    @Test
    fun `toAuthentication returns null when issuer does not match`() {
        val forgedToken =
            Jwts
                .builder()
                .subject(UUID.fromString("00000000-0000-0000-0000-000000000222").toString())
                .issuer("different-issuer")
                .claim("email", "user@psycho.pizza")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(properties.secret.toByteArray(StandardCharsets.UTF_8)))
                .compact()

        val authentication = accessTokenProvider.toAuthentication(forgedToken)

        Assertions.assertNull(authentication)
    }
}
