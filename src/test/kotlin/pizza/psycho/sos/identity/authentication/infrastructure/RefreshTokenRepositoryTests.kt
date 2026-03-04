package pizza.psycho.sos.identity.authentication.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.identity.authentication.domain.RefreshToken
import java.time.Instant
import java.util.UUID

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class RefreshTokenRepositoryTests {
    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Test
    fun `findByTokenHash returns stored token`() {
        val token =
            refreshTokenRepository.save(
                RefreshToken.create(
                    accountId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                    tokenHash = "hash-token-1",
                    jti = "jti-1",
                    expiresAt = Instant.now().plusSeconds(3600),
                ),
            )

        val found = refreshTokenRepository.findByTokenHash("hash-token-1")

        assertNotNull(found)
        assertEquals(token.jti, found?.jti)
    }

    @Test
    fun `findByTokenHash returns null for missing hash`() {
        val found = refreshTokenRepository.findByTokenHash("not-found")

        assertNull(found)
    }

    @Test
    fun `findAllByAccountIdAndRevokedAtIsNull returns only active tokens`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000222")
        val active =
            refreshTokenRepository.save(
                RefreshToken.create(
                    accountId = accountId,
                    tokenHash = "hash-active",
                    jti = "jti-active",
                    expiresAt = Instant.now().plusSeconds(3600),
                ),
            )
        val revoked =
            refreshTokenRepository.save(
                RefreshToken.create(
                    accountId = accountId,
                    tokenHash = "hash-revoked",
                    jti = "jti-revoked",
                    expiresAt = Instant.now().plusSeconds(3600),
                ),
            )
        revoked.revoke(Instant.now())
        refreshTokenRepository.save(revoked)

        val tokens = refreshTokenRepository.findAllByAccountIdAndRevokedAtIsNull(accountId)

        assertEquals(1, tokens.size)
        assertEquals(active.jti, tokens.first().jti)
        assertFalse(tokens.first().isRevoked())
    }

    @Test
    fun `existsByTokenHash returns true for saved token hash`() {
        refreshTokenRepository.save(
            RefreshToken.create(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000333"),
                tokenHash = "hash-token-exists",
                jti = "jti-exists",
                expiresAt = Instant.now().plusSeconds(3600),
            ),
        )

        val exists = refreshTokenRepository.existsByTokenHash("hash-token-exists")

        assertTrue(exists)
    }

    @Test
    fun `existsByTokenHash returns false when token hash does not exist`() {
        val exists = refreshTokenRepository.existsByTokenHash("unknown-hash")

        assertFalse(exists)
    }

    @Test
    fun `findAllByAccountIdAndRevokedAtIsNull returns empty for account without active tokens`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000444")

        val tokens = refreshTokenRepository.findAllByAccountIdAndRevokedAtIsNull(accountId)

        assertTrue(tokens.isEmpty())
    }
}
