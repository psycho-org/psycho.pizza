package pizza.psycho.sos.identity.challenge.infrastructure

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pizza.psycho.sos.identity.challenge.domain.ConfirmationToken
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.time.Instant
import java.util.UUID

interface ConfirmationTokenRepository : JpaRepository<ConfirmationToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT token
        FROM ConfirmationToken token
        WHERE token.id = :id
          AND token.used = false
          AND token.expiresAt > :now
          AND token.operationType = :operationType
        """,
    )
    fun findUsableByIdAndOperationTypeForUpdate(
        @Param("id") id: UUID,
        @Param("operationType") operationType: OperationType,
        @Param("now") now: Instant,
    ): ConfirmationToken?
}
