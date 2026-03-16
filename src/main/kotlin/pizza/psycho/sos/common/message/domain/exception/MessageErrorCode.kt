package pizza.psycho.sos.common.message.domain.exception

import org.springframework.http.HttpStatus
import pizza.psycho.sos.common.exception.BaseErrorCode

enum class MessageErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    MESSAGE_REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "Required message field is missing"),
    MESSAGE_INVALID_PARAM(HttpStatus.BAD_REQUEST, "Invalid message parameter"),
    MESSAGE_INVALID_NUMBER_PARAM(HttpStatus.BAD_REQUEST, "Message parameter must be a valid number"),
    MESSAGE_POSITIVE_NUMBER_REQUIRED(HttpStatus.BAD_REQUEST, "Message parameter must be positive"),
    MESSAGE_INVALID_UUID_PARAM(HttpStatus.BAD_REQUEST, "Message parameter must be a valid UUID"),
    MESSAGE_INVALID_EMAIL_PARAM(HttpStatus.BAD_REQUEST, "Message parameter must be a valid email"),
    MESSAGE_MAIL_UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST, "Unsupported mail type"),
    MESSAGE_CHANNEL_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "Message channel is not supported"),
    MESSAGE_TOKEN_AUTH_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "Token authentication is not supported"),
    MESSAGE_MAIL_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "Mail template not found"),
    MESSAGE_MAIL_TEMPLATE_RENDER_FAILED(HttpStatus.BAD_REQUEST, "Failed to render mail template"),
    MESSAGE_MAIL_DUPLICATE_PENDING_AUTH_TOKEN(HttpStatus.CONFLICT, "Duplicate pending mail auth token"),
    MESSAGE_MAIL_VERIFY_URL_REQUIRED(HttpStatus.INTERNAL_SERVER_ERROR, "Mail verify URL is required"),
    MESSAGE_MAIL_TOKEN_EXPIRE_HOURS_REQUIRED(HttpStatus.INTERNAL_SERVER_ERROR, "Token expire hours is required"),
    MESSAGE_MAIL_ACTION_TYPE_REQUIRED(HttpStatus.INTERNAL_SERVER_ERROR, "Mail action type is required"),
    ;

    override val code: String = name
}
