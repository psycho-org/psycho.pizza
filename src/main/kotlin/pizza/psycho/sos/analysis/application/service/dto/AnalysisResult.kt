package pizza.psycho.sos.analysis.application.service.dto

import java.time.Instant
import java.util.UUID

/*
 * 서비스 반환 DTO
 */
sealed interface AnalysisResult {
    data class Created(
        val id: UUID,
        val status: String,
        val createdAt: Instant,
    ) : AnalysisResult
}
