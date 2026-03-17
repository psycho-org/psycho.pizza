package pizza.psycho.sos.analysis.infrastructure.sqs.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import pizza.psycho.sos.analysis.application.service.dto.SprintAnalysisInput
import java.util.UUID

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SqsRequestQueueItem(
    val externalRequestId: UUID,
    val resultFetchUrl: String,
    val openaiRequest: SprintAnalysisInput,
    val tenant: String? = null,
    val context: Context? = null,
//    val workspaceId: UUID,
//    val analysisRequestId: UUID,
//    val payload: SprintAnalysisInput,
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Context(
        val workspaceId: UUID,
        val analysisRequestId: UUID,
    )
}
