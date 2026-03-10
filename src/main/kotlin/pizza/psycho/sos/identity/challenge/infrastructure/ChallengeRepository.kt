package pizza.psycho.sos.identity.challenge.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import pizza.psycho.sos.identity.challenge.domain.Challenge
import pizza.psycho.sos.identity.challenge.domain.vo.ChallengeStatus
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.util.UUID

interface ChallengeRepository : JpaRepository<Challenge, UUID> {
    fun findByTargetEmailValueIgnoreCaseAndOperationTypeAndStatus(
        targetEmail: String,
        operationType: OperationType,
        status: ChallengeStatus,
    ): Challenge?

    fun findByIdAndStatus(
        id: UUID,
        status: ChallengeStatus,
    ): Challenge?
}
