package pizza.psycho.sos.project.reason.application.port.inbound.usecase

import pizza.psycho.sos.project.reason.application.event.ReasonInternalEvent

@FunctionalInterface
interface RecordReasonUseCase {
    fun record(event: ReasonInternalEvent)
}
