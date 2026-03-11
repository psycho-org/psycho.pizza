package pizza.psycho.sos.project.task.domain.exception

import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.project.task.domain.model.vo.Priority

class InvalidPriorityTransitionException(
    from: Priority,
    to: Priority,
) : DomainException("Cannot transition from $from to $to")
