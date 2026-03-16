package pizza.psycho.sos.project.reason.application.port.inbound.usecase.command

import pizza.psycho.sos.project.reason.domain.model.vo.EventType
import pizza.psycho.sos.project.reason.domain.model.vo.TargetType
import java.util.UUID

data class RecordReasonCommand(
    val reason: String,
    val workspaceId: UUID,
    val targetId: UUID,
    val targetType: TargetType,
    val eventId: UUID,
    val eventType: EventType,
)
