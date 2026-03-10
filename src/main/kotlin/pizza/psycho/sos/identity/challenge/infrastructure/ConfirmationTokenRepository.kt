package pizza.psycho.sos.identity.challenge.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import pizza.psycho.sos.identity.challenge.domain.ConfirmationToken
import java.util.UUID

interface ConfirmationTokenRepository : JpaRepository<ConfirmationToken, UUID> {
    fun findByIdAndUsedFalse(id: UUID): ConfirmationToken?
}
