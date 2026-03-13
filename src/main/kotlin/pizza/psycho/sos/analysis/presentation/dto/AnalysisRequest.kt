package pizza.psycho.sos.analysis.presentation.dto

import jakarta.validation.constraints.NotNull
import java.util.UUID

sealed interface AnalysisRequest {
    data class CreateAnalysisRequest(
        @field:NotNull
        var workspaceId: UUID,
        @field:NotNull
        var sprintId: UUID,
    ) : AnalysisRequest

    data class CreateAnalysisReport(
        @field:NotNull
        var jobId: UUID,
        @field:NotNull
        var reportContent: String,
    )
}
