package pizza.psycho.sos.common.exception

import org.springframework.http.HttpStatus

enum class CommonErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    COMMON_INVALID_EMAIL(HttpStatus.BAD_REQUEST, "Invalid email"),
    ;

    override val code: String = name
}
