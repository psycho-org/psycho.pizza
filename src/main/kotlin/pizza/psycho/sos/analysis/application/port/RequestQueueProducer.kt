package pizza.psycho.sos.analysis.application.port

import pizza.psycho.sos.analysis.application.service.dto.SprintAnalysisInput
import java.util.UUID

interface RequestQueueProducer {
    fun send(
        workspaceId: UUID,
        analysisRequestId: UUID,
        payload: SprintAnalysisInput,
    )
}
