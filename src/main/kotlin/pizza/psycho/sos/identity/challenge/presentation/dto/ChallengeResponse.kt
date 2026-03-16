package pizza.psycho.sos.identity.challenge.presentation.dto

import java.time.Instant
import java.util.UUID

sealed interface ChallengeResponse {
    data class Requested(
        val challengeId: UUID,
        val expiresAt: Instant,
    ) : ChallengeResponse

    data class Confirmed(
        val confirmationTokenId: UUID,
        val verifiedEmail: String,
    ) : ChallengeResponse
}
