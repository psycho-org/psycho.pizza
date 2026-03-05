package pizza.psycho.sos.analysis.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

sealed interface AnalysisRequest {
    data class Create(
        @field:NotNull
        var target: Target.Sprint,
    ) : AnalysisRequest

    sealed interface Target {
        data class Sprint(
            @field:NotBlank
            val sprintId: UUID,
        ) : Target
    }
}
