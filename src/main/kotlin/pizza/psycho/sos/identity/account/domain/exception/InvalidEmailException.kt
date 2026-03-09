package pizza.psycho.sos.identity.account.domain.exception

import pizza.psycho.sos.common.handler.DomainException

class InvalidEmailException(
    override val message: String,
    override val cause: Throwable? = null,
) : DomainException(message, cause)
