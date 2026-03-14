package pizza.psycho.sos.analysis.domain.event

import pizza.psycho.sos.common.event.DomainEvent
import java.time.Instant
import java.util.UUID

data class AnalysisRequestCreatedEvent(
    val workspaceId: UUID,
    val analysisRequestId: UUID,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
