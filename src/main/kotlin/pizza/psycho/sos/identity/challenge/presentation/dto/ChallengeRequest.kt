package pizza.psycho.sos.identity.challenge.presentation.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

sealed interface ChallengeRequest {
    data class RequestOtp(
        @field:Email
        @field:NotBlank
        val email: String,
    ) : ChallengeRequest

    data class VerifyOtp(
        @field:NotNull
        var challengeId: UUID,
        @field:NotBlank
        val otpCode: String,
    ) : ChallengeRequest
}
