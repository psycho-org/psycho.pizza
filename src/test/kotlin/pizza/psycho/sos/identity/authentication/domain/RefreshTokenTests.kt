package pizza.psycho.sos.identity.authentication.domain

import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ActiveProfiles("test")
class RefreshTokenTests {
    @Test
    fun `isExpired returns false when now is before expiresAt`() {
        val expiresAt = Instant.parse("2030-01-01T00:00:00Z")
        val token = createToken(expiresAt)

        val now = Instant.parse("2029-12-31T23:59:59Z")
        assertFalse(token.isExpired(now))
    }

    @Test
    fun `isExpired returns true when now is equal to expiresAt`() {
        val expiresAt = Instant.parse("2030-01-01T00:00:00Z")
        val token = createToken(expiresAt)

        assertTrue(token.isExpired(expiresAt))
    }

    @Test
    fun `revoke marks token as revoked and keeps first revoked timestamp`() {
        val token = createToken(Instant.parse("2030-01-01T00:00:00Z"))
        val first = Instant.parse("2026-01-01T00:00:00Z")
        val second = Instant.parse("2026-01-02T00:00:00Z")

        token.revoke(first)
        token.revoke(second)

        assertTrue(token.isRevoked())
        assertEquals(token.revokedAt, first)
    }

    private fun createToken(expiresAt: Instant): RefreshToken =
        RefreshToken.create(
            accountId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            tokenHash = "token-hash",
            jti = "jti-1",
            expiresAt = expiresAt,
        )
}
