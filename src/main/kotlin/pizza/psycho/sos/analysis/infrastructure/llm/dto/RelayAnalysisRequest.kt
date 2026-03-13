package pizza.psycho.sos.analysis.infrastructure.llm.dto

import pizza.psycho.sos.analysis.application.service.dto.SprintAnalysisPayload
import java.util.UUID

class RelayAnalysisRequest(
    val jobId: UUID,
    val workspaceId: UUID,
    val data: SprintAnalysisPayload,
)
