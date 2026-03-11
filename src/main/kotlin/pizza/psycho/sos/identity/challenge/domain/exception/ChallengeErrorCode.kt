package pizza.psycho.sos.identity.challenge.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class ChallengeErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    CHALLENGE_OTP_COOLDOWN_ACTIVE(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting a new OTP"),
    CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Challenge not found"),
    CHALLENGE_EXPIRED(HttpStatus.GONE, "Challenge has expired"),
    CHALLENGE_MAX_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Maximum attempts exceeded"),
    CHALLENGE_INVALID_OTP(HttpStatus.UNAUTHORIZED, "Invalid OTP code"),
    ;

    override val code: String = name
}
