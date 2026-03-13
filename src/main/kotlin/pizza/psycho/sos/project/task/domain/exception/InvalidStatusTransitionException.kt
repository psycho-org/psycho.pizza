package pizza.psycho.sos.project.task.domain.exception

import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.project.task.domain.model.vo.Status

class InvalidStatusTransitionException(
    from: Status,
    to: Status,
) : DomainException(TaskErrorCode.INVALID_TRANSITION, "Cannot transition from $from to $to")
