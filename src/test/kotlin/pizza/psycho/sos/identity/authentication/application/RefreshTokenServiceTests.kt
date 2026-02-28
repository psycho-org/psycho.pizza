package pizza.psycho.sos.identity.authentication.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import pizza.psycho.sos.identity.authentication.application.service.RefreshTokenService
import pizza.psycho.sos.identity.authentication.domain.RefreshToken
import pizza.psycho.sos.identity.authentication.infrastructure.RefreshTokenRepository
import pizza.psycho.sos.identity.security.config.JwtProperties
import java.time.Instant
import java.util.UUID

class RefreshTokenServiceTests {
    private val refreshTokenRepository = mock(RefreshTokenRepository::class.java)
    private val jwtProperties =
        JwtProperties().apply {
            refreshTokenValiditySeconds = 1209600
        }
    private val refreshTokenService = RefreshTokenService(refreshTokenRepository, jwtProperties)

    @Test
    fun `issue stores hashed refresh token and returns raw token`() {
        `when`(refreshTokenRepository.existsByTokenHash(anyString())).thenReturn(false)

        val rawToken = refreshTokenService.issue(UUID.fromString("00000000-0000-0000-0000-000000000111"))

        val captor = ArgumentCaptor.forClass(RefreshToken::class.java)
        verify(refreshTokenRepository).save(captor.capture())
        val saved = captor.value
        assertNotNull(rawToken)
        assertNotEquals(rawToken, saved.tokenHash)
    }

    @Test
    fun `rotate revokes previous token and issues a new token`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000222")
        val existing =
            RefreshToken.create(
                accountId = accountId,
                tokenHash = "existing-hash",
                jti = "existing-jti",
                expiresAt = Instant.now().plusSeconds(3600),
            )
        existing.id = UUID.fromString("00000000-0000-0000-0000-000000000444")
        val existingId = requireNotNull(existing.id)

        `when`(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(existing)
        `when`(refreshTokenRepository.revokeIfActive(eq(existingId), any(Instant::class.java)))
            .thenReturn(1)
        `when`(refreshTokenRepository.existsByTokenHash(anyString())).thenReturn(false)

        val rotated = refreshTokenService.rotate("raw-refresh-token")

        assertNotNull(rotated)
        assertEquals(accountId, rotated!!.accountId)
        verify(refreshTokenRepository, times(1)).save(org.mockito.ArgumentMatchers.any(RefreshToken::class.java))
    }

    @Test
    fun `rotate returns null when refresh token is invalid`() {
        `when`(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(null)

        val rotated = refreshTokenService.rotate("unknown-token")

        assertNull(rotated)
    }

    @Test
    fun `rotate returns null when active token revoke update fails`() {
        val existing =
            RefreshToken.create(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000555"),
                tokenHash = "existing-hash-2",
                jti = "existing-jti-2",
                expiresAt = Instant.now().plusSeconds(3600),
            )
        existing.id = UUID.fromString("00000000-0000-0000-0000-000000000666")
        val existingId = requireNotNull(existing.id)

        `when`(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(existing)
        `when`(refreshTokenRepository.revokeIfActive(eq(existingId), any(Instant::class.java)))
            .thenReturn(0)

        val rotated = refreshTokenService.rotate("raw-refresh-token")

        assertNull(rotated)
    }
}
