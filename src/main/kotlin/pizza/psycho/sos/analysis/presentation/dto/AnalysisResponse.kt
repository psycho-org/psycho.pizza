package pizza.psycho.sos.analysis.presentation.dto

import java.time.Instant
import java.util.UUID

sealed interface AnalysisResponse {
    data class CreateAnalysisRequestResponse(
        val analysisRequestId: UUID,
        val status: String,
        val createdAt: Instant,
    ) : AnalysisResponse
}
