package pizza.psycho.sos.identity.challenge.application.service.dto

import java.util.UUID

sealed interface RequestChallengeResult {
    data class Success(
        val challengeId: UUID,
    ) : RequestChallengeResult

    sealed interface Failure :
        RequestChallengeResult,
        ChallengeFailures {
        data object CooldownActive : Failure
    }
}

sealed interface VerifyOtpResult {
    data class Success(
        val confirmationTokenId: UUID,
        val targetEmail: String,
    ) : VerifyOtpResult

    sealed interface Failure :
        VerifyOtpResult,
        ChallengeFailures {
        data object ChallengeNotFound : Failure

        data object ChallengeExpired : Failure

        data object MaxAttemptsExceeded : Failure

        data object InvalidOtp : Failure
    }
}

sealed interface ConsumeTokenResult {
    data class Success(
        val targetEmail: String,
    ) : ConsumeTokenResult

    sealed interface Failure :
        ConsumeTokenResult,
        ChallengeFailures {
        data object TokenNotFound : Failure

        data object TokenExpired : Failure

        data object OperationTypeMismatch : Failure
    }
}

sealed interface ChallengeFailures
