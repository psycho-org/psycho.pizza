package pizza.psycho.sos.common.handler

import pizza.psycho.sos.common.exception.BaseErrorCode

open class DomainException(
    val errorCode: BaseErrorCode?,
    override val message: String =
        errorCode?.message ?: throw IllegalArgumentException(
            "[WARNING]: Exception message should be filled by manually or as ErrorCode message.",
            null,
        ),
    override val cause: Throwable? = null,
    val meta: ErrorMeta? = null,
) : RuntimeException(message, cause) {
    @Deprecated(
        message =
            "DomainException(errorCode, message, cause) 형식을 사용하세요.",
        level = DeprecationLevel.ERROR,
    )
    constructor(
        message: String,
        cause: Throwable? = null,
    ) : this(null, message, cause, null)
}
