package pizza.psycho.sos.project.reason.application.port.inbound.usecase

import pizza.psycho.sos.project.reason.application.port.inbound.usecase.command.RecordReasonCommand

@FunctionalInterface
interface RecordReasonUseCase {
    fun record(command: RecordReasonCommand)
}
