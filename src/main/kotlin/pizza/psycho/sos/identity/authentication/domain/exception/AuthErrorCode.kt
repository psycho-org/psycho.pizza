package pizza.psycho.sos.identity.authentication.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class AuthErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    ;

    override val code: String = name
}
