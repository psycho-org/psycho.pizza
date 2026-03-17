package pizza.psycho.sos.common.handler

import pizza.psycho.sos.common.exception.BaseErrorCode

open class DomainException(
    val errorCode: BaseErrorCode,
    override val message: String = errorCode.message,
    override val cause: Throwable? = null,
    val meta: ErrorMeta? = null,
) : RuntimeException(message, cause)
