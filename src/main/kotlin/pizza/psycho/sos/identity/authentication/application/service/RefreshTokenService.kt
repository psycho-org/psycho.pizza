package pizza.psycho.sos.identity.authentication.application.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import pizza.psycho.sos.identity.authentication.domain.RefreshToken
import pizza.psycho.sos.identity.authentication.infrastructure.RefreshTokenRepository
import pizza.psycho.sos.identity.security.config.JwtProperties
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
@Transactional
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
) {
    fun issue(accountId: UUID): String {
        repeat(MAX_TOKEN_GENERATION_RETRIES) {
            val rawToken = generateRawToken()
            val tokenHash = hash(rawToken)
            if (!refreshTokenRepository.existsByTokenHash(tokenHash)) {
                val expiresAt = Instant.now().plusSeconds(jwtProperties.refreshTokenValiditySeconds)
                refreshTokenRepository.save(
                    RefreshToken.create(
                        accountId = accountId,
                        tokenHash = tokenHash,
                        jti = UUID.randomUUID().toString(),
                        expiresAt = expiresAt,
                    ),
                )
                return rawToken
            }
        }
        throw IllegalStateException("Failed to generate unique refresh token")
    }

    fun rotate(rawToken: String): RotatedRefreshToken? {
        val current = findValidToken(rawToken) ?: return null
        val accountId = requireNotNull(current.accountId) { "Refresh token account id is required" }
        val tokenId = requireNotNull(current.id) { "Refresh token id is required" }
        val updatedRows = refreshTokenRepository.revokeIfActive(tokenId, Instant.now())
        if (updatedRows != 1) return null

        val newRawToken = issue(accountId)
        return RotatedRefreshToken(accountId = accountId, refreshToken = newRawToken)
    }

    fun revoke(rawToken: String) {
        val tokenHash = hashOrNull(rawToken) ?: return
        val token = refreshTokenRepository.findByTokenHash(tokenHash) ?: return
        token.revoke()
        refreshTokenRepository.save(token)
    }

    private fun findValidToken(rawToken: String): RefreshToken? {
        val tokenHash = hashOrNull(rawToken) ?: return null
        val token = refreshTokenRepository.findByTokenHash(tokenHash) ?: return null
        if (token.isRevoked()) return null
        if (token.isExpired()) return null
        return token
    }

    private fun hashOrNull(rawToken: String): String? {
        val normalized = rawToken.trim()
        if (normalized.isEmpty()) return null
        return hash(normalized)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(value.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun generateRawToken(): String {
        val randomBytes = ByteArray(REFRESH_TOKEN_BYTE_SIZE)
        secureRandom.nextBytes(randomBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }

    data class RotatedRefreshToken(
        val accountId: UUID,
        val refreshToken: String,
    )

    companion object {
        private const val MAX_TOKEN_GENERATION_RETRIES = 3
        private const val REFRESH_TOKEN_BYTE_SIZE = 48
        private val secureRandom = SecureRandom()
    }
}
