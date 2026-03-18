package pizza.psycho.sos.analysis.presentation.dto

import pizza.psycho.sos.analysis.domain.vo.AnalysisRequestStatus
import java.time.Instant
import java.util.UUID

sealed interface AnalysisResponse {
    data class CreateAnalysisRequestResponse(
        val analysisRequestId: UUID,
        val status: String,
        val createdAt: Instant,
    ) : AnalysisResponse

    sealed interface GetAnalysisRequestList {
        data class Response(
            val items: List<Item>,
        ) : AnalysisResponse

        data class Item(
            val analysisRequestId: UUID,
            val status: AnalysisRequestStatus,
            val hasReport: Boolean,
            val requestedAt: Instant,
        )
    }

    sealed interface GetAnalysisRequestReport {
        data class Response(
            val analysisRequestId: UUID,
            val status: AnalysisRequestStatus,
            val requestedAt: Instant,
            val result: Result?,
        ) : AnalysisResponse

        data class Result(
            val reportId: UUID,
            val summary: String,
            val strength: String?,
            val risk: String?,
            val recommendation: String?,
            val reportedAt: Instant,
        )
    }
}
