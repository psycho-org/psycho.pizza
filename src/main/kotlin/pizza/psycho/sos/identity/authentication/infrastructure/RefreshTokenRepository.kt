package pizza.psycho.sos.identity.authentication.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pizza.psycho.sos.identity.authentication.domain.RefreshToken
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByJti(jti: String): RefreshToken?

    fun findByTokenHash(tokenHash: String): RefreshToken?

    fun existsByTokenHash(tokenHash: String): Boolean

    fun findAllByAccountIdAndRevokedAtIsNull(accountId: UUID): List<RefreshToken>

    @Modifying
    @Query(
        """
        update RefreshToken token
           set token.revokedAt = :now
         where token.id = :id
           and token.revokedAt is null
           and token.expiresAt > :now
        """,
    )
    fun revokeIfActive(
        @Param("id") id: UUID?,
        @Param("now") now: Instant?,
    ): Int
}
