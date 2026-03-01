package pizza.psycho.sos.common.handler

open class DomainException(
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
