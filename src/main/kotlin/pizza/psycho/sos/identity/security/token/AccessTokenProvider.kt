package pizza.psycho.sos.identity.security.token

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.security.config.JwtProperties
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class AccessTokenProvider(
    private val properties: JwtProperties,
) {
    fun issueAccessToken(account: Account): String {
        val accountId = requireNotNull(account.id) { "Account id is required for access token issuance" }
        val email = account.email.value
        val now = Instant.now()
        val expiresAt = now.plusSeconds(properties.accessTokenValiditySeconds)

        return Jwts
            .builder()
            .subject(accountId.toString())
            .issuer(properties.issuer)
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey())
            .compact()
    }

    fun parse(token: String): AccessTokenClaims? =
        try {
            val claims =
                Jwts
                    .parser()
                    .verifyWith(signingKey())
                    .requireIssuer(properties.issuer)
                    .build()
                    .parseSignedClaims(token)
                    .payload

            val accountId = UUID.fromString(claims.subject)
            val email = claims["email"] as? String ?: return null
            AccessTokenClaims(accountId = accountId, email = email)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: JwtException) {
            null
        }

    private fun signingKey() = Keys.hmacShaKeyFor(properties.secret.toByteArray(StandardCharsets.UTF_8))
}
