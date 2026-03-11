package pizza.psycho.sos.identity.security.token

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.domain.vo.Email
import pizza.psycho.sos.identity.security.config.JwtProperties
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID

@ActiveProfiles("test")
class AccessTokenProviderTests {
    private val properties =
        JwtProperties().apply {
            issuer = "psycho-sos-test"
            accessTokenValiditySeconds = 3600
            secret = "test-secret-key-with-at-least-thirty-two-bytes-123"
        }
    private val accessTokenProvider = AccessTokenProvider(properties)

    @Test
    fun `issueAccessToken creates token that can be parsed into claims`() {
        val account =
            Account.Companion
                .create(
                    email = Email.of("user@psycho.pizza"),
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).apply {
                    id = UUID.fromString("00000000-0000-0000-0000-000000000222")
                }

        val accessToken = accessTokenProvider.issueAccessToken(account)
        val claims = accessTokenProvider.parse(accessToken)

        Assertions.assertNotNull(claims)
        Assertions.assertEquals("00000000-0000-0000-0000-000000000222", claims!!.accountId.toString())
        Assertions.assertEquals("user@psycho.pizza", claims.email)
    }

    @Test
    fun `parse returns null for malformed token`() {
        val claims = accessTokenProvider.parse("not-a-jwt")

        Assertions.assertNull(claims)
    }

    @Test
    fun `parse returns null when issuer does not match`() {
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

        val claims = accessTokenProvider.parse(forgedToken)

        Assertions.assertNull(claims)
    }
}
