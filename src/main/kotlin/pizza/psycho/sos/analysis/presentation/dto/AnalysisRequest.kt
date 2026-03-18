package pizza.psycho.sos.analysis.presentation.dto

import jakarta.validation.constraints.NotNull
import java.util.UUID

sealed interface AnalysisRequest {
    data class CreateAnalysisRequest(
        @field:NotNull
        var sprintId: UUID,
    ) : AnalysisRequest
}
