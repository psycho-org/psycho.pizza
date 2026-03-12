package pizza.psycho.sos.identity.challenge.application.service.dto

import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.util.UUID

sealed interface ChallengeCommand {
    data class Request(
        val email: String,
        val operationType: OperationType,
    ) : ChallengeCommand

    data class Verify(
        val challengeId: UUID,
        val otpCode: String,
        val expectedOperationType: OperationType,
        val requesterEmail: String?,
    ) : ChallengeCommand

    data class AcquireToken(
        val tokenId: UUID,
        val operationType: OperationType,
    ) : ChallengeCommand
}
