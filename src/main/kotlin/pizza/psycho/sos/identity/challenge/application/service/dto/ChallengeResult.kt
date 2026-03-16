package pizza.psycho.sos.identity.challenge.application.service.dto

import pizza.psycho.sos.common.domain.vo.Email
import java.time.Instant
import java.util.UUID

sealed interface RequestChallengeResult {
    data class Success(
        val challengeId: UUID,
        val expiresAt: Instant,
    ) : RequestChallengeResult

    sealed interface Failure : RequestChallengeResult {
        data class CooldownActive(
            val availableAt: Instant,
            val retryAfterSeconds: Long,
        ) : Failure
    }
}

sealed interface VerifyOtpResult {
    data class Success(
        val confirmationTokenId: UUID,
        val targetEmail: Email,
    ) : VerifyOtpResult

    sealed interface Failure : VerifyOtpResult {
        data object ChallengeExpired : Failure

        data object ChallengeNotFound : Failure

        data object InvalidOtp : Failure

        data object MaxAttemptsExceeded : Failure

        data object OperationTypeMismatch : Failure

        data object RequesterEmailMismatch : Failure
    }
}
