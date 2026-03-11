package pizza.psycho.sos.identity.challenge.application.service.dto

import pizza.psycho.sos.identity.account.domain.vo.Email
import java.util.UUID

sealed interface RequestChallengeResult {
    data class Success(
        val challengeId: UUID,
    ) : RequestChallengeResult

    sealed interface Failure : RequestChallengeResult {
        data object CooldownActive : Failure
    }
}

sealed interface VerifyOtpResult {
    data class Success(
        val confirmationTokenId: UUID,
        val targetEmail: Email,
    ) : VerifyOtpResult

    sealed interface Failure : VerifyOtpResult {
        data object ChallengeNotFound : Failure

        data object ChallengeExpired : Failure

        data object MaxAttemptsExceeded : Failure

        data object InvalidOtp : Failure
    }
}

sealed interface ConsumeTokenResult {
    data class Success(
        val targetEmail: Email,
    ) : ConsumeTokenResult

    sealed interface Failure : ConsumeTokenResult {
        data object TokenNotFound : Failure

        data object TokenExpired : Failure

        data object OperationTypeMismatch : Failure
    }
}
