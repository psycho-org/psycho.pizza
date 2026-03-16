package pizza.psycho.sos.analysis.infrastructure.sqs.dto

import pizza.psycho.sos.analysis.application.service.dto.SprintAnalysisInput
import java.util.UUID

data class SqsRequestQueueItem(
    val workspaceId: UUID,
    val analysisRequestId: UUID,
    val payload: SprintAnalysisInput,
)
