package pizza.psycho.sos.analysis.application.port.dto

import java.util.UUID

data class AnalysisJobQueueItem(
    val jobId: UUID, // NOTE: AnalysisRequestId
)
