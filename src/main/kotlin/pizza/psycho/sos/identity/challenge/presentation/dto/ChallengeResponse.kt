package pizza.psycho.sos.identity.challenge.presentation.dto

import java.util.UUID

sealed interface ChallengeResponse {
    data class Requested(
        val challengeId: UUID,
    ) : ChallengeResponse

    data class Confirmed(
        val confirmationTokenId: UUID,
        val verifiedEmail: String,
    ) : ChallengeResponse
}
