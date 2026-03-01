package pizza.psycho.sos.project.task.domain.exception

import pizza.psycho.sos.common.handler.DomainException

class InvalidDueDateException(
    override val message: String,
    override val cause: Throwable? = null,
) : DomainException(message, cause)
