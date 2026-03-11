package pizza.psycho.sos.identity.account.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class AccountErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    ACCOUNT_EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Email already registered"),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Account not found"),
    ACCOUNT_INVALID_DISPLAY_NAME(HttpStatus.BAD_REQUEST, "Invalid display name"),
    ACCOUNT_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    ACCOUNT_INVALID_CONFIRMATION_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired confirmation token"),
    ACCOUNT_WITHDRAWAL_BLOCKED_BY_OWNED_WORKSPACE(
        HttpStatus.PRECONDITION_FAILED,
        "Transfer ownership or delete owned workspaces before withdrawing",
    ),
    ;

    override val code: String = name
}
