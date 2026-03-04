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
            if (refreshTokenRepository.existsByTokenHash(tokenHash)) return@repeat
            refreshTokenRepository.save(
                RefreshToken.create(
                    accountId = accountId,
                    tokenHash = tokenHash,
                    jti = UUID.randomUUID().toString(),
                    expiresAt = Instant.now().plusSeconds(jwtProperties.refreshTokenValiditySeconds),
                ),
            )
            return rawToken
        }
        throw IllegalStateException("Failed to generate unique refresh token")
    }

    fun rotate(rawToken: String): RotatedRefreshToken? {
        val current = findValidToken(rawToken) ?: return null
        if (refreshTokenRepository.revokeIfActive(current.id(), Instant.now()) != 1) return null
        val accountId = current.accountId()

        return RotatedRefreshToken(
            accountId = accountId,
            refreshToken = issue(accountId),
        )
    }

    fun revoke(rawToken: String) {
        val token =
            hashOrNull(rawToken)
                ?.let(refreshTokenRepository::findByTokenHash) ?: return

        token.revoke()
        refreshTokenRepository.save(token)
    }

    private fun findValidToken(rawToken: String): RefreshToken? =
        hashOrNull(rawToken)
            ?.let(refreshTokenRepository::findByTokenHash)
            ?.takeUnless { it.isRevoked() || it.isExpired() }

    private fun hashOrNull(rawToken: String): String? =
        rawToken
            .trim()
            .takeIf(String::isNotEmpty)
            ?.let(this::hash)

    private fun hash(value: String): String {
        val bytes =
            MessageDigest
                .getInstance(HASH_ALGORITHM)
                .digest(value.toByteArray(StandardCharsets.UTF_8))

        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateRawToken(): String =
        ByteArray(REFRESH_TOKEN_BYTE_SIZE)
            .also(secureRandom::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    data class RotatedRefreshToken(
        val accountId: UUID,
        val refreshToken: String,
    )

    companion object {
        private const val MAX_TOKEN_GENERATION_RETRIES = 3
        private const val REFRESH_TOKEN_BYTE_SIZE = 48
        private const val HASH_ALGORITHM = "SHA-256"
        private val secureRandom = SecureRandom()
    }
}
