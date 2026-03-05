package pizza.psycho.sos.analysis.presentation.dto

import pizza.psycho.sos.analysis.domain.vo.AnalysisRequestStatus
import java.time.Instant
import java.util.UUID

sealed interface AnalysisResponse {
    data class Create(
        val analysisRequestId: UUID,
        val status: AnalysisRequestStatus,
        val createdAt: Instant,
    ) : AnalysisResponse
}
