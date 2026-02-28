package pizza.psycho.sos.identity.authentication.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import pizza.psycho.sos.identity.authentication.domain.RefreshToken
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByJti(jti: String): RefreshToken?

    fun findByTokenHash(tokenHash: String): RefreshToken?

    fun existsByTokenHash(tokenHash: String): Boolean

    fun findAllByAccountIdAndRevokedAtIsNull(accountId: UUID): List<RefreshToken>
}
