package pizza.psycho.sos.identity.authentication.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseEntity
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_tokens_account_id", columnList = "account_id"),
    ],
)
class RefreshToken protected constructor() : BaseEntity() {
    @Column(name = "account_id", nullable = false)
    var accountId: UUID? = null
        protected set

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    var tokenHash: String? = null
        protected set

    @Column(name = "jti", nullable = false, unique = true, length = 64)
    var jti: String? = null
        protected set

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant? = null
        protected set

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null
        protected set

    fun isExpired(now: Instant = Instant.now()): Boolean {
        val expiry = requireNotNull(expiresAt) { "expiresAt must be set before expiration check" }
        return expiry.isBefore(now) || expiry == now
    }

    fun isRevoked(): Boolean = revokedAt != null

    fun revoke(now: Instant = Instant.now()) {
        revokedAt = revokedAt ?: now
    }

    fun id(): UUID = requireNotNull(id) { "RefreshToken.id must not be null after persistence" }

    fun accountId(): UUID = requireNotNull(accountId) { "RefreshToken.accountId must not be null" }

    companion object {
        fun create(
            accountId: UUID,
            tokenHash: String,
            jti: String,
            expiresAt: Instant,
        ): RefreshToken {
            val normalizedTokenHash = tokenHash.trim()
            val normalizedJti = jti.trim()
            require(normalizedTokenHash.isNotEmpty()) { "tokenHash must not be blank" }
            require(normalizedJti.isNotEmpty()) { "jti must not be blank" }

            return RefreshToken().apply {
                this.accountId = accountId
                this.tokenHash = normalizedTokenHash
                this.jti = normalizedJti
                this.expiresAt = expiresAt
            }
        }
    }
}
