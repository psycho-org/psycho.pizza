package pizza.psycho.sos.analysis.infrastructure.sqs.dto

import java.util.UUID

data class SqsResponseQueueItem(
    val analysisRequestId: UUID,
)
