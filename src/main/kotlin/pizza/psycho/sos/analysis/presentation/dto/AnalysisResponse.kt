package pizza.psycho.sos.analysis.presentation.dto

import java.time.Instant
import java.util.UUID

sealed interface AnalysisResponse {
    data class Create(
        val analysisRequestId: UUID,
        val status: String,
        val createdAt: Instant,
    ) : AnalysisResponse
}
